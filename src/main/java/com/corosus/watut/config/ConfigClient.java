package com.corosus.watut.config;

import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.Watut;


public class ConfigClient implements IConfigCategory {

    public static boolean sendMouseInfo = true;
    public static boolean sendTypingSpeed = true;
    public static boolean sendActiveGui = true;
    public static boolean sendIdleState = true;

    @Override
    public String getName() {
        return "-client";
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
