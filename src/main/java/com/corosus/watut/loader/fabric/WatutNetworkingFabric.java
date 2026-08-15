package com.corosus.watut.loader.fabric;

import com.corosus.watut.WatutNetworking;
import com.corosus.watut.network.PacketNBTFromClient;
import com.corosus.watut.network.PacketNBTFromServer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class WatutNetworkingFabric extends WatutNetworking {

    public WatutNetworkingFabric() {
        super();
    }

    @Override
    public void clientSendToServer(CompoundTag data) {
        ClientPlayNetworking.send(new PacketNBTFromClient(data));
    }

    @Override
    public void serverSendToClientAll(CompoundTag data) {
        if (WatutModFabric.minecraftServer != null) {
            for (ServerPlayer player : PlayerLookup.all(WatutModFabric.minecraftServer)) {
                ServerPlayNetworking.send(player, new PacketNBTFromServer(data));
            }
        }
    }

    @Override
    public void serverSendToClientPlayer(CompoundTag data, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new PacketNBTFromServer(data));
        }
    }

    @Override
    public void serverSendToClientNear(CompoundTag data, Vec3 pos, double dist, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : PlayerLookup.around(serverLevel, pos, dist)) {
                ServerPlayNetworking.send(player, new PacketNBTFromServer(data));
            }
        }
    }
}
