package com.corosus.watut;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class WatutNetworking {

    private static final String PROTOCOL_VERSION = Integer.toString(4);
    private static short lastID = 0;
    public static final ResourceLocation NETWORK_CHANNEL_ID_MAIN = new ResourceLocation(Watut.MODID, "main");

    public static String NBTPacketCommand = "packetCommand";
    public static String NBTPacketCommandUpdateStatusPlayer = "updateStatusPlayer";
    public static String NBTPacketCommandUpdateMousePlayer = "updateMousePlayer";
    public static String NBTPacketCommandUpdateStatusAll = "updateStatusAll";
    public static String NBTDataPlayerUUID = "playerUuid";
    public static String NBTDataPlayerStatus = "playerStatus";
    public static String NBTDataPlayerMouseX = "playerMouseX";
    public static String NBTDataPlayerMouseY = "playerMouseY";
    public static String NBTDataPlayerMousePressed = "playerMousePressed";

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

}

