package com.lezo.mall.blade.require.top.po;

import java.util.List;

public class CategoryElement {
    private String name;
    private String url;
    private String code;
    private int level = 1;
    List<CategoryElement> children;

    @Override
    public String toString() {
        return "CategoryElement [name=" + name + ", url=" + url + ", level=" + level + "]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public List<CategoryElement> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryElement> children) {
        this.children = children;
    }
}