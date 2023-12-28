package com.corosus.watut;

import com.corosus.coroutil.common.core.modconfig.ConfigMod;
import com.corosus.coroutil.common.core.modconfig.CoroConfigRegistry;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigCommon;
import net.minecraft.server.players.PlayerList;

public abstract class WatutMod
{

    // Define mod id in a common place for everything to reference
    public static final String MODID = "watut";

    private static PlayerStatusManagerClient playerStatusManagerClient = null;
    private static PlayerStatusManagerServer playerStatusManagerServer = null;

    private static WatutMod instance;

    public static WatutMod instance() {
        return instance;
    }

    public static PlayerStatusManagerClient getPlayerStatusManagerClient() {
        if (playerStatusManagerClient == null) playerStatusManagerClient = new PlayerStatusManagerClient();
        return playerStatusManagerClient;
    }

    public static PlayerStatusManagerServer getPlayerStatusManagerServer() {
        if (playerStatusManagerServer == null) playerStatusManagerServer = new PlayerStatusManagerServer();
        return playerStatusManagerServer;
    }

    public WatutMod() {
        instance = this;
        /*while (ConfigMod.instance() == null) {
            System.out.println("WAITING FOR COROUTIL TO LOAD MAYBE DO THIS DIFFERENTLY AAAAAAAAAAAAAA");
            try {
                Thread.sleep(100L);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }*/
        CoroConfigRegistry.instance().addConfigFile(MODID, new ConfigCommon());
        CoroConfigRegistry.instance().addConfigFile(MODID, new ConfigClient());
    }

    public abstract PlayerList getPlayerList();

    public static void dbg(Object obj) {
        //System.out.println("" + obj);
    }
}
