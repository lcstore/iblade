package com.lezo.mall.blade.require.top;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.require.top.po.CategoryElement;
import com.lezo.mall.blade.require.top.worker.JdBestSaleListWorker;

public class JdBestSaleListMain {

    public static void main(String[] args) throws Exception {
        String destPath = System.getProperty("dest", "./data/jd/top/cate/");
        String skipString = System.getProperty("skip", "true");
        boolean skipDone = "true".equals(skipString) ? true : false;
        String url = "http://www.jd.com/allSort.aspx";
        long startMills = System.currentTimeMillis();
        Document dom = Jsoup.connect(url).get();
        Elements ceEls = dom.select("#allsort.w div[id^=JDS_].m");
        List<CategoryElement> cElements = new ArrayList<CategoryElement>();
        Set<String> doneSet = getDoneSet(new File(destPath));
        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        for (Element ce : ceEls) {
            Elements ctEls = ce.select("div.mt h2 a");
            CategoryElement element = new CategoryElement();
            if (ctEls.size() > 1) {
                element.setName(ctEls.text().trim());
                element.setUrl("");
                element.setLevel(1);
                element.setCode(element.getName());
            } else {
                element.setName(ctEls.first().text().trim());
                element.setUrl(ctEls.first().absUrl("href"));
                element.setLevel(1);
                element.setCode(element.getName());
            }
            cElements.add(element);
            if (!skipDone && doneSet.contains(element.getName())) {
                System.err.println("had done:" + element.getName());
                continue;
            }
            String fileName =
                    cElements.size() + "_" + element.getName().trim().replace("/", "-") + ".txt";
            File destFile = new File(destPath, fileName);
            JdBestSaleListWorker worker = new JdBestSaleListWorker(destFile, element, ce);
            exec.execute(worker);
        }
        exec.shutdown();
        while (!exec.isTerminated()) {
            System.err.println("active:" + exec.getActiveCount() + ",done:"
                    + exec.getCompletedTaskCount() + ",queue:" + exec.getQueue().size());
            TimeUnit.SECONDS.sleep(1);
        }
        long costMills = System.currentTimeMillis() - startMills;
        float minutes = costMills * 1F / 1000 / 60;
        System.err.println("done......cost:" + costMills + ",minutes:" + minutes);
    }

    private static Set<String> getDoneSet(File file) {
        Set<String> doneSet = new HashSet<String>();
        File[] hasArr = file.listFiles();
        if (hasArr == null) {
            return doneSet;
        }
        for (File f : hasArr) {
            String name = f.getName();
            int index = name.indexOf("_");
            name = name.substring(index + 1);
            name = name.replace(".txt", "");
            doneSet.add(name);
        }
        return doneSet;
    }
}
