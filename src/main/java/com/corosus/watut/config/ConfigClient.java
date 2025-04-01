package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.ConfigParams;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;


public class ConfigClient implements IConfigCategory {

    @ConfigComment("SETTING THESE CLIENT SIDE SETTINGS TO FALSE WILL OVERRIDE ANY OF THE SAME SERVER SETTINGS THAT ARE SET TO TRUE (for your client only).")
    public static boolean dummySetting = true;

    @ConfigComment("Sends relative mouse position and clicking")
    public static boolean sendMouseInfo = true;
    @ConfigComment("Sends a calculated rate of typing. If off, uses a default value on server")
    public static boolean sendTypingSpeed = true;
    @ConfigComment("Sends when you open a Gui, and what Gui")
    public static boolean sendActiveGui = true;
    @ConfigComment("Sends when you go idle / return")
    public static boolean sendIdleState = true;

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

    @ConfigComment("This is for the older non dynamic GUI only. If there isnt an exact Gui available for what the player is using, it will show the Chest Gui. If this is false, it will show nothing, lots of modded Guis will use this for now")
    public static boolean showPlayerActiveGuiIfNotExactMatch = true;

    @ConfigComment("Show 'Player is typing...' on the chat screen")
    public static boolean screenTypingVisible = true;

    @ConfigComment("Adjust the X position where the 'Player is typing...' text shows in the chat Gui")
    public static int screenTypingRelativePosition_X = 0;

    @ConfigComment("Adjust the Y position where the 'Player is typing...' text shows in the chat Gui")
    public static int screenTypingRelativePosition_Y = 0;

    @ConfigComment("Max characters allowed before it switches to using string set in screenTypingMultiplePlayersText")
    public static int screenTypingCharacterLimit = 50;

    @ConfigComment("String to use when too many people are typing determined by screenTypingCharacterLimit")
    public static String screenTypingMultiplePlayersText = "Several people are typing";

    @ConfigComment("String to use next to the typing player(s) name")
    public static String screenTypingText = " is typing";

    @ConfigComment("Plays a sound when a player opens some Guis")
    public static boolean playScreenOpenSounds = true;

    @ConfigComment("Plays a sutble sound when a player clicks their mouse in a Gui")
    public static boolean playMouseClickSounds = true;

    @ConfigParams(min = 0.1, comment = "Adjusts the size of the gui visual that appears infront of a player, 2 = twice the size")
    public static double particleSizeScale = 1;

    @ConfigComment("Delay between ticks your client will accept and update new image of another players gui, you can only increase the delay from what the server/other client is set to, 10 = twice a second")
    public static int tickReceiveAndRenderRateOfGUIUpdates = 10;

    @ConfigComment("Privacy setting, if you dont want to send very detailed info of your screen for everyone to see and instead use the old basic gui visual, set this to true")
    public static boolean dontSendDetailedGUIInfo = false;

    @ConfigComment("Privacy setting, if you dont want to show the items you are transferring to and from yourself and containers, set this to true")
    public static boolean dontSendItemInfo = false;

    @ConfigComment("Set to false if you dont want watut to sho your own dynamic guis in 3rd person")
    public static boolean showGuisForYourOwnPlayerIn3rdPerson = true;

    @Override
    public String getName() {
        return "-client";
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
