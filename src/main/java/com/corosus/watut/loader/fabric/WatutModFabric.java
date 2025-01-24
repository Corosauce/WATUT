package com.corosus.watut.loader.fabric;

import com.corosus.watut.WatutMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import java.util.ArrayList;
import java.util.List;

public class WatutModFabric extends WatutMod implements ModInitializer {

	public static MinecraftServer minecraftServer = null;

	public WatutModFabric() {
		super();
		new WatutNetworkingFabric();
		loadConfigs();
	}

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register((minecraftServer) -> {
			WatutModFabric.minecraftServer = minecraftServer;
		});
		ServerPlayNetworking.registerGlobalReceiver(WatutNetworkingFabric.NBT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			CompoundTag nbt = buf.readNbt();
			server.execute(() -> {
				if (player != null) {
					WatutMod.getPlayerStatusManagerServer().receiveAny(player, nbt);
				}
			});
		});
	}

	@Override
	public PlayerList getPlayerList() {
		if (minecraftServer == null) return null;
		return minecraftServer.getPlayerList();
	}

	@Override
	public boolean isModInstalled(String modID) {
		return FabricLoader.getInstance().isModLoaded(modID);
	}
}