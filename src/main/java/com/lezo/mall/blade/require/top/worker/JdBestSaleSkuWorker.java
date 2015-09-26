package com.lezo.mall.blade.require.top.worker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.require.top.po.TopBucket;

public class JdBestSaleSkuWorker implements Runnable {
    private Logger log = Logger.getLogger(JdBestSaleSkuWorker.class);
    private static final Pattern NUM_REG = Pattern.compile("[0-9]+");
    private static final Pattern PRICE_REG = Pattern.compile("[0-9.]+");
    private String crumb;
    private String cateName;
    private String cateUrl;
    private String level;
    private String dirPath;
    private Integer siteId = 1001;
    private int maxRetry = 5;

    public JdBestSaleSkuWorker(String crumb, String cateName, String cateUrl, String level, String dirPath) {
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
                        .cookies(getCookies())
                        .ignoreContentType(true)
                        .method(method)
                        .execute();
        return response;
    }

    private Map<String, String> getCookies() {
        Map<String, String> cookieMap = new HashMap<String, String>();
        cookieMap.put("__jda", "95931165.580577879.1416135846.1416135846.1416135846.1");
        cookieMap.put("__jdb", "95931165.1.580577879|1.1416135846");
        cookieMap.put("__jdc", "95931165");
        cookieMap.put("__jdv", "95931165|direct|-|none|-");
        return cookieMap;
    }

    @Override
    public void run() {
        List<TopBucket> totalList = new ArrayList<TopBucket>();
        int curNum = 1;
        int maxPage = 1;
        while (curNum <= maxPage) {
            String sUrl = getPageUrl(cateUrl, curNum);
            Document dom = getDocument(sUrl, maxRetry, Method.GET);
            List<TopBucket> topBuckets = getDataList(dom);
            for (TopBucket top : topBuckets) {
                top.setPageNum(curNum);
                totalList.add(top);
            }
            addPrices(topBuckets);
            curNum++;
        }

        String fileName = crumb.trim().replace("\\/", "-").replace(AmazonBestSaleListWorker.CODE_SEPERATOR, "_");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        // 不序列化null
        try {
            String dataString = mapper.writeValueAsString(totalList);
            File destFile = new File(dirPath, fileName + ".txt");
            FileUtils.writeStringToFile(destFile, dataString, "UTF-8");
            log.info("fetch cate:" + cateName + ",level:" + level + ",count:" + totalList.size()
                    + ",toFile:" + destFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addPrices(List<TopBucket> topBuckets) {
        Pattern codeReg = Pattern.compile("item.jd.com[^0-9]*?([0-9]{7})[^0-9]*");
        Map<String, TopBucket> code2BucketMap = new HashMap<String, TopBucket>();
        for (TopBucket top : topBuckets) {
            Matcher matcher = codeReg.matcher(top.getProductUrl());
            if (matcher.find()) {
                String pCode = matcher.group(1);
                code2BucketMap.put(pCode, top);
            }
        }
        if (code2BucketMap.isEmpty()) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        for (String code : code2BucketMap.keySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append("J_");
            sb.append(code);
        }
        String sGetPriceUrl =
                "http://p.3.cn/prices/mgets?skuIds=" + sb.toString()
                        + "&type=1&area=1_72_2799_0&callback=jQuery3841544&_=" + System.currentTimeMillis();
        sGetPriceUrl =
                "http://p.3.cn/prices/mgets?&callback=jQuery3677497&my=list_price&type=1&area=1_72_2799_0&skuIds="
                        + sb.toString();
        String source = getBody(sGetPriceUrl, maxRetry, Method.GET);
        int fromIndex = source.indexOf("(") + 1;
        int toIndex = source.lastIndexOf(")");
        source = source.substring(fromIndex, toIndex);
        try {
            JSONArray jArray = new JSONArray(source);
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject jObj = jArray.getJSONObject(i);
                String pCode = jObj.getString("id");
                pCode = pCode.replace("J_", "");
                TopBucket bucket = code2BucketMap.get(pCode);
                if (bucket != null) {
                    bucket.setPrice(Float.valueOf(jObj.get("p").toString()));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getPageUrl(String url, int pageNum) {
        if (url.indexOf("?") < 0) {
            url = url + "?&delivery=1&page=" + pageNum + "&sort=sort_totalsales15_desc";
        }
        if (url.indexOf("delivery=") < 0) {
            url += "&delivery=1&sort_totalsales15_desc";
        }
        url = url.replaceAll("page=[0-9]+", "page=" + pageNum);
        return url;
    }

    private List<TopBucket> getDataList(Document dom) {
        Elements rowEls = dom.select("div#plist ul.gl-warp li.gl-item");
        if (rowEls.isEmpty()) {
            return Collections.emptyList();
        }
        List<TopBucket> dataList = new ArrayList<TopBucket>();
        Integer pageNum = 0;
        Date fetchDate = new Date();
        int rankNum = 0;
        Integer curNum = null;
        for (Element ele : rowEls) {
            TopBucket topBucket = new TopBucket();
            topBucket.setRankNum(++rankNum);
            topBucket.setFromUrl(dom.baseUri());
            topBucket.setCrumb(this.crumb);
            topBucket.setPageNum(pageNum);
            Elements cmmEls = ele.select("div.p-commit");
            if (!cmmEls.isEmpty()) {
                curNum = toNum(cmmEls.first().text());
                topBucket.setCommonNum(curNum);
            }
            topBucket.setFetchTime(fetchDate);
            Elements priceEls = ele.select("div.p-price strong.J_price");
            if (!priceEls.isEmpty()) {
                String sPrice = priceEls.first().ownText();
                sPrice = sPrice.replaceAll("￥", "");
                sPrice = sPrice.replaceAll(",", "");
                Matcher matcher = PRICE_REG.matcher(sPrice);
                if (matcher.find()) {
                    topBucket.setPrice(Float.valueOf(matcher.group()));
                }
            }
            Elements titleEls = ele.select("div.p-name a[href][title][target=_blank]");
            topBucket.setProductUrl(titleEls.first().absUrl("href").trim());
            topBucket.setTitle(titleEls.first().text());
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
