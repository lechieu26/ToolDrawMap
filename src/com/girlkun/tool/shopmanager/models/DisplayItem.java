package com.girlkun.tool.shopmanager.models;

import java.util.ArrayList;
import java.util.List;

public class DisplayItem {
    public int id;
    public String name;
    public int sellType;
    public String sellTypeName;
    public int cost;
    public List<ItemOptionDisplay> options = new ArrayList<>();

    public static class ItemOptionDisplay {
        public int id;
        public String name;
        public int param;

        public ItemOptionDisplay(int id, String name, int param) {
            this.id = id;
            this.name = name;
            this.param = param;
        }
    }
}
