package com.lezo.mall.blade.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Parser {

    public Map<String, String> toMap() {
        String text = "barCode";
        Map<String, String> map = new HashMap<String, String>();
        map.put("methodName", "getProductByBarcodeWithPMS/v1.3.8");
        map.put("methodBody", "");
        map.put("barcode", text);
        map.put("guid", "0");
        return map;
    }

    public Map<String, String> toParamMap() {
        Map<String, String> paramMap = new HashMap<String, String>();
        String key = "";
        paramMap.put("signature_method", "md5");
        paramMap.put("timestamp", getServerStamp());
        paramMap.put("trader", "androidSystem");
        paramMap.put("signature", getSign(key, paramMap));

        // key.toLowerCase(Locale.US)

        // paramString = paramString.getBytes();
        // return new String(d.a(b("MD5").digest(paramString)));
        return paramMap;
    }

    private String getSign(String key, Map<String, String> paramMap) {
        StringBuilder sb = new StringBuilder(key);
        Map<String, String> treeMap = new TreeMap<String, String>();
        treeMap.putAll(paramMap);
        Iterator<String> iter = treeMap.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            sb.append(name.toLowerCase(Locale.US)).append(paramMap.get(name));
        }
        String source = sb.toString();
        return null;
    }

    private String getServerStamp() {
        long stamp = System.currentTimeMillis();
        return (stamp / 1000L) + "";
    }
}
