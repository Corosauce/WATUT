package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;


public class ConfigCommon implements IConfigCategory {

    public static boolean announceIdleStatesInChat = false;

    @ConfigComment("Default 5 minutes")
    public static int ticksToMarkPlayerIdle = 20*60*5;

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
