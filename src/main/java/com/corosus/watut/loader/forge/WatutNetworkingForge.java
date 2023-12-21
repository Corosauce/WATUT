package com.corosus.watut.loader.forge;

import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import com.corosus.watut.loader.forge.PacketNBTFromClient;
import com.corosus.watut.loader.forge.PacketNBTFromServer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class WatutNetworkingForge extends WatutNetworking {

    private static final String PROTOCOL_VERSION = Integer.toString(4);
    private static short lastID = 0;
    public static final ResourceLocation NETWORK_CHANNEL_ID_MAIN = new ResourceLocation(WatutMod.MODID, "main");

    public WatutNetworkingForge() {
        super();
    }

    public static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder
            .named(NETWORK_CHANNEL_ID_MAIN)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    public static void register() {
        registerMessage(PacketNBTFromServer.class, PacketNBTFromServer::encode, PacketNBTFromServer::decode, PacketNBTFromServer.Handler::handle, NetworkDirection.PLAY_TO_CLIENT);
        registerMessage(PacketNBTFromClient.class, PacketNBTFromClient::encode, PacketNBTFromClient::decode, PacketNBTFromClient.Handler::handle, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer, NetworkDirection networkDirection) {
        HANDLER.registerMessage(lastID, messageType, encoder, decoder, messageConsumer, Optional.ofNullable(networkDirection));
        lastID++;
        if (lastID > 0xFF)
            throw new RuntimeException("Too many messages!");
    }

    @Override
    public void clientSendToServer(CompoundTag data) {
        HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
    }

    @Override
    public void serverSendToClientAll(CompoundTag data) {
        HANDLER.send(PacketDistributor.ALL.noArg(), new PacketNBTFromServer(data));
    }

    @Override
    public void serverSendToClientPlayer(CompoundTag data, Player player) {
        HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new PacketNBTFromServer(data));
    }

    @Override
    public void serverSendToClientNear(CompoundTag data, Vec3 pos, double dist, Level level) {
        HANDLER.send(PacketDistributor.NEAR.with(() ->
                        new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, dist, level.dimension())),
                new PacketNBTFromServer(data));
    }
}

