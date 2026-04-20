package com.girlkun.tool.shopmanager.models;

import java.util.List;

public class BossConfig {
    public int bossId;
    public String bossName;
    public byte gender;
    public String outfit;
    public long dame;
    public String hp;
    public String mapJoin;
    public String skills;
    public String textS;
    public String textM;
    public String textE;
    public int secondsRest;
    public byte appearType;
    public String bossesAppearTogether;
    public byte levelIndex;
    public String bossType;
    public boolean isNotifyDisabled;
    public boolean isZone01SpawnDisabled;
    public int spawnCount;
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
    public String rewardConfig;
    public String customClass;
    public boolean enabled;

    @Override
    public String toString() {
        return bossId + " - " + bossName;
    }
}
