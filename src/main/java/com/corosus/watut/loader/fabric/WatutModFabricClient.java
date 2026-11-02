package com.corosus.watut.loader.fabric;

import com.corosus.coroutil.config.ConfigCoroUtil;
import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import com.corosus.watut.network.PacketNBTFromServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class WatutModFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WatutMod.getPlayerStatusManagerClient().onDisconnect());
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
					CULog.dbg("WATUT ERROR: packet with invalid uuid sent from server");
					CULog.dbg("full nbt data: " + nbt);
					if (ConfigCoroUtil.useLoggingDebug) {
						ex.printStackTrace();
					}
				}
			});
		});

		/*List<ParticleRenderType> render_order = new ArrayList<>();
		render_order.addAll(ParticleEngine.RENDER_ORDER);
		render_order.add(ParticleRotating.PARTICLE_SHEET_TRANSLUCENT_NO_FACE_CULL);
		ParticleEngine.RENDER_ORDER = render_order;*/

		//ShaderRegistry.init();
	}

}