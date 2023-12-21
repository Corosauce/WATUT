package com.corosus.watut;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class WatutNetworking {

    private static final String PROTOCOL_VERSION = Integer.toString(4);
    private static short lastID = 0;
    public static final ResourceLocation NETWORK_CHANNEL_ID_MAIN = new ResourceLocation(WatutMod.MODID, "main");

    public static String NBTDataPlayerUUID = "playerUuid";
    public static String NBTDataPlayerStatus = "playerStatus";
    public static String NBTDataPlayerTypingAmp = "playerTypingAmp";
    public static String NBTDataPlayerIdleTicks = "playerIdleTicks";
    //a bit of a heavy way to sync a server config to client, but itll do for now
    public static String NBTDataPlayerTicksToGoIdle = "playerTicksToGoIdle";
    public static String NBTDataPlayerMouseX = "playerMouseX";
    public static String NBTDataPlayerMouseY = "playerMouseY";
    public static String NBTDataPlayerMousePressed = "playerMousePressed";

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

