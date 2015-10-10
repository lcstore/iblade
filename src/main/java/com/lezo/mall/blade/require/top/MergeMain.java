package com.lezo.mall.blade.require.top;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import lombok.extern.log4j.Log4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@Log4j
public class MergeMain {

    public static void main(String[] args) throws Exception {
        String srcPath = System.getProperty("src", "src/main/resources/shell");
        String destFile = System.getProperty("dest", "src/main/resources/dest.txt");
        final String suffix = System.getProperty("suffix", ".sh");
        File srcFile = new File(srcPath);
        File[] srcArr = srcFile.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(suffix);
            }
        });
        if (srcArr == null) {
            return;
        }
        int count = 0;
        Writer out = new FileWriter(destFile);
        BufferedWriter bw = new BufferedWriter(out);
        for (File file : srcArr) {
            count++;
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (br.ready()) {
                String line = br.readLine();
                if (StringUtils.isNotBlank(line)) {
                    bw.append(line);
                    bw.append("\n");
                }
            }
            IOUtils.closeQuietly(br);
        }
        bw.flush();
        IOUtils.closeQuietly(bw);
        log.info("done.merge srcPath:" + srcFile + ",dest:" + destFile + ",count:" + count);
    }

}
