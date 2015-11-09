package com.lezo.mall.blade.monitor;

import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
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
public class TmallListMonitor {

    public static void main(String[] args) throws Exception {
        int index = 0;
        long timeout = 5000;
        String pattern = "MMdd_HH_mm_ss";
        disableSSLCertCheck();
        String dateChars = DateFormatUtils.format(new Date(), pattern);
        int lastErrorCount = 0;
        while (true) {
            index++;
            String url =
                    "https://buydeem.tmall.com/search.htm?orderType=defaultSort&viewType=grid&keyword=&lowPrice=&highPrice=&scene=taobao_shop";
            url =
                    "https://buydeem.tmall.com/i/asynSearch.htm?_ksTS="
                            + System.currentTimeMillis()
                            + "_380&callback=jsonp381&mid=w-3442796266-0&wid=3442796266&path=/search.htm&&search=y&orderType=defaultSort&viewType=grid&keyword=null&lowPrice=null&highPrice=null&scene=taobao_shop";
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
                    .writeStringToFile(new File("./data/tmall/monitor/", dateChars + File.separator + index
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
        String sCode = "41159567374";
        return body.indexOf("J_TItems") > 0 && body.indexOf(sCode) > 0;
    }

    private static Response doRequest(String url, int maxRetry) {
        long timeout = 1000;
        String userAgent =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36";
        Map<String, String> cookies = getCookieMap();
        for (int i = 1; i <= maxRetry; i++) {
            try {
                return Jsoup.connect(url).cookies(cookies).method(Method.GET).userAgent(userAgent).execute();
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
