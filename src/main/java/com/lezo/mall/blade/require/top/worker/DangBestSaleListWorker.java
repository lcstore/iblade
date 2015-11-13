package com.lezo.mall.blade.require.top.worker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.lezo.mall.blade.require.top.po.CategoryElement;

public class DangBestSaleListWorker implements Runnable {
    private Logger log = Logger.getLogger(DangBestSaleListWorker.class);
    public static String CODE_SEPERATOR = "->";
    private Element element;
    private CategoryElement ce;
    private File destFile;

    public DangBestSaleListWorker(File destFile, CategoryElement ce, Element element) {
        super();
        this.destFile = destFile;
        this.ce = ce;
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
        Elements ct2Els = element.select("div.cfied-list");
        if (ct2Els.isEmpty()) {
            log.warn("no ct2 elment:" + element);
            return;
        }
        List<CategoryElement> children = new ArrayList<CategoryElement>();
        parent.setChildren(children);
        for (Element ct2Ele : ct2Els) {
            CategoryElement ct2Element = new CategoryElement();
            Elements dataEls = ct2Ele.select("h4 a[href][target]");
            ct2Element.setName(dataEls.first().ownText().trim());
            ct2Element.setUrl(dataEls.first().absUrl("href"));
            ct2Element.setLevel(parent.getLevel() + 1);
            if (parent.getCode() == null) {
                ct2Element.setCode(ct2Element.getName());
            } else {
                ct2Element.setCode(parent.getCode() + CODE_SEPERATOR + ct2Element.getName());
            }
            children.add(ct2Element);
            Elements ct3Els = ct2Ele.select("div.list a[target][href]");
            if (ct3Els.isEmpty()) {
                continue;
            }
            ct2Element.setChildren(new ArrayList<CategoryElement>());
            for (Element ct3Ele : ct3Els) {
                CategoryElement ce3Element = new CategoryElement();
                ce3Element.setName(ct3Ele.ownText().trim());
                ce3Element.setUrl(ct3Ele.absUrl("href"));
                ce3Element.setLevel(ct2Element.getLevel() + 1);
                if (parent.getCode() == null) {
                    ce3Element.setCode(ce3Element.getName());
                } else {
                    ce3Element.setCode(ct2Element.getCode() + CODE_SEPERATOR + ce3Element.getName());
                }
                ct2Element.getChildren().add(ce3Element);
            }
        }
    }

    @Override
    public void run() {
        long startMills = System.currentTimeMillis();
        CategoryElement cce = ce;
        addChildren(ce);
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
