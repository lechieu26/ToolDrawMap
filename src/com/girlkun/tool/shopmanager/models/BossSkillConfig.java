package com.girlkun.tool.shopmanager.models;

public class BossSkillConfig {
    public int id;
    public int formId;
    public int skillId;
    public int skillLevel = 1;
    public int cooldown = 1000;

    public BossSkillConfig() {
    }

    public BossSkillConfig(int skillId, int skillLevel, int cooldown) {
        this.skillId = skillId;
        this.skillLevel = skillLevel;
        this.cooldown = cooldown;
    }

    public BossSkillConfig(int id, int formId, int skillId, int skillLevel, int cooldown) {
        this.id = id;
        this.formId = formId;
        this.skillId = skillId;
        this.skillLevel = skillLevel;
        this.cooldown = cooldown;
    }
}