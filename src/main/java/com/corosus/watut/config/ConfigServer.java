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

    @ConfigComment("Disable new dynamic gui system and use old simple visual.")
    public static boolean dynamicGuiUseOldSimpleGUIVisual = false;

    @ConfigParams(min = 5, comment = "Delay in ticks before sending a new image of a gui to other clients, 10 = twice a second, 0 = no delay, 20 = once a second, more frequent might affect performance and network load")
    public static int dynamicGuiTickSendRateOfGUIUpdates = 10;

    //TODO: players coming into range or joining server might never receive the screen if the gui was already opened
    @ConfigParams(comment = "If enabled, only sends the initial image of a players gui, does not constantly update after it's been opened.")
    public static boolean dynamicGuiDontSendConstantGUIUpdates = false;

    @ConfigParams(min = 0, max = 2, comment = "Blur is used to prevent nasty aliasing/flicker artifacts at the cost of clarity")
    public static int dynamicGuiBlurLevel = 1;

    @ConfigParams(comment = "Adjust the size of the circle used to cut off extra info beyond their main area of their gui, visual issues may occur of too big. Set to -1 to disable")
    public static double dynamicGuiSizeRadiusInPixelsToShow = 112;

    @ConfigParams(comment = "Show a clients entire screen instead of using dynamicGuiSizeRadiusInPixelsToShow for a circle fade in a 512x512 area, WARNING: experimental, has cpu and network performance impact. Tweak dynamicGuiSizeRadiusInPixelsToShow to -1 or something large if you use this setting.")
    public static boolean dynamicGuiShowClientsEntireScreen = false;

    @ConfigParams(comment = "Disables the background rendering for most guis, might hide things like JEI or other things count as background rendering")
    public static boolean dynamicGuiDisableBackgroundRendering = true;

    @ConfigParams(comment = "Show items being transferred between player and container")
    public static boolean showItemsBeingTransferredBetweenPlayerAndContainer = true;

    @ConfigParams(comment = "How close another player has to be to another player to see what they're doing in GUI, item transferring, mouse movement, etc")
    public static int distanceRequiredToShowGUIInfo = 10;

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
