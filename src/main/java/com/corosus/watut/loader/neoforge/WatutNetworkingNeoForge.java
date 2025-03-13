package com.corosus.watut.loader.neoforge;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutModClient;
import com.corosus.watut.WatutNetworking;
import com.corosus.watut.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;

public class WatutNetworkingNeoForge extends WatutNetworking {

    public WatutNetworkingNeoForge() {
        super();
    }

    public static void register(Object... args) {
        registerClientboundPacket(
                PacketNBTFromServer.TYPE,
                PacketNBTFromServer.STREAM_CODEC,
                PacketNBTFromServer::handle,
                args
        );

        registerServerboundPacket(
                PacketNBTFromClient.TYPE,
                PacketNBTFromClient.STREAM_CODEC,
                PacketNBTFromClient::handle,
                args
        );
    }

    public static <T extends PacketBase, B extends FriendlyByteBuf> void registerServerboundPacket(
            CustomPacketPayload.Type<T> type,
            StreamCodec<B, T> codec,
            BiConsumer<T, Player> handler,
            Object... args) {

        PayloadRegistrar registrar = (PayloadRegistrar) args[0];

        IPayloadHandler<T> serverHandler = (packet, ctx) -> {
            ctx.enqueueWork(() -> {
                handler.accept(packet, ctx.player());
            });
        };

        registrar.playToServer(type, (StreamCodec<RegistryFriendlyByteBuf, T>)codec, serverHandler);
    }

    public static <T extends PacketBase, B extends FriendlyByteBuf> void registerClientboundPacket(
            CustomPacketPayload.Type<T> type,
            StreamCodec<B, T> codec,
            BiConsumer<T, Player> handler,
            Object... args)
    {
        PayloadRegistrar registrar = (PayloadRegistrar) args[0];

        IPayloadHandler<T> clientHandler = (packet, ctx) -> {
            ctx.enqueueWork(() -> {
                handler.accept(packet, WatutModClient.getPlayer());
            });
        };

        registrar.playToClient(type, (StreamCodec<RegistryFriendlyByteBuf, T>)codec, clientHandler);
    }

    @Override
    public void clientSendToServer(CompoundTag data) {
        PacketDistributor.sendToServer(new PacketNBTFromClient(data));
    }

    @Override
    public void serverSendToClientAll(CompoundTag data) {
        PacketDistributor.sendToAllPlayers(new PacketNBTFromServer(data));
    }

    @Override
    public void serverSendToClientPlayer(CompoundTag data, Player player) {
        PacketDistributor.sendToPlayer((ServerPlayer) player, new PacketNBTFromServer(data));
    }

    @Override
    public void serverSendToClientNear(CompoundTag data, Vec3 pos, double dist, Level level) {
        PacketDistributor.sendToPlayersNear((ServerLevel) level, null, pos.x, pos.y, pos.z, dist, new PacketNBTFromServer(data));
    }
}

