package com.corosus.watut;

import com.corosus.coroutil.util.CULog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public abstract class WatutNetworking {

    public static String NBTDataPlayerUUID = "playerUuid";
    public static String NBTDataPlayerGuiStatus = "playerGuiStatus";
    public static String NBTDataPlayerGuiDontSendDetailedGUIInfo = "dontSendDetailedGUIInfo";
    public static String NBTDataPlayerGuiDontSendItemInfo = "dontSendDetailedItemInfo";
    public static String NBTDataPlayerChatStatus = "playerChatStatus";
    public static String NBTDataPlayerTypingAmp = "playerTypingAmp";
    //public static String NBTDataPlayerScreenRenderCalls = "screenRenderCalls";
    public static String NBTDataPlayerScreenCompressedPixelData = "screenCompressedPixelData";
    public static String NBTDataPlayerScreenCompressedPixelDataPacketCount = "screenCompressedPixelDataPacketCount";
    public static String NBTDataPlayerScreenCompressedPixelDataPacketIndex = "screenCompressedPixelDataPacketIndex";
    //public static String NBTDataPlayerScreenCompressedPixelDataMD5 = "screenCompressedPixelDataMD5";
    public static String NBTDataPlayerScreenCompressedPixelDataSize = "screenCompressedPixelDataSize";
    public static String NBTDataPlayerScreenWidth = "screenWidth";
    public static String NBTDataPlayerScreenHeight = "screenHeight";
    public static String NBTDataPlayerIdleTicks = "playerIdleTicks";
    //a bit of a heavy way to sync a server config to client, but itll do for now
    public static String NBTDataPlayerTicksToGoIdle = "playerTicksToGoIdle";
    public static String NBTDataPlayerMouseX = "playerMouseX";
    public static String NBTDataPlayerMouseY = "playerMouseY";
    public static String NBTDataPlayerMousePressed = "playerMousePressed";
    public static String NBTDataItemTransferItemStack = "itemTransferItemStack";
    public static String NBTDataItemTransferFromX = "itemTransferFromX";
    public static String NBTDataItemTransferFromY = "itemTransferFromY";
    public static String NBTDataItemTransferFromZ = "itemTransferFromZ";
    public static String NBTDataItemTransferToX = "itemTransferToX";
    public static String NBTDataItemTransferToY = "itemTransferToY";
    public static String NBTDataItemTransferToZ = "itemTransferToZ";

    //server to client config
    public static String NBTDataServerConfig = "serverConfig";

    public List<CompoundTag> listQueue = new ArrayList<>();
    public long lastTickProcessed = 0;

    private static WatutNetworking instance;

    public static WatutNetworking instance() {
        return instance;
    }

    public WatutNetworking() {
        instance = this;
    }

    public abstract void clientSendToServer(CompoundTag data);
    public void clientSendToServerAddToQueue(CompoundTag data) {
        listQueue.add(data);
    }

    public void process1ItemFromQueue(Level level) {
        //just 1 item, to fix our issue of multi part packets having issues when all sent at once
        if (!listQueue.isEmpty() && level.getGameTime() > lastTickProcessed + 20) {
            lastTickProcessed = level.getGameTime();
            clientSendToServer(listQueue.remove(0));
            CULog.dbg("sending packet on tick " + level.getGameTime() + " queue size " + listQueue.size());
        }
    }

    public abstract void serverSendToClientAll(CompoundTag data);

    public abstract void serverSendToClientPlayer(CompoundTag data, Player player);

    public abstract void serverSendToClientNear(CompoundTag data, Vec3 pos, double dist, Level level);

}

