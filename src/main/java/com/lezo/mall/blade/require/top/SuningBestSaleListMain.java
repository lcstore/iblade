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

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.require.top.po.CategoryElement;
import com.lezo.mall.blade.require.top.worker.SuningBestSaleListWorker;

@Log4j
public class SuningBestSaleListMain {

    public static void main(String[] args) throws Exception {
        String destPath = System.getProperty("dest", "./data/suning/top/cate/");
        String skipString = System.getProperty("skip", "true");
        boolean skipDone = "true".equals(skipString) ? true : false;
        String url = "http://www.suning.com/emall/pgv_10052_10051_1_.html";
        long startMills = System.currentTimeMillis();
        Document dom = Jsoup.connect(url).get();
        Elements ceEls = dom.select("div.sFloor:has(div.listLeft)");
        List<CategoryElement> cElements = new ArrayList<CategoryElement>();
        Set<String> doneSet = getDoneSet(new File(destPath));
        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        Set<String> ignoreSet = new HashSet<String>();
        ignoreSet.add("游戏/充值/旅行/彩票");
        ignoreSet.add("图书");
        ignoreSet.add("公益频道");
        ignoreSet.add("汽车服务");
        for (int i = 0; i < ceEls.size(); i++) {
            Element ce = ceEls.get(i);
            Elements ctEls = ce.select("> h3.sName a");
            CategoryElement element = new CategoryElement();
            element.setName(ctEls.first().ownText().trim());
            element.setUrl(ctEls.first().absUrl("href"));
            element.setLevel(1);
            if (StringUtils.isBlank(element.getName())) {
                String sName = ceEls.get(i - 1).select("> h3.sName a").first().ownText().trim();
                element.setName(sName);
            }
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
            SuningBestSaleListWorker worker = new SuningBestSaleListWorker(destFile, element, ce);
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
