package com.corosus.watut;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public abstract class WatutNetworking {

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

    public abstract void clientSendToServer(CompoundNBT data);

    public abstract void serverSendToClientAll(CompoundNBT data);

    public abstract void serverSendToClientPlayer(CompoundNBT data, PlayerEntity player);

    public abstract void serverSendToClientNear(CompoundNBT data, Vector3d pos, double dist, World level);

}

