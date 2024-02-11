package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;


public class ConfigClient implements IConfigCategory {

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

    @ConfigComment("If there isnt an exact Gui available for what the player is using, it will show the Chest Gui. If this is false, it will show nothing, lots of modded Guis will use this for now")
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

    @ConfigComment("Plays a sound when a player opens some Guis")
    public static boolean playScreenOpenSounds = true;

    @ConfigComment("Plays a sutble sound when a player clicks their mouse in a Gui")
    public static boolean playMouseClickSounds = true;

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
