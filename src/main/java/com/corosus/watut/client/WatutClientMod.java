package com.corosus.watut.client;

import com.corosus.coroutil.config.ConfigCoroUtil;
import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.network.PacketNBTFromServer;
import com.corosus.watut.network.WatutNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Entrypoint Client di Fabric per WATUT.
 */
public class WatutClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Registrazione del renderer olografico 3D per schermi dinamici
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(
                com.corosus.watut.client.screen.DynamicScreenRenderer::render
        );

        ClientPlayNetworking.registerGlobalReceiver(PacketNBTFromServer.TYPE, (payload, ctx) -> {
            CompoundTag nbt = payload.nbt();
            ctx.client().execute(() -> {
                try {
                    if (nbt.contains(WatutNetworking.NBTDataPlayerUUID)) {
                        UUID uuid = UUID.fromString(nbt.getStringOr(WatutNetworking.NBTDataPlayerUUID, ""));
                        WatutMod.getPlayerStatusManagerClient().receiveAny(uuid, nbt);
                    } else if (nbt.contains(WatutNetworking.NBTDataServerConfig)) {
                        WatutMod.getPlayerStatusManagerClient().receiveServerConfig(nbt);
                    } else if (nbt.contains(WatutNetworking.NBTDataItemTransferItemStack)) {
                        WatutMod.getPlayerStatusManagerClient().receiveItemMove(nbt);
                    }
                } catch (Exception ex) {
                    CULog.dbg("WATUT ERROR: packet with invalid uuid sent from server: " + nbt);
                    if (ConfigCoroUtil.useLoggingDebug) {
                        ex.printStackTrace();
                    }
                }
            });
        });
    }
}
