package com.lezo.mall.blade.require.top.worker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.common.SiteConstant;
import com.lezo.mall.blade.require.top.po.TopBucket;

public class AmazonBestSaleSkuWorker implements Runnable {
    private Logger log = Logger.getLogger(AmazonBestSaleSkuWorker.class);
    private static final Pattern NUM_REG = Pattern.compile("[0-9]+");
    private static final Pattern PRICE_REG = Pattern.compile("[0-9.]+");
    private String crumb;
    private String cateName;
    private String cateUrl;
    private String level;
    private String dirPath;
    private Integer siteId = SiteConstant.SITE_AMAZON;

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
        int maxRetry = 5;

        List<TopBucket> totalList = new ArrayList<TopBucket>();
        int curNum = 1;
        int maxPage = 5;
        while (curNum <= maxPage) {
            String sUrl = getPageUrl(cateUrl, curNum);
            Document dom = getDocument(sUrl, maxRetry);
            List<TopBucket> topBuckets = getDataList(dom);
            for (TopBucket top : topBuckets) {
                top.setPageNum(curNum);
                totalList.add(top);
            }
            dom = getDocument(sUrl + "&isAboveTheFold=0", maxRetry);
            topBuckets = getDataList(dom);
            for (TopBucket top : topBuckets) {
                top.setPageNum(curNum);
                totalList.add(top);
            }
            if (totalList.size() % 20 != 0 || totalList.size() >= 100) {
                break;
            }
            curNum++;
        }

        String fileName = crumb.trim().replace("\\/", "-").replace(AmazonBestSaleListWorker.CODE_SEPERATOR, "_");
        fileName = Math.abs(fileName.hashCode()) + "_" + System.currentTimeMillis() + ".txt";
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        // 不序列化null
        try {
            String dataString = mapper.writeValueAsString(totalList);
            if (StringUtils.isNotBlank(dataString)) {
                dataString += "\n";
                File destFile = new File(dirPath, fileName);
                FileUtils.writeStringToFile(destFile, dataString, "UTF-8");
                log.info("fetch cate:" + cateName + ",level:" + level + ",count:" + totalList.size()
                        + ",toFile:" + destFile + ",len:" + dataString.length());
            } else {
                log.warn("fetch empty.cate:" + cateName + ",level:" + level + ",count:" + totalList.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPageUrl(String url, int pageNum) {
        if (url.indexOf("?") < 0) {
            url = url + "?ie=UTF8&pg=" + pageNum + "&ajax=1";
        } else {
            url = url.replaceAll("pg=[0-9]+", "pg=" + pageNum);
        }
        return url;
    }

    private List<TopBucket> getDataList(Document dom) {
        Elements rowEls = dom.select("[id^=zg_] div.zg_itemRow");
        if (rowEls.isEmpty()) {
            return Collections.emptyList();
        }
        List<TopBucket> dataList = new ArrayList<TopBucket>();
        Integer pageNum = 0;
        Date fetchDate = new Date();
        for (Element ele : rowEls) {
            TopBucket topBucket = new TopBucket();
            Elements rankEls = ele.select("span.zg_rankNumber");
            Integer curNum = toNum(rankEls.first().ownText());
            topBucket.setRankNum(curNum);
            topBucket.setFromUrl(cateUrl);
            topBucket.setCrumb(this.crumb);
            topBucket.setPageNum(pageNum);
            Elements cmmEls = ele.select("div.zg_reviews span.crAvgStars a");
            if (!cmmEls.isEmpty()) {
                curNum = toNum(cmmEls.first().ownText());
                topBucket.setCommentNum(curNum);
            }
            topBucket.setFetchTime(fetchDate);
            Elements priceEls = ele.select("p.priceBlock:contains(特价) span.price b");
            if (priceEls.isEmpty()) {
                priceEls = ele.select("p.priceBlock:contains(价格) span.price b");
            }
            // 无价格，则为：目前无货，欢迎选购其他类似产品
            if (!priceEls.isEmpty()) {
                String sPrice = priceEls.first().ownText();
                Matcher matcher = PRICE_REG.matcher(sPrice);
                if (matcher.find()) {
                    topBucket.setPrice(Float.valueOf(matcher.group()));
                }
            }
            Elements titleEls = ele.select("div.zg_title a[href]");
            if (!titleEls.isEmpty()) {
                topBucket.setProductUrl(titleEls.first().absUrl("href").trim());
                topBucket.setTitle(titleEls.first().ownText());
            }
            topBucket.setSiteId(siteId);
            dataList.add(topBucket);
        }
        return dataList;
    }

    private Integer toNum(String text) {
        Matcher matcher = NUM_REG.matcher(text);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group());
        }
        return null;
    }
}
