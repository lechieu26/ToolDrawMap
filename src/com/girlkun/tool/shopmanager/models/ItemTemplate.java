package com.girlkun.tool.shopmanager.models;

public class ItemTemplate {
    public int id;
    public String name;

    public ItemTemplate() {
    }

    public ItemTemplate(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
