package com.corosus.watut.config;

import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.Watut;


public class ConfigCommon implements IConfigCategory {

    public static boolean announceIdleStatesInChat = true;
    public static int ticksToMarkPlayerIdle = 20*60*5;

    @Override
    public String getName() {
        return "-common";
    }

    @Override
    public String getRegistryName() {
        return Watut.MODID + getName();
    }

    @Override
    public String getConfigFileName() {
        return Watut.MODID + getName();
    }

    @Override
    public String getCategory() {
        return Watut.MODID + ": " + getName();
    }

    @Override
    public void hookUpdatedValues() {

    }
}
