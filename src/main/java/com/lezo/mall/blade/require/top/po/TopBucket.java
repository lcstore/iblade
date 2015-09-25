package com.lezo.mall.blade.require.top.po;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopBucket {
    // 商家ID
    private Integer siteId;
    // 面包屑名称（比如：电脑、办公->电脑整机->笔记本）
    private String crumb;
    // 抓取链接（比如：http://list.jd.com/list.html?cat=670%2C671%2C672&delivery=1&page=1）
    private String fromUrl;
    // 商品链接（比如：http://item.jd.com/1466274.html）
    private String productUrl;
    // 页码
    private Integer pageNum;
    // 排名（期望是全局排名，比如第二页的第一个，那应该是排名61，在一页60的情况下）
    private Integer rankNum;
    // 评论数
    private Integer commonNum;
    // 价格（折扣价）
    private Float price;
    // 标题
    private String title;
    // 抓取时间
    private Date fetchTime;
}
