package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.ConfigParams;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;

/**
 * These configs are controlled by server, some could be only used on client, some could be sycned to client, some both sides if required
 *   - new system syncs it to this same class
 */
public class ConfigServerControlledSyncedToClient implements IConfigCategory {

    /**
     * ALL SERVER SETTINGS ARE SYNCED
     */

    @ConfigComment("SETTING THESE SERVER SIDE SETTINGS TO FALSE WILL OVERRIDE ANY OF THE SAME CLIENT SETTINGS THAT ARE SET TO TRUE (for all clients)")
    public static boolean dummySetting = true;


    /**
     * DYNAMIC GUI
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

    /**
     * MISC
     */

    @ConfigParams(comment = "Show items being transferred between player and container")
    public static boolean showItemsBeingTransferredBetweenPlayerAndContainer = true;

    @ConfigParams(comment = "How close another player has to be to another player to see what they're doing in GUI, item transferring, mouse movement, etc")
    public static int distanceRequiredToShowGUIInfo = 10;

    /**
     * From ConfigClient to give a server override version
     */

    @ConfigComment("Enables or disables idle visual in the server player list tab screen")
    public static boolean showIdleStatesInPlayerList = true;

    @ConfigComment("Enables or disables idle visual above player head")
    public static boolean showIdleStatesInPlayerAboveHead = true;

    @ConfigComment("Setting false disables all animations")
    public static boolean showPlayerAnimations = true;

    @ConfigComment("Enables or disables typing animation")
    public static boolean showPlayerAnimation_Typing = true;

    @ConfigComment("Enables or disables idle animation")
    public static boolean showPlayerAnimation_Idle = true;

    @ConfigComment("Enables or disables the non typing animations used when showing the open Gui, such as head looking, arms moving up, arms pointing and clicking")
    public static boolean showPlayerAnimation_Gui = true;

    @ConfigComment("Show any Gui they're using that isn't chat typing related in world")
    public static boolean showPlayerActiveNonChatGui = true;

    @ConfigComment("Show the chat typing Gui in world")
    public static boolean showPlayerActiveChatGui = true;

    @ConfigComment("Show 'Player is typing...' on the chat screen")
    public static boolean screenTypingVisible = true;

    @ConfigComment("Plays a sound when a player opens some Guis")
    public static boolean playScreenOpenSounds = true;

    @ConfigComment("Plays a sutble sound when a player clicks their mouse in a Gui")
    public static boolean playMouseClickSounds = true;

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
