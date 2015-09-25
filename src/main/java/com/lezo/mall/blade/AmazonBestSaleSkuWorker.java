package com.lezo.mall.blade;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AmazonBestSaleSkuWorker implements Runnable {
    private Logger log = Logger.getLogger(AmazonBestSaleSkuWorker.class);
    private String crumb;
    private String cateName;
    private String cateUrl;
    private String level;
    private String dirPath;

    public AmazonBestSaleSkuWorker(String crumb, String cateName, String cateUrl, String level, String dirPath) {
        super();
        this.crumb = crumb;
        this.cateName = cateName;
        this.cateUrl = cateUrl;
        this.level = level;
        this.dirPath = dirPath;
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

        String fileName = crumb.trim().replace("/", "-").replace("", "_");
        File destFile = new File(dirPath, fileName + ".txt");

    }
}
