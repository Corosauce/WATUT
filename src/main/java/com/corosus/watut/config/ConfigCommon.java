package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.ConfigParams;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;


public class ConfigCommon implements IConfigCategory {

    public static boolean announceIdleStatesInChat = false;

    @ConfigComment("Default 5 minutes")
    public static int ticksToMarkPlayerIdle = 20*60*5;

    //TODO: a server side config that syncs to clients i guess
    @ConfigParams(min = 5, comment = "Delay between ticks your client sends out a new image of your gui to other clients, 10 = twice a second")
    public static int tickSendRateOfGUIUpdates = 10;

    @ConfigComment("Delay between ticks your client will accept and update new image of another players gui, you can only increase the delay from what the server/other client is set to, 10 = twice a second")
    public static int tickReceiveAndRenderRateOfGUIUpdates = 10;

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
