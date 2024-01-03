package com.corosus.watut.loader.forge;

import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

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

    private static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, PacketBuffer> encoder, Function<PacketBuffer, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer, NetworkDirection networkDirection) {
        HANDLER.registerMessage(lastID, messageType, encoder, decoder, messageConsumer, Optional.ofNullable(networkDirection));
        lastID++;
        if (lastID > 0xFF)
            throw new RuntimeException("Too many messages!");
    }

    @Override
    public void clientSendToServer(CompoundNBT data) {
        HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
    }

    @Override
    public void serverSendToClientAll(CompoundNBT data) {
        HANDLER.send(PacketDistributor.ALL.noArg(), new PacketNBTFromServer(data));
    }

    @Override
    public void serverSendToClientPlayer(CompoundNBT data, PlayerEntity player) {
        HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new PacketNBTFromServer(data));
    }

    @Override
    public void serverSendToClientNear(CompoundNBT data, Vector3d pos, double dist, World level) {
        HANDLER.send(PacketDistributor.NEAR.with(() ->
                        new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, dist, level.dimension())),
                new PacketNBTFromServer(data));
    }
}

