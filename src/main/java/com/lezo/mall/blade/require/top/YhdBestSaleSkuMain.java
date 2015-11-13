package com.lezo.mall.blade.require.top;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j;

import org.apache.commons.io.FileUtils;

import com.lezo.mall.blade.require.top.po.CategoryElement;
import com.lezo.mall.blade.require.top.worker.YhdBestSaleSkuWorker;

@Log4j
public class YhdBestSaleSkuMain {

    public static void main(String[] args) throws Exception {
        String srcPath = System.getProperty("src", "./data/yhd/top/cate/");
        String destPath = System.getProperty("dest", "./data/yhd/top/sku/");
        long startMills = System.currentTimeMillis();
        File srcFile = new File(srcPath);
        File[] srcFiles = srcFile.listFiles();
        if (srcFiles == null) {
            throw new RuntimeException("no source in path,check src path:" + srcPath);
        }
        int total = srcFiles.length;
        int count = 0;
        int urlCount = 0;
        for (File dataFile : srcFiles) {
            ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
            List<String> urlList = FileUtils.readLines(dataFile, "UTF-8");
            for (String line : urlList) {
                String[] unitArr = line.split("\t");
                int index = -1;
                String cateUrl = unitArr[++index];
                String cateName = unitArr[++index];
                String level = unitArr[++index];
                String crumb = unitArr[++index];
                // if (!crumb.contains("美妆、个人护理、洗护->")) {
                // continue;
                // }
                YhdBestSaleSkuWorker worker = new YhdBestSaleSkuWorker(crumb, cateName,
                        cateUrl, level, destPath);
                urlCount++;
                exec.execute(worker);
                // worker.run();
                // return;
            }
            waitForDone(exec);
            count++;
            log.info("file total:" + total + ",done:" + count + ".urlCount:" + urlCount);
        }
        long costMills = System.currentTimeMillis() - startMills;
        log.info("done......cost:" + costMills);
    }

    private static void waitForDone(ThreadPoolExecutor exec) throws Exception {
        exec.shutdown();
        while (!exec.isTerminated()) {
            log.info("active:" + exec.getActiveCount() + ",done:"
                    + exec.getCompletedTaskCount() + ",queue:" + exec.getQueue().size());
            TimeUnit.SECONDS.sleep(1);
        }

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
}
