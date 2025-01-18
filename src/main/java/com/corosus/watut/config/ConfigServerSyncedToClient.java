package com.corosus.watut.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.watut.WatutMod;

/**
 * Synced from client when player joins, initially set to the compiled default of server settings
 */
public class ConfigServerSyncedToClient {

    public static boolean useOldSimpleGUIVisual = ConfigServer.useOldSimpleGUIVisual;

    public static int blurLevel = ConfigServer.blurLevel;

    public static double sizeRadiusInPixelsToShow = ConfigServer.sizeRadiusInPixelsToShow;

    public static int tickSendRateOfGUIUpdates = ConfigServer.tickSendRateOfGUIUpdates;

}
