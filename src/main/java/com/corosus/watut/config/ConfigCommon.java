package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;


public class ConfigCommon implements IConfigCategory {

    public static boolean announceIdleStatesInChat = false;

    @ConfigComment("Default 5 minutes")
    public static int ticksToMarkPlayerIdle = 20*60*5;

    public static boolean dc_noHordesIfInCreative = true;

    public static boolean dc_noHordesIfWatutIdle = true;
    public static boolean dc_noHordesIfGameTimePaused = false;
    public static boolean dc_useResetBeforeDay104 = true;
    public static boolean dc_dbg = false;

    public static int dc_resetDay = 70;

    @Override
    public String getName() {
        return "-common";
    }

    @Override
    public String getRegistryName() {
        return WatutMod.MODID + getName();
    }

    @Override
    public String getConfigFileName() {
        return WatutMod.MODID + getName();
    }

    @Override
    public String getCategory() {
        return WatutMod.MODID + ": " + getName();
    }

    @Override
    public void hookUpdatedValues() {

    }
}
