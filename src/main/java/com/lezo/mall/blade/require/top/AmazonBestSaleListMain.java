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
import com.lezo.mall.blade.require.top.worker.AmazonBestSaleListWorker;

public class AmazonBestSaleListMain {

    public static void main(String[] args) throws Exception {
        String dirPath = System.getProperty("dir", "./data/amazon/top/cate/");
        String skipString = System.getProperty("skip", "false");
        boolean skipDone = "true".equals(skipString) ? true : false;
        String url = "http://www.amazon.cn/gp/bestsellers/ref=zg_bs_unv_cps_0_665021051_3";
        long startMills = System.currentTimeMillis();
        Document dom = Jsoup.connect(url).get();
        Elements ceEls =
                dom.select("#zg_browseRoot ul li a[href*=www.amazon.cn/gp/bestsellers]:not(a:matchesOwn([图书]))");
        List<CategoryElement> cElements = new ArrayList<CategoryElement>();
        Set<String> doneSet = getDoneSet(new File(dirPath));
        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        for (Element ce : ceEls) {
            CategoryElement element = new CategoryElement();
            element.setName(ce.ownText().trim());
            element.setUrl(ce.absUrl("href"));
            element.setLevel(1);
            element.setCode(element.getName());
            cElements.add(element);
            if (!skipDone && doneSet.contains(element.getName())) {
                System.err.println("had done:" + element.getName());
                continue;
            }
            String fileName =
                    cElements.size() + "_" + element.getName().trim().replace("/", "-") + ".txt";
            File destFile = new File(dirPath, fileName);
            exec.execute(new AmazonBestSaleListWorker(destFile, element));
        }
        exec.shutdown();
        while (!exec.isTerminated()) {
            System.err.println("active:" + exec.getActiveCount() + ",done:"
                    + exec.getCompletedTaskCount() + ",queue:" + exec.getQueue().size());
            TimeUnit.SECONDS.sleep(1);
        }
        long costMills = System.currentTimeMillis() - startMills;
        System.err.println("done......cost:" + costMills);
    }

    private static Set<String> getDoneSet(File file) {
        Set<String> doneSet = new HashSet<String>();
        for (File f : file.listFiles()) {
            String name = f.getName();
            int index = name.indexOf("_");
            name = name.substring(index + 1);
            name = name.replace(".txt", "");
            doneSet.add(name);
        }
        return doneSet;
    }
}
