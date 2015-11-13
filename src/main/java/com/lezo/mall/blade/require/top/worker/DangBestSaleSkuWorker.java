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
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.common.SiteConstant;
import com.lezo.mall.blade.require.top.po.TopBucket;

public class DangBestSaleSkuWorker implements Runnable {
    private Logger log = Logger.getLogger(DangBestSaleSkuWorker.class);
    private static final Pattern NUM_REG = Pattern.compile("[0-9]+");
    private static final Pattern PRICE_REG = Pattern.compile("[0-9.]+");
    private String crumb;
    private String cateName;
    private String cateUrl;
    private String level;
    private String dirPath;
    private Integer siteId = SiteConstant.SITE_DANGDANG;
    private int maxRetry = 5;

    public DangBestSaleSkuWorker(String crumb, String cateName, String cateUrl, String level, String dirPath) {
        super();
        this.crumb = crumb;
        this.cateName = cateName;
        this.cateUrl = cateUrl;
        this.level = level;
        this.dirPath = dirPath;
    }

    private Document getDocument(String url, int maxRetry, Method method) {
        String body = getBody(url, maxRetry, method);
        return Jsoup.parse(body, url);
    }

    private String getBody(String url, int maxRetry, Method method) {
        int count = 0;
        while (count++ < maxRetry) {
            try {
                Response res = getResopne(url, maxRetry, method);
                return res.body();
            } catch (Exception e) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                }
                log.warn("url:" + url + ",retry:" + count, e);
            }
        }
        return null;
    }

    private Response getResopne(String url, int maxRetry, Method method) throws IOException {
        Response response =
                Jsoup.connect(url)
                        .userAgent(
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:40.0) Gecko/20100101 Firefox/40.0")
                        .header("Accept-Encoding", "gzip, deflate, sdch")
                        .header("Accept", "*/*")
                        .header("Cookie",
                                "loc=2_1_310000_310100_310101_29313; prid=29313_2621; wsid=1; Hm_lvt_23bc0079ed4b2d06e5d5332ef5de1ec7=1444821395,1445333463; __jda=1982585.538562759.1442302572.1445335498.1445344965.7; visitkey=42984858774391402; y_guid=1404168C456CA400; __jdu=330eeb90-3c61-4f05-a8ae-74c8b0fa4128; __jdv=1982585%7Cbaidu%7C-%7Corganic%7Cnot%20set; y_source=1400000; Hm_lpvt_23bc0079ed4b2d06e5d5332ef5de1ec7=1445344965; __jdc=1982585; y_track=1400000-100087100-0-706131211012.3; y_rfid=22019971%7C7061312; __jdb=1982585.63.538562759%7C7.1445344965; rgStat=p%3Dsearch.51buy.com%26regionID1%3D2016%26regionID2%3D1005%26regionID3%3D2%26regionID4%3D%26siteid%3D1%26srcid%3D104%26spvid%3D1429848587743914021445344960725514168653%26uid%3D%26YTAG%3D3.706131211012%26referurl%3Dhttp%253A%252F%252Fsearchex.yixun.com%252F706124t706131-1-%252Fall%252F----1--2---------.html%26curPage%3D1%26ss%3D%26hitNum%3D122%26defCommNum%3D40%26defRowCommNum%3D4%26extraMsg%3D29313%26otherClick%3D1_0_%u70B9%u51FB%u6309%u9500%u91CF%u4ECE%u9AD8%u5230%u4F4E%u6392%u5E8F")
                        .ignoreContentType(true)
                        .method(method)
                        .execute();
        return response;
    }

    @Override
    public void run() {
        List<TopBucket> totalList = new ArrayList<TopBucket>();
        int curNum = 1;
        int maxCount = 120;
        // cateUrl = "http://category.dangdang.com/cid4002276.html";
        while (true) {
            String sUrl = getPageUrl(cateUrl, curNum);
            Document dom = getDocument(sUrl, maxRetry, Method.GET);
            dom.setBaseUri(sUrl);
            List<TopBucket> topBuckets = getDataList(dom);
            for (TopBucket top : topBuckets) {
                top.setPageNum(curNum);
                totalList.add(top);
                top.setRankNum(totalList.size());
            }
            if (totalList.size() >= maxCount) {
                break;
            }
            Elements nextEls = dom.select("li.next a[href]:contains(下一页)");
            if (nextEls.isEmpty()) {
                break;
            } else {
                cateUrl = nextEls.first().absUrl("href");
                if (StringUtils.isBlank(cateUrl)) {
                    break;
                }
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

    private Document toSaleDecDom(Document dom) {
        Elements saleEls = dom.select("#list.sort div.sort_cate a.sort_cate_lk:has(span:contains(销量))");
        if (saleEls.isEmpty()) {
            return dom;
        }
        if (saleEls.first().hasClass("sort_cate_on")) {
            return dom;
        }
        String sSaleDescUrl = saleEls.first().absUrl("href");
        return getDocument(sSaleDescUrl, maxRetry, Method.GET);
    }

    private String getPageUrl(String url, int pageNum) {
        if (pageNum > 1) {
            return url;
        }
        String sMark = ".html";
        int index = url.indexOf(sMark);
        String sSaleDesc = "-srsort_sale_amt_desc-tteq0";
        if (index < 0) {
            log.warn("url:" + url);
            return url + sSaleDesc;
        }
        url = url.substring(0, index);
        url = url + sSaleDesc + sMark;
        return url;
    }

    private List<TopBucket> getDataList(Document dom) {
        Elements rowEls = dom.select("ul[id^=component_] li[id][class^=line]");
        if (rowEls.isEmpty()) {
            return Collections.emptyList();
        }
        List<TopBucket> dataList = new ArrayList<TopBucket>();
        Integer pageNum = 0;
        Date fetchDate = new Date();
        Integer curNum = null;
        for (Element ele : rowEls) {
            TopBucket topBucket = new TopBucket();
            topBucket.setFromUrl(dom.baseUri());
            topBucket.setCrumb(this.crumb);
            topBucket.setPageNum(pageNum);
            Elements cmmEls = ele.select("p.star a[href]:contains(评论)");
            if (!cmmEls.isEmpty()) {
                curNum = toNum(cmmEls.first().text());
                topBucket.setCommentNum(curNum);
            }
            topBucket.setFetchTime(fetchDate);
            Elements priceEls = ele.select("p.price span.price_n");
            if (!priceEls.isEmpty()) {
                String sPrice = priceEls.first().ownText();
                Matcher matcher = PRICE_REG.matcher(sPrice);
                if (matcher.find()) {
                    topBucket.setPrice(Float.valueOf(matcher.group()));
                }
            }
            Elements titleEls = ele.select("p.name a[href][title][target=_blank]");
            topBucket.setProductUrl(titleEls.first().absUrl("href").trim());
            int index = topBucket.getProductUrl().indexOf("#");
            if (index > 0) {
                topBucket.setProductUrl(topBucket.getProductUrl().substring(0, index));
            }
            topBucket.setTitle(titleEls.first().attr("title"));
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
