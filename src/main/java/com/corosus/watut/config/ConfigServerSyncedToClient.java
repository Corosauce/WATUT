package com.corosus.watut.config;

/**
 * Synced from client when player joins, initially set to the compiled default of server settings
 */
public class ConfigServerSyncedToClient {

    public static boolean useOldSimpleGUIVisual = ConfigServer.dynamicGuiUseOldSimpleGUIVisual;

    public static int blurLevel = ConfigServer.dynamicGuiBlurLevel;

    public static double sizeRadiusInPixelsToShow = ConfigServer.dynamicGuiSizeRadiusInPixelsToShow;

    public static int tickSendRateOfGUIUpdates = ConfigServer.dynamicGuiTickSendRateOfGUIUpdates;

    public static boolean dynamicGuiShowClientsEntireScreen = ConfigServer.dynamicGuiShowClientsEntireScreen;

    public static boolean dynamicGuiDisableBackgroundRendering = ConfigServer.dynamicGuiDisableBackgroundRendering;

}
