package com.lezo.mall.blade.require.top.worker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lezo.mall.blade.common.SiteConstant;
import com.lezo.mall.blade.require.top.po.TopBucket;

public class SuningBestSaleSkuWorker implements Runnable {
    private Logger log = Logger.getLogger(SuningBestSaleSkuWorker.class);
    private static final Pattern NUM_REG = Pattern.compile("[0-9]+");
    private String crumb;
    private String cateName;
    private String cateUrl;
    private String level;
    private String dirPath;
    private Integer siteId = SiteConstant.SITE_SUNING;
    private int maxRetry = 3;

    public SuningBestSaleSkuWorker(String crumb, String cateName, String cateUrl, String level, String dirPath) {
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
                                .header("Accept-Encoding", "gzip, deflate")
                                .userAgent(
                                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:40.0) Gecko/20100101 Firefox/40.0")
                                .referrer("http://www.suning.com/")
                                .header("Cookie",
                                        "smhst=124552235a127891140a103126787a101951359a102076668; SN_CITY=10_010_1000000_9017_01_10106_3_0%7C190_755_1000051_9051_01_10346_1_1; cityId=9017; districtId=10106; _snma=1%7C144232146940421674%7C1442321469404%7C1445309644772%7C1445309647233%7C88%7C10; _ga=GA1.2.1507088533.1442321470; __wmv=1444965697.13; sesab=b; sesabv=34%2320%3A80; WC_PERSISTENT=Qz%2fwizjlm83rDOHnwzU77ZDinWw%3d%0a%3b2015%2d10%2d16+11%3a22%3a18%2e286%5f1444965738180%2d2072006%5f10052; _snsr=direct%7Cdirect%7C%7C%7C; cart_abtest_num=18; __utma=1.1507088533.1442321470.1445244871.1445309502.3; __utmz=1.1445309502.3.2.utmcsr=list.suning.com|utmccn=(referral)|utmcmd=referral|utmcct=/0-500239-0-0-0-9017-0-8-0-0.html; Hm_lvt_cb12e33a15345914e449a2ed82a2a216=1445260636; _customId=65eiee776936; _snmc=1; _snmb=144530939981389865%7C1445309647261%7C1445309647236%7C8; _snmp=14453096472322292; __wms=1445311200; _gat=1; authId=siFAE65CEFBE3B805A4F8429A6246D8E43; cart_abtest=B; _snck=14453095822398307; __utmb=1.2.10.1445309502; __utmc=1; __utmt=1")
                                .get();
                return dom;
            } catch (IOException e) {
                int timeout = new Random().nextInt(5);
                timeout++;
                try {
                    TimeUnit.SECONDS.sleep(timeout);
                } catch (InterruptedException e1) {
                }
                log.warn("url:" + url + ",retry:" + count + ",sleep seconds:" + timeout, e);
            }
        }
        return null;
    }

    @Override
    public void run() {
        int maxRetry = 5;

        List<TopBucket> totalList = new ArrayList<TopBucket>();
        int curNum = 1;
        int maxPage = 4;
        while (curNum <= maxPage) {
            String sUrl = getPageUrl(cateUrl, curNum);
            cateUrl = sUrl;
            Document dom = getDocument(sUrl, maxRetry);
            List<TopBucket> topBuckets = getDataList(dom);
            for (TopBucket top : topBuckets) {
                top.setPageNum(curNum);
                totalList.add(top);
                top.setRankNum(totalList.size());
            }
            Elements nextEls = dom.select("a#nextPage.next[href]:contains(下一页)");
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
            if (CollectionUtils.isNotEmpty(totalList)) {
                String dataString = mapper.writeValueAsString(totalList);
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
        Pattern oReg = Pattern.compile("(list.suning.com).?([0-9\\-]+)(?=\\.html)");
        Matcher matcher = oReg.matcher(url);
        if (matcher.find()) {
            if (pageNum > 1) {
                return url;
            }
            String sParam = matcher.group(2);
            String[] paramArr = sParam.split("-");
            String[] defaultArr = "0-340574-0-0-1-9017-0-8-0-0-28798".split("-");
            for (int i = 0; i < paramArr.length; i++) {
                defaultArr[i] = paramArr[i];
            }
            defaultArr[4] = "1";
            int len = defaultArr.length;
            len = paramArr.length < defaultArr.length ? len - 1 : len;
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < len; j++) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(defaultArr[j]);
            }
            url = url.replace(sParam, sb.toString());
        } else {
            String sMark = "search.suning.com/emall/";
            int index = url.indexOf(sMark);
            if (index > 0) {
                if (!url.contains("search.do")) {
                    String remain = url.substring(index + sMark.length());
                    remain = remain.replaceAll("/", "&");
                    url = "http://search.suning.com/emall/search.do?keyword=" + remain;
                } else {
                    url += "&cityId=9017&pg=01&cp=0&il=0&st=8&iy=0&n=1&ct=1";
                }
                if (!url.contains("&st=8")) {
                    url += "&cityId=9017&pg=01&cp=0&il=0&st=8&iy=0&n=1&ct=1";
                }
            }
        }
        return url;
    }

    private List<TopBucket> getDataList(Document dom) {
        Elements rowEls = dom.select("div#proShow ul.items.clearfix li.item");
        if (rowEls.isEmpty()) {
            return Collections.emptyList();
        }
        List<TopBucket> dataList = new ArrayList<TopBucket>();
        Integer pageNum = 0;
        Date fetchDate = new Date();
        Map<String, TopBucket> key2BeanMap = new HashMap<String, TopBucket>();
        Pattern codeReg = Pattern.compile("([0-9]+)\\.html");
        for (Element ele : rowEls) {
            TopBucket topBucket = new TopBucket();
            topBucket.setFromUrl(cateUrl);
            topBucket.setCrumb(this.crumb);
            topBucket.setPageNum(pageNum);
            Elements cmmEls = ele.select("a.comment span.com-cnt:contains(评论)");
            if (!cmmEls.isEmpty()) {
                Integer cmmNum = toNum(cmmEls.first().ownText());
                topBucket.setCommentNum(cmmNum);
            }
            topBucket.setFetchTime(fetchDate);
            Elements titleEls = ele.select("div.i-name a[name][title][href][target]");
            if (!titleEls.isEmpty()) {
                topBucket.setProductUrl(titleEls.first().absUrl("href").trim());
                topBucket.setTitle(titleEls.first().ownText());
            }
            Matcher matcher = codeReg.matcher(topBucket.getProductUrl());
            if (matcher.find()) {
                String key = matcher.group(1);
                String source = "000000000000000000";
                key = source + key;
                int fromIndex = key.length() - source.length();
                key = key.substring(fromIndex);
                key2BeanMap.put(key, topBucket);
            }
            topBucket.setSiteId(siteId);
            dataList.add(topBucket);
        }
        StringBuilder sb = new StringBuilder();
        for (Entry<String, TopBucket> entry : key2BeanMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey());
        }
        if (sb.length() > 0) {
            String sUrl =
                    "http://ds.suning.cn/ds/prices/" + sb.toString()
                            + "-9017--1-SES.product.priceCenterCallBack.jsonp";
            String retString = getBody(sUrl, maxRetry, Method.GET);
            int index = retString.indexOf("{");
            int toIndex = retString.lastIndexOf("}");
            String text = retString.substring(index, toIndex + 1);
            JSONObject jOb = JSON.parseObject(text);
            JSONArray jArr = jOb.getJSONArray("rs");
            for (int i = 0; i < jArr.size(); i++) {
                JSONObject pObj = jArr.getJSONObject(i);
                String key = pObj.getString("cmmdtyCode");
                TopBucket bean = key2BeanMap.get(key);
                if (bean != null) {
                    Float price = pObj.getFloat("price");
                    bean.setPrice(price);
                }
            }

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
                        .ignoreContentType(true)
                        .method(method)
                        .execute();
        return response;
    }
}
