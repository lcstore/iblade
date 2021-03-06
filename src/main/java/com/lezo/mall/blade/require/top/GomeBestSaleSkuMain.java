package com.lezo.mall.blade.require.top;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.common.ParseConstant;
import com.lezo.mall.blade.require.top.po.CategoryElement;
import com.lezo.mall.blade.require.top.worker.GomeBestSaleSkuWorker;

@Log4j
public class GomeBestSaleSkuMain {

    public static void main(String[] args) throws Exception {
        String srcPath = System.getProperty("src", "./data/gome/top/cate/");
        String destPath = System.getProperty("dest", "./data/gome/top/sku/");
        long startMills = System.currentTimeMillis();
        File srcFile = new File(srcPath);
        File[] srcFiles = srcFile.listFiles();
        if (srcFiles == null) {
            throw new RuntimeException("no source in path,check src path:" + srcPath);
        }
        int total = srcFiles.length;
        int count = 0;
        int urlCount = 0;
        for (File dataFile : srcFiles) {
            ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
            List<String> urlList = FileUtils.readLines(dataFile, "UTF-8");
            for (String line : urlList) {
                String[] unitArr = line.split("\t");
                int index = -1;
                String cateUrl = unitArr[++index];
                String cateName = unitArr[++index];
                String level = unitArr[++index];
                String crumb = unitArr[++index];
                GomeBestSaleSkuWorker worker = new GomeBestSaleSkuWorker(crumb, cateName,
                        cateUrl, level, destPath);
                urlCount++;
                exec.execute(worker);
                // worker.run();
                // return;
            }
            waitForDone(exec);
            count++;
            log.info("file total:" + total + ",done:" + count + ".urlCount:" + urlCount);
        }
        long costMills = System.currentTimeMillis() - startMills;
        log.info("done......cost:" + costMills);
    }

    private static void waitForDone(ThreadPoolExecutor exec) throws Exception {
        exec.shutdown();
        while (!exec.isTerminated()) {
            log.info("active:" + exec.getActiveCount() + ",done:"
                    + exec.getCompletedTaskCount() + ",queue:" + exec.getQueue().size());
            TimeUnit.SECONDS.sleep(1);
        }

    }

    public static void getLeafs(CategoryElement cce, List<CategoryElement> leafList) {
        if (cce.getChildren() == null) {
            leafList.add(cce);
            return;
        }
        for (CategoryElement child : cce.getChildren()) {
            getLeafs(child, leafList);
        }
    }

    private static void addChildren(CategoryElement parent) throws Exception {
        String url = parent.getUrl();
        System.err.println(parent.getName() + ",level:" + parent.getLevel() + ",url:" + url);
        Document dom =
                Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:40.0) Gecko/20100101 Firefox/40.0")
                        .get();
        Elements curEls = dom.select("#zg_browseRoot span.zg_selected");
        if (curEls.isEmpty()) {
            return;
        }
        Elements ceEls =
                curEls.first().parent().parent().select("ul li a[href*=www.amazon.cn/gp/bestsellers]");
        if (ceEls.isEmpty()) {
            return;
        }
        List<CategoryElement> children = new ArrayList<CategoryElement>();
        parent.setChildren(children);
        for (Element ce : ceEls) {
            CategoryElement element = new CategoryElement();
            element.setName(ce.ownText().trim());
            element.setUrl(ce.absUrl("href"));
            element.setLevel(parent.getLevel() + 1);
            if (parent.getCode() == null) {
                element.setCode(element.getName());
            } else {
                element.setCode(parent.getCode() + ParseConstant.CODE_SEPERATOR + element.getName());
            }
            children.add(element);
            addChildren(element);
        }
    }
}
