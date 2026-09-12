package com.girlkun.tool.shopmanager.models;

import java.util.ArrayList;
import java.util.List;

public class BossConfig {
    public int bossId;
    public String bossName = "New Boss";
    public byte gender = 0; // 0: Trái Đất, 1: Namếc, 2: Xayda
    public String bossType = "NORMAL"; // NORMAL, TASK, EVENT, DUNGEON, PHOBAN, MINI, FINAL
    public String subType = "DEFAULT";
    public boolean enabled = true;
    public int spawnCount = 1;
    public int respawnDelay = 300; // seconds
    public int despawnTimeout = 900; // seconds
    public boolean isNotify = true;
    public boolean isZone01SpawnDisabled = true;
    public Integer requireTaskId = null;
    public String extraConfig = null;

    public String mapJoin = "";
    public String bossesAppearTogether = "";

    // If this boss is a sub-boss spawned by another boss:
    public Integer parentBossId = null;
    public String parentBossName = null;

    public List<BossFormConfig> forms = new ArrayList<>();
    public List<BossRewardConfig> rewards = new ArrayList<>();

    // --- Legacy compatibility fields ---
    public String outfit = "";
    public long dame = 10000;
    public String hp = "1000000";
    public String skills = "[]";
    public String textS = "[]";
    public String textM = "[]";
    public String textE = "[]";
    public int secondsRest = 300;
    public byte appearType = 0;
    public byte levelIndex = 0;
    public boolean isNotifyDisabled = false;
    public Long maxDamagePerHit;
    public Integer damageDivisor;
    public Long damageFlatReduction;
    public Integer dodgeRate;
    public boolean pierceReverse;
    public Long autoLeaveTimeout;
    public boolean autoLeaveResetOnPlayer;
    public Long autoLeaveRandomMin;
    public Long autoLeaveRandomMax;
    public boolean appendRandomName;
    public boolean doneChatSToAfk;
    public Integer skipNotifyAtLevel;
    public Integer skipMoveAtLevel;
    public String specialAbilities;
    public String rewardConfig = "{}";
    public String customClass = "";

    @Override
    public String toString() {
        return bossId + " - " + bossName + (bossType != null ? " [" + bossType + "]" : "");
    }
}