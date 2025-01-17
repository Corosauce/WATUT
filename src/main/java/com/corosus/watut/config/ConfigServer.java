package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.ConfigParams;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * These configs are controlled by server, some could be only used on client, some could be sycned to client, some both sides if required
 */
public class ConfigServer implements IConfigCategory {


    /**
     * SERVER SETTINGS SYNCED
     */

    //TODO: flesh these names and info out

    @ConfigComment("Disable new dynamic gui system and use old simple visual.")
    public static boolean useOldSimpleGUIVisual = false;

    @ConfigParams(min = 5, comment = "Delay between ticks when a client sends out a new image of a gui to other clients, 10 = twice a second")
    public static int tickSendRateOfGUIUpdates = 10;

    @ConfigParams(min = 0, max = 2, comment = "Synced to clients, blur is used to prevent nasty aliasing/flicker artifacts")
    public static int blurLevel = 1;

    //TODO: consider a 'show full gui' option that shows clients entire screen, JEI and all, to help this setting
    @ConfigParams(comment = "Synced to clients, adjust the size of the circle used to cut off extra info beyond their main area of their gui, visual issues may occur of too big")
    public static double sizeRadiusInPixelsToShow = 128;

    @Override
    public String getName() {
        return "-server";
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
        if (ServerLifecycleHooks.getCurrentServer() != null && ServerLifecycleHooks.getCurrentServer().getPlayerList() != null) {
            WatutMod.getPlayerStatusManagerServer().syncServerConfigToAllPlayers();
        }
    }
}
