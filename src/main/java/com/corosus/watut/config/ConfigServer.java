package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.ConfigParams;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;

/**
 * These configs are controlled by server, some could be only used on client, some could be sycned to client, some both sides if required
 */
public class ConfigServer implements IConfigCategory {


    /**
     * SERVER SETTINGS SYNCED
     */

    @ConfigComment("Synced to clients, Disable new dynamic gui system and use old simple visual.")
    public static boolean dynamicGuiUseOldSimpleGUIVisual = false;

    @ConfigParams(min = 5, comment = "Synced to clients, Delay in ticks before sending a new image of a gui to other clients, 10 = twice a second, 0 = no delay, 20 = once a second")
    public static int dynamicGuiTickSendRateOfGUIUpdates = 10;

    @ConfigParams(min = 0, max = 2, comment = "Synced to clients, blur is used to prevent nasty aliasing/flicker artifacts at the cost of clarity")
    public static int dynamicGuiBlurLevel = 1;

    //TODO: consider a 'show full gui' option that shows clients entire screen, JEI and all, to help this setting
    @ConfigParams(comment = "Synced to clients, adjust the size of the circle used to cut off extra info beyond their main area of their gui, visual issues may occur of too big")
    public static double dynamicGuiSizeRadiusInPixelsToShow = 128;

    @ConfigParams(comment = "Synced to clients, show a clients entire screen instead of using dynamicGuiSizeRadiusInPixelsToShow for a circle fade in a 512x512 area, WARNING: experimental, has cpu and network performance impact")
    public static boolean dynamicGuiShowClientsEntireScreen = false;

    @ConfigParams(comment = "Synced to clients, disables the background rendering for most guis, might hide things like JEI or other things count as background rendering")
    public static boolean dynamicGuiDisableBackgroundRendering = true;

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
        if (WatutMod.instance().getPlayerList() != null) {
            WatutMod.getPlayerStatusManagerServer().syncServerConfigToAllPlayers();
        }
    }
}
