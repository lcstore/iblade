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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.log4j.Log4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.require.top.po.CategoryElement;
import com.lezo.mall.blade.require.top.worker.DangBestSaleListWorker;

@Log4j
public class DangBestSaleListMain {

    public static void main(String[] args) throws Exception {
        String destPath = System.getProperty("dest", "./data/dangdang/top/cate/");
        String skipString = System.getProperty("skip", "true");
        boolean skipDone = "true".equals(skipString) ? true : false;
        String url = "http://category.dangdang.com/?ref=www-0-C";
        long startMills = System.currentTimeMillis();
        Document dom =
                Jsoup.connect(url)
                        .header("Accept-Encoding", "gzip, deflate")
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:41.0) Gecko/20100101 Firefox/41.0")
                        .get();
        Elements ceEls = dom.select("div[id^=f][name]");
        List<CategoryElement> cElements = new ArrayList<CategoryElement>();
        Set<String> doneSet = getDoneSet(new File(destPath));
        ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        Pattern oReg = Pattern.compile("cid[0-9]+\\.html");
        Map<String, String> map = new HashMap<String, String>();
        map.put("cid4003844.html", "服装");
        map.put("cid4003872.html", "鞋");
        map.put("cid4003728.html", "运动户外");
        map.put("cid4001829.html", "箱包皮具");
        map.put("cid4007338.html", "珠宝饰品");
        map.put("cid4006453.html", "手表/眼镜礼品");
        map.put("cid4002074.html", "时尚美妆");
        // map.put("cid4008388.html", "当当优品");
        map.put("cid4001940.html", "母婴用品");
        map.put("cid4004344.html", "童装童鞋");
        map.put("cid4004866.html", "孕婴服饰");
        map.put("cid4002061.html", "玩具");
        map.put("cid4003900.html", "家居日用");
        map.put("cid4003760.html", "家具装饰");
        map.put("cid4006497.html", "手机通讯");
        map.put("cid4003613.html", "数码影音");
        map.put("cid4003819.html", "电脑办公");
        map.put("cid4001001.html", "家用电器");
        map.put("cid4002429.html", "汽车用品");
        map.put("cid4002145.html", "食品");
        map.put("cid4005284.html", "营养/保健成人");
        // map.put("cp01.00.00.00.00.00.html", "图书");
        // map.put("list_98.00.00.00.htm", "数字");
        // map.put("", "音像");
        for (Element ce : ceEls) {
            Elements ct1Els = ce.select("div.cfied-pic a[href]");
            String sUrl = ct1Els.first().attr("href");
            Matcher matcher = oReg.matcher(sUrl);
            if (!matcher.find()) {
                continue;
            }
            String sMark = matcher.group();
            String sName = map.get(sMark);
            if (sName == null) {
                continue;
            }
            CategoryElement element = new CategoryElement();
            element.setUrl(sUrl);
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
            DangBestSaleListWorker worker = new DangBestSaleListWorker(destFile, element, ce);
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
