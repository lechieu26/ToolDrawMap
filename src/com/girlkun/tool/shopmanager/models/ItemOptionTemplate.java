package com.girlkun.tool.shopmanager.models;

public class ItemOptionTemplate {
    public int id;
    public String name;

    public ItemOptionTemplate() {
    }

    public ItemOptionTemplate(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
