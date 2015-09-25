package com.lezo.mall.blade;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class AmazonBestSaleSkuMain {
    private static String CODE_SEPERATOR = "->";

    public static void main(String[] args) throws Exception {
        String dirPath = "./data/amazon/top/sku/";
        long startMills = System.currentTimeMillis();
        File dirFile = new File(dirPath);
        for (File dataFile : dirFile.listFiles()) {
            ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
            List<String> urlList = FileUtils.readLines(dataFile, "UTF-8");
            for (String line : urlList) {
                String[] unitArr = line.split("\t");
                int index = -1;
                String cateUrl = unitArr[++index];
                String cateName = unitArr[++index];
                String level = unitArr[++index];
                String crumb = unitArr[++index];
                exec.execute(new AmazonBestSaleSkuWorker(crumb, cateName, cateUrl, level, dirPath));
                return;
            }
        }
        long costMills = System.currentTimeMillis() - startMills;
        System.err.println("done......cost:" + costMills);
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
                element.setCode(parent.getCode() + CODE_SEPERATOR + element.getName());
            }
            children.add(element);
            addChildren(element);
        }
    }
}
