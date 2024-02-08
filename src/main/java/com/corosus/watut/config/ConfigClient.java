package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;


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

    @ConfigComment("Enables or disables all animations relating to typing")
    public static boolean showPlayerAnimations = true;

    @ConfigComment("Enables or disables typing animation")
    public static boolean showPlayerAnimation_Typing = true;

    @ConfigComment("Enables or disables idle animation")
    public static boolean showPlayerAnimation_Idle = true;

    @ConfigComment("Enables or disables the animation used when showing the open Gui, not including chat")
    public static boolean showPlayerAnimation_Gui = true;

    @ConfigComment("Enables or disables the pointing and clicking animation used when showing the open Gui, not including chat")
    public static boolean showPlayerAnimation_Gui_PointingClicking = true;

    public static boolean playScreenOpenSounds = true;
    public static boolean playMouseClickSounds = true;

    @ConfigComment("Show any Gui they're using that isn't chat typing related in world")
    public static boolean showPlayerActiveNonChatGui = true;

    @ConfigComment("Show the chat typing Gui in world")
    public static boolean showPlayerActiveChatGui = true;

    @ConfigComment("If there isnt an exact Gui available for what the player is using, it will show the Chest Gui. If this is false, it will show nothing")
    public static boolean showPlayerActiveGuiIfNotExactMatch = true;

    @ConfigComment("Adjust the X position where the 'Player is typing...' text shows in the chat gui")
    public static int renderTypingStatusRelativePosition_X = 0;

    @ConfigComment("Adjust the Y position where the 'Player is typing...' text shows in the chat gui")
    public static int renderTypingStatusRelativePosition_Y = 0;

    @ConfigComment("Max characters allowed before it switches to using string set in renderTypingMultipleTyping")
    public static int renderTypingCharacterLimit = 50;

    @ConfigComment("String to use when too many people are typing determined by renderTypingCharacterLimit")
    public static String renderTypingMultipleTyping = "Several people are typing";

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
