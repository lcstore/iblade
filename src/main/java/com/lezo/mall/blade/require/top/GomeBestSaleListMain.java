package com.lezo.mall.blade.require.top;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.require.top.po.CategoryElement;
import com.lezo.mall.blade.require.top.worker.GomeBestSaleListWorker;

@Log4j
public class GomeBestSaleListMain {

    public static void main(String[] args) throws Exception {
        String destPath = System.getProperty("dest", "./data/gome/top/cate/");
        String skipString = System.getProperty("skip", "true");
        boolean skipDone = "true".equals(skipString) ? true : false;
        String url = "http://list.gome.com.cn/";
        long startMills = System.currentTimeMillis();
        Document dom =
                Jsoup.connect(url)
                        .header("Accept-Encoding", "gzip, deflate")
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:41.0) Gecko/20100101 Firefox/41.0")
                        .get();
        Elements ceEls = dom.select("div.cateItem div.hd");
        List<CategoryElement> cElements = new ArrayList<CategoryElement>();
        Set<String> doneSet = getDoneSet(new File(destPath));
        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        Map<String, String> map = new HashMap<String, String>();
        map.put("图书音像", "");
        map.put("文化艺术", "");
        map.put("汽车维修保养", "");
        map.put("整车销售", "");
        for (Element ce : ceEls) {
            Elements ct1Els = ce.select("b:not(b.pr15)");
            String sName = ct1Els.first().ownText().trim();
            if (map.containsKey(sName)) {
                continue;
            }
            CategoryElement element = new CategoryElement();
            element.setName(sName);
            element.setLevel(1);
            element.setCode(element.getName());
            cElements.add(element);
            String fileName =
                    cElements.size() + "_" + element.getName().trim().replace("/", "-") + ".txt";
            if (!skipDone && doneSet.contains(fileName)) {
                log.info("had done:" + element.getName());
                continue;
            }
            File destFile = new File(destPath, fileName);
            GomeBestSaleListWorker worker = new GomeBestSaleListWorker(destFile, element, ce);
            exec.execute(worker);
            // worker.run();
            // return;
        }
        exec.shutdown();
        while (!exec.isTerminated()) {
            log.info("active:" + exec.getActiveCount() + ",done:"
                    + exec.getCompletedTaskCount() + ",queue:" + exec.getQueue().size());
            TimeUnit.SECONDS.sleep(1);
        }
        long costMills = System.currentTimeMillis() - startMills;
        float minutes = costMills * 1F / 1000 / 60;
        log.info("done......cost:" + costMills + ",minutes:" + minutes);
    }

    private static Set<String> getDoneSet(File file) {
        Set<String> doneSet = new HashSet<String>();
        File[] hasArr = file.listFiles();
        if (hasArr == null) {
            return doneSet;
        }
        for (File f : hasArr) {
            String name = f.getName();
            doneSet.add(name);
        }
        return doneSet;
    }
}
