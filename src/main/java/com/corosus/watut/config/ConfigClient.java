package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.Watut;


public class ConfigClient implements IConfigCategory {

    @ConfigComment("Sends relative mouse position and clicking")
    public static boolean sendMouseInfo = true;
    @ConfigComment("Sends a calculated rate of typing. If off, uses a default value on server")
    public static boolean sendTypingSpeed = true;
    @ConfigComment("Sends when you open a gui, and what gui")
    public static boolean sendActiveGui = true;
    @ConfigComment("Sends when you go idle / return")
    public static boolean sendIdleState = true;

    public static boolean showIdleStatesInPlayerList = true;
    public static boolean showIdleStatesInPlayerAboveHead = true;

    public static boolean playScreenOpenSounds = true;
    public static boolean playMouseClickSounds = true;

    @ConfigComment("Default 5 minutes")
    public static int ticksToMarkPlayerIdle = 20*60*5;

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
