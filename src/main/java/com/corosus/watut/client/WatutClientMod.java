package com.corosus.watut.client;

import com.corosus.coroutil.config.ConfigCoroUtil;
import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.network.PacketNBTFromServer;
import com.corosus.watut.network.WatutNetworking;
import com.corosus.watut.client.status.PlayerStatusClientManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Entrypoint Client di Fabric per WATUT.
 */
@Environment(EnvType.CLIENT)
public class WatutClientMod implements ClientModInitializer {

    private static PlayerStatusClientManager clientManager;

    public static PlayerStatusClientManager getPlayerStatusManagerClient() {
        if (clientManager == null) {
            clientManager = new PlayerStatusClientManager();
        }
        return clientManager;
    }

    @Override
    public void onInitializeClient() {
        com.corosus.modconfig.CoroConfigRegistry.instance().addConfigFile(WatutMod.MODID, new com.corosus.watut.config.ConfigClient());

        // Registrazione del renderer olografico 3D per schermi dinamici
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(
                com.corosus.watut.client.screen.DynamicScreenRenderer::render
        );

        ClientPlayNetworking.registerGlobalReceiver(PacketNBTFromServer.TYPE, (payload, ctx) -> {
            CompoundTag nbt = payload.nbt();
            ctx.client().execute(() -> {
                try {
                    if (nbt.contains(WatutNetworking.NBTDataPlayerUUID)) {
                        String uuidStr = nbt.getStringOr(WatutNetworking.NBTDataPlayerUUID, "");
                        if (!uuidStr.isEmpty()) {
                            UUID uuid = UUID.fromString(uuidStr);
                            getPlayerStatusManagerClient().receiveAny(uuid, nbt);
                        }
                    } else if (nbt.contains(WatutNetworking.NBTDataServerConfig)) {
                        getPlayerStatusManagerClient().receiveServerConfig(nbt);
                    } else if (nbt.contains(WatutNetworking.NBTDataItemTransferItemStack)) {
                        getPlayerStatusManagerClient().receiveItemMove(nbt);
                    }
                } catch (Exception ex) {
                    CULog.dbg("WATUT ERROR: error handling server packet: " + ex.getMessage());
                    if (ConfigCoroUtil.useLoggingDebug) {
                        ex.printStackTrace();
                    }
                }
            });
        });
    }
}
