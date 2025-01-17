package com.corosus.watut;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class WatutNetworking {

    public static String NBTDataPlayerUUID = "playerUuid";
    public static String NBTDataPlayerGuiStatus = "playerGuiStatus";
    public static String NBTDataPlayerGuiDontSendDetailedGUIInfo = "dontSendDetailedGUIInfo";
    public static String NBTDataPlayerChatStatus = "playerChatStatus";
    public static String NBTDataPlayerTypingAmp = "playerTypingAmp";
    //public static String NBTDataPlayerScreenRenderCalls = "screenRenderCalls";
    public static String NBTDataPlayerScreenCompressedPixelData = "screenCompressedPixelData";
    public static String NBTDataPlayerScreenCompressedPixelDataPacketCount = "screenCompressedPixelDataPacketCount";
    public static String NBTDataPlayerScreenCompressedPixelDataPacketIndex = "screenCompressedPixelDataPacketIndex";
    //public static String NBTDataPlayerScreenCompressedPixelDataMD5 = "screenCompressedPixelDataMD5";
    public static String NBTDataPlayerScreenCompressedPixelDataSize = "screenCompressedPixelDataSize";
    public static String NBTDataPlayerScreenClass = "screenClass";
    public static String NBTDataPlayerIdleTicks = "playerIdleTicks";
    //a bit of a heavy way to sync a server config to client, but itll do for now
    public static String NBTDataPlayerTicksToGoIdle = "playerTicksToGoIdle";
    public static String NBTDataPlayerMouseX = "playerMouseX";
    public static String NBTDataPlayerMouseY = "playerMouseY";
    public static String NBTDataPlayerMousePressed = "playerMousePressed";

    //server to client config
    public static String NBTDataServerConfig = "serverConfig";
    public static String NBTData_useOldSimpleGUIVisual = "useOldSimpleGUIVisual";
    public static String NBTData_tickSendRateOfGUIUpdates = "tickSendRateOfGUIUpdates";
    public static String NBTData_blurLevel = "blurLevel";
    public static String NBTData_sizeRadiusInPixelsToShow = "sizeRadiusInPixelsToShow";


    private static WatutNetworking instance;

    public static WatutNetworking instance() {
        return instance;
    }

    public WatutNetworking() {
        instance = this;
    }

    public abstract void clientSendToServer(CompoundTag data);

    public abstract void serverSendToClientAll(CompoundTag data);

    public abstract void serverSendToClientPlayer(CompoundTag data, Player player);

    public abstract void serverSendToClientNear(CompoundTag data, Vec3 pos, double dist, Level level);

}

