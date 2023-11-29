package com.corosus.watut.config;

import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.Watut;


public class ConfigCommon implements IConfigCategory {

    public static boolean announceIdleStates = true;

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
