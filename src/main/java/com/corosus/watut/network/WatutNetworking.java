package com.corosus.watut.network;

import com.corosus.watut.WatutMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Gestore centralizzato del networking Fabric per WATUT.
 */
public class WatutNetworking {

    public static final String NBTDataPlayerUUID = "playerUuid";
    public static final String NBTDataPlayerGuiStatus = "playerGuiStatus";
    public static final String NBTDataPlayerGuiDontSendDetailedGUIInfo = "dontSendDetailedGUIInfo";
    public static final String NBTDataPlayerGuiDontSendItemInfo = "dontSendDetailedItemInfo";
    public static final String NBTDataPlayerChatStatus = "playerChatStatus";
    public static final String NBTDataPlayerTypingAmp = "playerTypingAmp";
    public static final String NBTDataPlayerScreenCompressedPixelData = "screenCompressedPixelData";
    public static final String NBTDataPlayerScreenCompressedPixelDataPacketCount = "screenCompressedPixelDataPacketCount";
    public static final String NBTDataPlayerScreenCompressedPixelDataPacketIndex = "screenCompressedPixelDataPacketIndex";
    public static final String NBTDataPlayerScreenCompressedPixelDataSize = "screenCompressedPixelDataSize";
    public static final String NBTDataPlayerScreenWidth = "screenWidth";
    public static final String NBTDataPlayerScreenHeight = "screenHeight";
    public static final String NBTDataPlayerIdleTicks = "playerIdleTicks";
    public static final String NBTDataPlayerTicksToGoIdle = "playerTicksToGoIdle";
    public static final String NBTDataPlayerMouseX = "playerMouseX";
    public static final String NBTDataPlayerMouseY = "playerMouseY";
    public static final String NBTDataPlayerMousePressed = "playerMousePressed";
    public static final String NBTDataItemTransferItemStack = "itemTransferItemStack";
    public static final String NBTDataItemTransferFromX = "itemTransferFromX";
    public static final String NBTDataItemTransferFromY = "itemTransferFromY";
    public static final String NBTDataItemTransferFromZ = "itemTransferFromZ";
    public static final String NBTDataItemTransferToX = "itemTransferToX";
    public static final String NBTDataItemTransferToY = "itemTransferToY";
    public static final String NBTDataItemTransferToZ = "itemTransferToZ";
    public static final String NBTDataServerConfig = "serverConfig";

    private static final WatutNetworking INSTANCE = new WatutNetworking();

    public static WatutNetworking instance() {
        return INSTANCE;
    }

    public void clientSendToServer(CompoundTag data) {
        ClientPlayNetworking.send(new PacketNBTFromClient(data));
    }

    public void serverSendToClientAll(CompoundTag data) {
        if (WatutMod.minecraftServer != null) {
            for (ServerPlayer player : PlayerLookup.all(WatutMod.minecraftServer)) {
                ServerPlayNetworking.send(player, new PacketNBTFromServer(data));
            }
        }
    }

    public void serverSendToClientPlayer(CompoundTag data, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ServerPlayNetworking.send(serverPlayer, new PacketNBTFromServer(data));
        }
    }

    public void serverSendToClientNear(CompoundTag data, Vec3 pos, double dist, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : PlayerLookup.around(serverLevel, pos, dist)) {
                ServerPlayNetworking.send(player, new PacketNBTFromServer(data));
            }
        }
    }
}
