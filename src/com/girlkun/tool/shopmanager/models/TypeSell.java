package com.girlkun.tool.shopmanager.models;

public class TypeSell {
    public int id;
    public String name;

    public TypeSell(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
