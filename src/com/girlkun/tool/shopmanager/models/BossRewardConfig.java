package com.girlkun.tool.shopmanager.models;

public class BossRewardConfig {
    public int id;
    public int bossId;
    public int itemId;
    public int quantityMin = 1;
    public int quantityMax = 1;
    public double rate = 100.0;
    public String itemOptions = "[]";
    public int eventPoint = 0;
    public int activePoint = 0;

    public BossRewardConfig() {
    }

    public BossRewardConfig(int id, int bossId, int itemId, int quantityMin, int quantityMax, double rate, String itemOptions, int eventPoint, int activePoint) {
        this.id = id;
        this.bossId = bossId;
        this.itemId = itemId;
        this.quantityMin = quantityMin;
        this.quantityMax = quantityMax;
        this.rate = rate;
        this.itemOptions = itemOptions != null ? itemOptions : "[]";
        this.eventPoint = eventPoint;
        this.activePoint = activePoint;
    }
}