package com.girlkun.tool.shopmanager.models;

public class NpcTemplate {
    public int id;
    public String name;

    public NpcTemplate() {
    }

    public NpcTemplate(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return id + " - " + name; // For ComboBox
    }
}
