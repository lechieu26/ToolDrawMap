package com.girlkun.tool.shopmanager.models;

public class TabShop {
    public int id;
    public int shopId;
    public String tabName;

    public TabShop() {
    }

    public TabShop(int id, int shopId, String tabName) {
        this.id = id;
        this.shopId = shopId;
        this.tabName = tabName;
    }

    @Override
    public String toString() {
        return tabName; // For ComboBox
    }
}
