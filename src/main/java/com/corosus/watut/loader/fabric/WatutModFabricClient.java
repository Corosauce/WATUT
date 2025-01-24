package com.corosus.watut.loader.fabric;

import com.corosus.coroutil.config.ConfigCoroUtil;
import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import com.corosus.watut.particle.ParticleRotating;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WatutModFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(WatutNetworkingFabric.NBT_PACKET_ID, (client, handler, buf, responseSender) -> {
			CompoundTag nbt = buf.readNbt();
			client.execute(() -> {
				try {
					if (nbt.contains(WatutNetworking.NBTDataPlayerUUID)) {
						UUID uuid = UUID.fromString(nbt.getString(WatutNetworking.NBTDataPlayerUUID));
						WatutMod.getPlayerStatusManagerClient().receiveAny(uuid, nbt);
					} else if (nbt.contains(WatutNetworking.NBTDataServerConfig)) {
						WatutMod.getPlayerStatusManagerClient().receiveServerConfig(nbt);
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

		List<ParticleRenderType> render_order = new ArrayList<>();
		render_order.addAll(ParticleEngine.RENDER_ORDER);
		render_order.add(ParticleRotating.PARTICLE_SHEET_TRANSLUCENT_NO_FACE_CULL);
		ParticleEngine.RENDER_ORDER = render_order;
	}

}