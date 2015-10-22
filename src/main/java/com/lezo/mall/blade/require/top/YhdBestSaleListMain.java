package com.lezo.mall.blade.require.top;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.lezo.mall.blade.require.top.worker.YhdBestSaleListWorker;

@Log4j
public class YhdBestSaleListMain {

    public static void main(String[] args) throws Exception {
        String destPath = System.getProperty("dest", "./data/yhd/top/cate/");
        String skipString = System.getProperty("skip", "true");
        boolean skipDone = "true".equals(skipString) ? true : false;
        String url = "http://www.yhd.com/marketing/allproduct.html?tp=2092.0.156.0.1.L23MfE5-11-EpjEs";
        long startMills = System.currentTimeMillis();
        Document dom = Jsoup.connect(url).get();
        Elements ceEls = dom.select("div.allsort div.alonesort div.mt h3 a[href]");
        List<CategoryElement> cElements = new ArrayList<CategoryElement>();
        Set<String> doneSet = getDoneSet(new File(destPath));
        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        Set<String> ignoreSet = new HashSet<String>();
        ignoreSet.add("礼品、卡、旅游、充值");
        for (Element ce : ceEls) {
            CategoryElement element = new CategoryElement();
            element.setName(ce.ownText().trim());
            element.setUrl(ce.absUrl("href"));
            element.setLevel(1);
            element.setCode(element.getName());
            if (ignoreSet.contains(element.getName())) {
                continue;
            }
            cElements.add(element);
            String fileName =
                    cElements.size() + "_" + element.getName().trim().replace("/", "-") + ".txt";
            if (!skipDone && doneSet.contains(fileName)) {
                log.info("had done:" + element.getName());
                continue;
            }
            File destFile = new File(destPath, fileName);
            YhdBestSaleListWorker worker = new YhdBestSaleListWorker(destFile, element, ce);
            exec.execute(worker);
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
