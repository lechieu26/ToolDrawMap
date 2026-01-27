package com.girlkun.tool.shopmanager.models;

public class Shop {
    public int id;
    public int npcId;
    public String tagName;
    public int typeShop;

    // Extra field for display
    public String npcName;

    public Shop() {
    }

    public Shop(int id, int npcId, String tagName, int typeShop, String npcName) {
        this.id = id;
        this.npcId = npcId;
        this.tagName = tagName;
        this.typeShop = typeShop;
        this.npcName = npcName;
    }

    @Override
    public String toString() {
        return tagName; // For ComboBox display
    }
}
