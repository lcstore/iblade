package com.lezo.mall.blade;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class AmazonBestSaleListWorker implements Runnable {
    private Logger log = Logger.getLogger(AmazonBestSaleListWorker.class);
    private static String CODE_SEPERATOR = "->";
    private CategoryElement element;
    private File destFile;

    public AmazonBestSaleListWorker(File destFile, CategoryElement element) {
        super();
        this.destFile = destFile;
        this.element = element;
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

    private void addChildren(CategoryElement parent) {
        String url = parent.getUrl();
        System.err.println(parent.getName() + ",level:" + parent.getLevel() + ",url:" + url);
        Document dom = getDocument(url, 5);
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

    private Document getDocument(String url, int maxRetry) {
        int count = 0;
        while (count++ < maxRetry) {
            try {
                Document dom =
                        Jsoup.connect(url)
                                .userAgent(
                                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:40.0) Gecko/20100101 Firefox/40.0")
                                .get();
                return dom;
            } catch (IOException e) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                }
                log.warn("url:" + url + ",retry:" + count, e);
            }
        }
        return null;
    }

    @Override
    public void run() {
        long startMills = System.currentTimeMillis();
        int index = 0;
        CategoryElement cce = element;
        addChildren(element);
        List<CategoryElement> leafList = new ArrayList<CategoryElement>();
        getLeafs(cce, leafList);
        List<String> dataList = new ArrayList<String>();
        String separator = "\t";
        for (CategoryElement leaf : leafList) {
            StringBuilder sb = new StringBuilder();
            sb.append(leaf.getUrl());
            sb.append(separator);
            sb.append(leaf.getName());
            sb.append(separator);
            sb.append(leaf.getLevel());
            sb.append(separator);
            sb.append(leaf.getCode());
            dataList.add(sb.toString());
        }
        try {
            FileUtils.writeLines(destFile, "UTF-8", dataList);
        } catch (IOException e) {
            log.error("", e);
        }
        long costMills = System.currentTimeMillis() - startMills;
        log.info("done,destFile:" + destFile + ",cost:" + costMills);
    }
}
