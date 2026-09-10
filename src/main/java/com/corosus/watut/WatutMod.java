package com.corosus.watut;

import com.corosus.modconfig.CoroConfigRegistry;
import com.corosus.watut.command.CommandWatutReloadJSON;
import com.corosus.watut.config.ConfigCommon;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.config.CustomArmCorrections;
import com.corosus.watut.network.PacketNBTFromClient;
import com.corosus.watut.network.PacketNBTFromServer;
import com.corosus.watut.server.PlayerStatusServerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Entrypoint principale comune di WATUT per Fabric.
 */
public class WatutMod implements ModInitializer {

    public static final String MODID = "watut";
    public static final String configJSONName = "watut-item-arm-adjustments.json";

    public static MinecraftServer minecraftServer = null;
    private static WatutMod instance;
    private static PlayerStatusServerManager serverManager;

    public static WatutMod instance() {
        return instance;
    }

    public static PlayerStatusServerManager getPlayerStatusManagerServer() {
        if (serverManager == null) {
            serverManager = new PlayerStatusServerManager();
        }
        return serverManager;
    }

    public static net.minecraft.server.players.PlayerList getPlayerList() {
        return minecraftServer != null ? minecraftServer.getPlayerList() : null;
    }

    public WatutMod() {
        instance = this;
        CoroConfigRegistry.instance().addConfigFile(MODID, new ConfigCommon());
        CoroConfigRegistry.instance().addConfigFile(MODID, new ConfigServerControlledSyncedToClient());

        generateJsonConfigFile(configJSONName);
        CustomArmCorrections.loadJsonConfigs();
    }

    @Override
    public void onInitialize() {
        // Cattura e pulizia dell'istanza del server
        ServerLifecycleEvents.SERVER_STARTED.register(server -> minecraftServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            minecraftServer = null;
            serverManager = null;
        });

        // Registrazione comandi (/watut reloadJSON)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommandWatutReloadJSON.register(dispatcher);
        });

        // Registrazione dei tipi di payload
        PayloadTypeRegistry.clientboundPlay().register(PacketNBTFromServer.TYPE, PacketNBTFromServer.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PacketNBTFromClient.TYPE, PacketNBTFromClient.STREAM_CODEC);

        // Ricezione pacchetti C2S
        ServerPlayNetworking.registerGlobalReceiver(PacketNBTFromClient.TYPE, (payload, ctx) -> {
            CompoundTag nbt = payload.nbt();
            ctx.server().execute(() -> {
                if (ctx.player() != null) {
                    getPlayerStatusManagerServer().receiveAny(ctx.player(), nbt);
                }
            });
        });
    }

    public static void generateJsonConfigFile(String filename) {
        String filePath = "config/" + filename;
        String fileContents = getContentsFromResourceLocation(Identifier.fromNamespaceAndPath(MODID, filePath));
        if (!fileContents.isEmpty()) {
            File configDir = FabricLoader.getInstance().getConfigDir().toFile();
            File fileOut = new File(configDir, filename);
            if (!fileOut.exists()) {
                try {
                    FileUtils.writeStringToFile(fileOut, fileContents, StandardCharsets.UTF_8);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public static String getContentsFromResourceLocation(Identifier resourceLocation) {
        try {
            String str = "assets/" + resourceLocation.toString().replace(":", "/");
            InputStream in = WatutMod.class.getClassLoader().getResourceAsStream(str);
            if (in != null) {
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static boolean isModInstalled(String modID) {
        return FabricLoader.getInstance().isModLoaded(modID);
    }
}
