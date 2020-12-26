package com.corosus.watut;

import com.corosus.watut.config.Config;
import com.corosus.watut.network.WATUTNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;


/**
 * mod layout
 *
 * each client has a state for each other player
 * - client player cap? (not sure its designed for this kind of thing)
 *
 * this client opens gui, set info to in gui
 * send packet to server for current state
 *
 * server regularly syncs other clients with the states
 * other client receives states of nearby players
 * other client now sees other player in a gui
 * - for chat gui do a lil typing animation (if theyre actively typing + timeout)
 * -- for non typing chat gui do something a lil diff, idle chat open thing or something
 *
 * design it for potential support of strait up rendered screen data copying
 */


@Mod(WATUT.MODID)
public class WATUT
{
    public static final Logger LOGGER = LogManager.getLogger();

    public static final String MODID = "watut";

    public static HashMap<Class, String> cacheClassToCanonicalName = new HashMap<>();

    public static PlayerManagerServer playerManagerServer = new PlayerManagerServer();

    @OnlyIn(Dist.CLIENT)
    public static PlayerManagerClient playerManagerClient = new PlayerManagerClient();

    public WATUT() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.register(new EventHandlerForge());

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        WATUTNetwork.register();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {

    }

    public static String getCanonicalNameCached(Class clazz) {
        if (!cacheClassToCanonicalName.containsKey(clazz)) {
            cacheClassToCanonicalName.put(clazz, clazz.getCanonicalName());
        }
        return cacheClassToCanonicalName.get(clazz);
    }
}
