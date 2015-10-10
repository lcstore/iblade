package com.lezo.mall.blade.require.top;

public class TestMain {

    public static void main(String[] args) {
        String cmd =
                "java -Xms256m -Xmx1024m -Dsuffix=\".txt\"  -Ddest=data/20151001/amazon.sale.top.20151001.data -Dsrc=data/20151001/amazon/top/sku/ -cp ./blade.jar com.lezo.mall.blade.require.top.MergeMain";
        for (int i = 1; i <= 10; i++) {
            String newCmd = null;
            if (i < 10) {
                newCmd = cmd.replace("20151001", "2015100" + i);
            } else {
                newCmd = cmd.replace("20151001", "201510" + i);
            }
            System.err.println(newCmd);
        }

    }
}
