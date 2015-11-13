package com.lezo.mall.blade.monitor;

import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;

import lombok.extern.log4j.Log4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import com.alibaba.fastjson.JSON;

@Log4j
public class YixunSkuMonitor {

    public static void main(String[] args) throws Exception {
        int index = 0;
        long timeout = 5000;
        String pattern = "MMdd_HH_mm_ss";
        String dataDir = "./data/" + YixunSkuMonitor.class.getSimpleName().toLowerCase();
        disableSSLCertCheck();
        String dateChars = DateFormatUtils.format(new Date(), pattern);
        int lastErrorCount = 0;
        while (true) {
            index++;
            String url = "http://item.yixun.com/item-2142686.html?YTAG=2.2676901720023&t=" + System.currentTimeMillis();
            long start = System.currentTimeMillis();
            Response resp = doRequest(url, 3);
            long cost = System.currentTimeMillis() - start;
            if (resp == null) {
                log.warn("error,index:" + index + ",status:null,cost:" + cost
                        + ",url:" + url);
            } else {
                log.info("done,index:" + index + ",status:" + resp.statusCode() + ",cost:" + cost
                        + ",url:" + url);
            }
            boolean success = doValidateResponse(resp);
            if (success) {
                lastErrorCount = 0;
                log.info("validate=true.index:" + index + ",cookies:" + JSON.toJSONString(resp.cookies()));
                log.info("validate=true.index:" + index + ",headers:" + JSON.toJSONString(resp.headers()));
            } else {
                lastErrorCount++;
                if (resp != null) {
                    log.warn("validate=false.index:" + index + ",cookies:" + JSON.toJSONString(resp.cookies()));
                    log.warn("validate=false.index:" + index + ",cookies:" + JSON.toJSONString(resp.headers()));
                }
            }
            FileUtils
                    .writeStringToFile(new File(dataDir, dateChars + File.separator + index
                            + ".html"), resp == null ? "no response" : resp.body());
            if (lastErrorCount >= 10) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(timeout);
        }

    }

    private static boolean doValidateResponse(Response resp) {
        if (resp == null) {
            return false;
        }
        String body = resp.body();
        List<String> containList = new ArrayList<String>();
        containList.add("http://item.yixun.com/item-2142686.html");
        containList.add("苹果（Apple）iPhone 6 (A1586) 16GB 金色 移动联通电信4G手机");
        for (String sVal : containList) {
            if (body.indexOf(sVal) < 0) {
                return false;
            }
        }
        return true;
    }

    private static Response doRequest(String url, int maxRetry) {
        long timeout = 1000;
        String userAgent =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36";
        String sCookie =
                "wsid=1; prid=29313_2621; loc=2_1_310000_310100_310101_29313; Hm_lvt_23bc0079ed4b2d06e5d5332ef5de1ec7=1447231963; Hm_lpvt_23bc0079ed4b2d06e5d5332ef5de1ec7=1447234761; __jda=1982585.879353374.1447231963.1447232157.1447234761.2; __jdv=1982585%7Cdirect%7C-%7Cnone%7C-; __jdc=1982585; visitkey=42984674095727066; y_guid=141599CD8DB6C800; y_rfid=26769017%7C21426861; __jdu=3d4038a6-50ca-44c8-914b-77668e61576b; y_track=0-100010004-2676901720023-0.2; __jdb=1982585.1.879353374%7C2.1447234761";
        String referrer =
                "http://searchex.yixun.com/html?area=1&charset=gbk&as=1&key=%C6%BB%B9%FB%A3%A8Apple%A3%A9iPhone+6+%28A1586%29+16GB+%BD%F0%C9%AB+%D2%C6%B6%AF%C1%AA%CD%A8%B5%E7%D0%C54G%CA%D6%BB%FA&YTAG=2.1159608000401";
        Map<String, String> cookies = getCookieMap();
        for (int i = 1; i <= maxRetry; i++) {
            try {
                return Jsoup.connect(url).referrer(referrer).header("Cookie", sCookie).cookies(cookies)
                        .method(Method.GET)
                        .userAgent(userAgent).execute();
            } catch (Exception e) {
                try {
                    TimeUnit.MILLISECONDS.sleep(timeout);
                } catch (InterruptedException e1) {
                }
                log.info("do Retry:" + i + ",url:" + url + ",cause:", e);
            }
        }
        return null;
    }

    private static void disableSSLCertCheck() throws NoSuchAlgorithmException,
            KeyManagementException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public
                    void
                    checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                // TODO Auto-generated method stub

            }

            @Override
            public
                    void
                    checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                // TODO Auto-generated method stub

            }
        }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    private static Map<String, String> getCookieMap() {
        Map<String, String> cookieMap = new HashMap<String, String>();
        return cookieMap;
    }
}
