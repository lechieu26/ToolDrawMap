package com.girlkun.tool.shopmanager.models;

import java.util.ArrayList;
import java.util.List;

public class BossFormConfig {
    public int id;
    public int bossId;
    public int formOrder;
    public String name = "";
    public long hpMin = 1000000;
    public long hpMax = 1000000;
    public int dame = 10000;
    public short outfitHead = -1;
    public short outfitBody = -1;
    public short outfitLeg = -1;
    public short outfitBag = -1;
    public short outfitAura = -1;
    public short outfitEff = -1;
    public String textStart = "[]";
    public String textMid = "[]";
    public String textEnd = "[]";

    public List<BossSkillConfig> skills = new ArrayList<>();

    public BossFormConfig() {
    }

    public BossFormConfig(int formOrder, String name) {
        this.formOrder = formOrder;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Dạng " + formOrder + (name != null && !name.trim().isEmpty() ? ": " + name : "");
    }
}