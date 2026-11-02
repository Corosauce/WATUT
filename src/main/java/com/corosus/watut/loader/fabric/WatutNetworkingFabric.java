package com.corosus.watut.loader.fabric;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import com.corosus.watut.network.PacketNBTFromClient;
import com.corosus.watut.network.PacketNBTFromServer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
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
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(data);
        //ClientPlayNetworking.send(NBT_PACKET_ID, buf);
        ClientPlayNetworking.send(new PacketNBTFromClient(data));
    }

    @Override
    public void serverSendToClientAll(CompoundTag data) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(data);
        for (ServerPlayer player : PlayerLookup.all(WatutModFabric.minecraftServer)) {
            //ServerPlayNetworking.send(player, NBT_PACKET_ID, buf);
            ServerPlayNetworking.send(player, new PacketNBTFromServer(data));
        }
        //HANDLER.send(PacketDistributor.ALL.noArg(), new PacketNBTFromServer(data));
    }

    @Override
    public void serverSendToClientPlayer(CompoundTag data, Player player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(data);
        //HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new PacketNBTFromServer(data));
        //ServerPlayNetworking.send((ServerPlayer) player, NBT_PACKET_ID, buf);
        ServerPlayNetworking.send((ServerPlayer) player, new PacketNBTFromServer(data));
    }

    @Override
    public void serverSendToClientNear(CompoundTag data, Vec3 pos, double dist, Level level) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(data);
        for (ServerPlayer player : PlayerLookup.around((ServerLevel) level, pos, dist)) {
            ServerPlayNetworking.send(player, new PacketNBTFromServer(data));
        }
        /*HANDLER.send(PacketDistributor.NEAR.with(() ->
                        new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, dist, dimension)),
                new PacketNBTFromServer(data));*/
    }
}

