package com.lezo.mall.blade.require.top.worker;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lezo.mall.blade.common.SiteConstant;
import com.lezo.mall.blade.require.top.po.TopBucket;

public class YhdBestSaleSkuWorker implements Runnable {
    private Logger log = Logger.getLogger(YhdBestSaleSkuWorker.class);
    private static final Pattern NUM_REG = Pattern.compile("[0-9]+");
    private static final Pattern PRICE_REG = Pattern.compile("[0-9.]+");
    private static int maxRetry = 5;
    private String crumb;
    private String cateName;
    private String cateUrl;
    private String level;
    private String dirPath;
    private Integer siteId = SiteConstant.SITE_YHD;

    public YhdBestSaleSkuWorker(String crumb, String cateName, String cateUrl, String level, String dirPath) {
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
                String html =
                        Jsoup.connect(url)
                                .header("Accept-Encoding", "gzip, deflate")
                                .header("Cookie",
                                        "provinceId=1; guid=Y8UVSWYNZPJZHS3MY9Q5EDUGWDT5EANQESHW; gray=578446; tma=40580330.63589774.1436931022776.1441612547583.1442985900171.7; tmd=17.40580330.63589774.1436931022776.; search_browse_history=22588%2C34748853%2C33939679%2C30070606%2C33939842%2C18090984%2C13445367%2C20862454%2C40589293%2C1086272; wide_screen=1; cart_num=0; pms_cart=\"\"; grouponAreaId=3; cid=NWFNMTk0NnBENTM2N2NENzk2OHpDNzk4MXhaMTM2MnBKNDU2M2JXOTk0NHlSNzE4; uname=lcstore; yihaodian_uid=42420486; abtest=80; _ga=GA1.2.723018310.1442985251; unionType=1; cart_cookie_uuid=a8ce82ee-abca-48f5-b24d-76c1491a9945143796570406526960311480216771; gc=63130572; bfd_g=8a28c81f66bd0659000024bd00034d28556faf0a; msessionid=7S2JVXCCQ3ER5A7ZWE9VK9GGE1X5HEE59S3M; gla=1.-10_0_0; JSESSIONID=DBA1D153C5EA679B63B9D00AA239F3FF;")
                                .userAgent(
                                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:40.0) Gecko/20100101 Firefox/40.0")
                                .referrer("http://www.yhd.com/")
                                .method(Method.GET).execute().body();
                html = turnHtml(html);
                return Jsoup.parse(html, url);
            } catch (Exception e) {
                int timeout = new Random().nextInt(5);
                timeout++;
                log.warn("url:" + url + ",retry:" + count + ",sleep seconds:" + timeout, e);
                try {
                    TimeUnit.SECONDS.sleep(timeout);
                } catch (InterruptedException e1) {
                }
            }
        }
        return null;
    }

    @Override
    public void run() {
        List<TopBucket> totalList = new ArrayList<TopBucket>();
        int curNum = 1;
        int maxRank = 120;
        // cateUrl = "http://list.yhd.com/ctg/s2/c22882-0/b/a101419-s1-v0-p1-price-d0-f0-m1-rt0-pid-mid0-k//";
        while (true) {
            String sUrl = cateUrl;
            sUrl = turnUrl(sUrl);
            Document dom = getDocument(sUrl, maxRetry);
            String isLargeImg = "1";
            Pattern largeReg = Pattern.compile("largeImgCategoryFlag[^0-9]*?([0-9]+).*?;");
            String html = dom.html();
            Matcher matcher = largeReg.matcher(html);
            if (matcher.find()) {
                isLargeImg = matcher.group(1);
            }
            String fashionCateType = "1";
            Pattern fashionReg = Pattern.compile("fashionCateType[^0-9]*?([0-9]+).*?;");
            matcher = fashionReg.matcher(html);
            if (matcher.find()) {
                fashionCateType = matcher.group(1);
            }
            String firstPgAdSize = "0";
            Pattern firstPgAdSizeReg = Pattern.compile("firstPgAdSize[^0-9]*?([0-9]+).*?;");
            matcher = firstPgAdSizeReg.matcher(html);
            if (matcher.find()) {
                firstPgAdSize = matcher.group(1);
            }
            dom = toSaleDescDom(dom);
            cateUrl = dom.baseUri();
            List<TopBucket> topBuckets = getDataList(dom);
            for (TopBucket top : topBuckets) {
                top.setPageNum(curNum);
                totalList.add(top);
                top.setRankNum(totalList.size());
            }
            if (topBuckets.size() >= 30) {
                String template = "1";
                Elements jsonEls = dom.select("#jsonValue");
                if (!jsonEls.isEmpty()) {
                    String sJson = jsonEls.first().val();
                    JSONObject jObj = JSON.parseObject(sJson);
                    if (jObj != null) {
                        template = jObj.getString("search_template");
                    }
                }
                String adProductIdListStr = "";
                Elements adPIdEls = dom.select("#adProductIdListStr");
                if (!adPIdEls.isEmpty()) {
                    adProductIdListStr = adPIdEls.first().val();
                }
                String curAdBlockStartIndex = "0";
                Elements curAdBlockEls = dom.select("#curAdBlockStartIndex");
                if (!curAdBlockEls.isEmpty()) {
                    curAdBlockStartIndex = curAdBlockEls.first().val();
                }
                sUrl =
                        dom.baseUri()
                                +
                                "&isGetMoreProducts=1&moreProductsDefaultTemplate="
                                + template
                                + "&isLargeImg=" + isLargeImg + "&nextAdIndex=" + curAdBlockStartIndex
                                + "&adProductIdListStr="
                                + adProductIdListStr
                                + "&fashionCateType=" + fashionCateType + "&firstPgAdSize=" + firstPgAdSize + "&_="
                                + System.currentTimeMillis();
                dom = getDocument(sUrl, maxRetry);
                topBuckets = getDataList(dom);
                for (TopBucket top : topBuckets) {
                    top.setPageNum(curNum);
                    totalList.add(top);
                    top.setRankNum(totalList.size());
                }
            } else {
                break;
            }
            if (totalList.size() >= maxRank) {
                break;
            }
            Elements nextEls = dom.select("a[href].page_next");
            if (nextEls.isEmpty()) {
                break;
            } else {
                cateUrl = nextEls.first().absUrl("href");
            }
        }

        String fileName = crumb.trim().replace("\\/", "-").replace(AmazonBestSaleListWorker.CODE_SEPERATOR, "_");
        fileName = Math.abs(fileName.hashCode()) + "_" + System.currentTimeMillis() + ".txt";
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        // 不序列化null
        try {
            if (CollectionUtils.isNotEmpty(totalList)) {
                String dataString = mapper.writeValueAsString(totalList);
                dataString += "\n";
                File destFile = new File(dirPath, fileName);
                FileUtils.writeStringToFile(destFile, dataString, "UTF-8");
                log.info("fetch cate:" + cateName + ",level:" + level + ",count:" + totalList.size()
                        + ",toFile:" + destFile + ",len:" + dataString.length());
            } else {
                log.warn("fetch empty.cate:" + cateName + ",url:" + cateUrl + ",level:" + level + ",count:"
                        + totalList.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Document toSaleDescDom(Document dom) {
        Elements saleEls = dom.select("div#rankOpDiv div.sort_b a:contains(销量)");
        if (saleEls.isEmpty()) {
            return dom;
        }
        if (saleEls.first().hasClass("cur")) {
            return dom;
        }
        String destUrl = saleEls.first().absUrl("url");
        destUrl = turnUrl(destUrl);
        return getDocument(destUrl, maxRetry);
    }

    private String turnHtml(String html) {
        if (html.startsWith("jsonp")) {
            int beginIndex = html.indexOf("(");
            int endIndex = html.lastIndexOf(")");
            html = html.substring(beginIndex + 1, endIndex);
        }
        if (html.startsWith("{") && html.endsWith("}")) {
            JSONObject sObject = JSON.parseObject(html);
            return sObject.getString("value");
        }
        return html;
    }

    private String turnUrl(String url) {
        if (url.endsWith("|")) {
            url = url.substring(0, url.length() - 1);
        }
        url = url.endsWith("/") ? url : url + "/";
        Pattern sortReg = Pattern.compile("(-s[0-9]-)[0-9a-z\\-]+(-f[0-9a-z]+-)");
        Matcher sMatcher = sortReg.matcher(url);
        if (sMatcher.find()) {
            String sGroup = sMatcher.group(1);
            String fGroup = sMatcher.group(2);
            url = url.replace(sGroup, "-s2-");
            url = url.replace(fGroup, "-f0d6-");
        }
        if (!url.contains("?callback=jsonp")) {
            url += "?callback=jsonp" + System.currentTimeMillis();
        }
        if (url.indexOf("searchVirCateAjax") > 0) {
            url = url.replace("-price-d0-f0-m1-rt0-pid-mid0-k", "-price-d0-mid0--f0d6");
        }
        url = url.replace("|/?", "?");
        return url;
    }

    private List<TopBucket> getDataList(Document dom) {
        Elements rowEls = dom.select("div[id^=producteg].mod_search_pro,[id^=producteg].search_item");
        if (rowEls.isEmpty()) {
            return Collections.emptyList();
        }
        List<TopBucket> dataList = new ArrayList<TopBucket>();
        Integer pageNum = 0;
        Date fetchDate = new Date();
        for (Element ele : rowEls) {
            TopBucket topBucket = new TopBucket();
            topBucket.setFromUrl(cateUrl);
            topBucket.setCrumb(this.crumb);
            topBucket.setPageNum(pageNum);
            Elements cmmEls = ele.select("a[id^=pdlinkcomment_]");
            if (!cmmEls.isEmpty()) {
                Integer cmmNum = toNum(cmmEls.first().ownText());
                topBucket.setCommentNum(cmmNum);
            }
            topBucket.setFetchTime(fetchDate);
            Elements priceEls = ele.select("[id^=price]");
            if (!priceEls.isEmpty()) {
                String sPrice = priceEls.first().text();
                Matcher matcher = PRICE_REG.matcher(sPrice);
                if (matcher.find()) {
                    topBucket.setPrice(Float.valueOf(matcher.group()));
                }
            }
            Elements titleEls = ele.select("a[id^=pdlink][title][href][target]");
            if (!titleEls.isEmpty()) {
                topBucket.setProductUrl(titleEls.first().absUrl("href").trim());
                topBucket.setTitle(titleEls.first().attr("title"));
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
