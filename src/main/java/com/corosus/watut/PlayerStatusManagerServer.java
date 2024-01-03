package com.corosus.watut;

import com.corosus.watut.config.ConfigCommon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;

import java.util.Map;
import java.util.UUID;

public class PlayerStatusManagerServer extends PlayerStatusManager {

    @Override
    public void tickPlayer(PlayerEntity player) {
        getStatus(player).setTicksToMarkPlayerIdleSyncedForClient(ConfigCommon.ticksToMarkPlayerIdle);
        super.tickPlayer(player);
    }

    /**
     * receive data from client, inject the relevant player uuid, and send it right back to the rest of the relevant clients
     *
     * @param player
     * @param data
     */
    public void receiveAny(PlayerEntity player, CompoundNBT data) {
        data.putString(WatutNetworking.NBTDataPlayerUUID, player.getUUID().toString());

        if (data.contains(WatutNetworking.NBTDataPlayerStatus)) {
            PlayerStatus.PlayerGuiState playerGuiState = PlayerStatus.PlayerGuiState.get(data.getInt(WatutNetworking.NBTDataPlayerStatus));
            getStatus(player).setPlayerGuiState(playerGuiState);
        }

        if (data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) {
            handleIdleState(player, data.getInt(WatutNetworking.NBTDataPlayerIdleTicks));
            //send latest config setting for ticks to go idle
            data.putInt(WatutNetworking.NBTDataPlayerTicksToGoIdle, ConfigCommon.ticksToMarkPlayerIdle);
        }

        if (data.contains(WatutNetworking.NBTDataPlayerMouseX)) {
            float x = data.getFloat(WatutNetworking.NBTDataPlayerMouseX);
            float y = data.getFloat(WatutNetworking.NBTDataPlayerMouseY);
            boolean pressed = data.getBoolean(WatutNetworking.NBTDataPlayerMousePressed);
            setMouse(player.getUUID(), x, y, pressed);
        }

        //update active snapshot with latest data
        getStatus(player).getNbtCache().merge(data);

        if (data.contains(WatutNetworking.NBTDataPlayerStatus) || data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) {
            WatutNetworking.instance().serverSendToClientAll(data);
        } else {
            WatutNetworking.instance().serverSendToClientNear(data, player.position(), nearbyPlayerDataSendDist, player.level);
        }
    }

    public void handleIdleState(PlayerEntity player, int idleTicks) {
        PlayerStatus status = getStatus(player);
        if (WatutMod.instance().getPlayerList().getPlayerCount() > 1 || singleplayerTesting) {
            if (idleTicks > ConfigCommon.ticksToMarkPlayerIdle) {
                if (!status.isIdle()) {
                    broadcast(player.getDisplayName().getString() + " has gone idle");
                }
            } else {
                if (status.isIdle()) {
                    broadcast(player.getDisplayName().getString() + " is no longer idle");
                }
            }
        }
        status.setTicksSinceLastAction(idleTicks);
    }

    public void broadcast(String msg) {
        if (ConfigCommon.announceIdleStatesInChat) {
            WatutMod.instance().getPlayerList().broadcastMessage(new StringTextComponent(msg), ChatType.CHAT, new UUID(0, 0));
        }
    }

    @Override
    public void playerLoggedIn(PlayerEntity player) {
        super.playerLoggedIn(player);

        WatutMod.dbg("player logged in");
        if (player instanceof ServerPlayerEntity) {
            for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
                WatutMod.dbg("sending update all packet for " + entry.getKey().toString() + " to " + player.getDisplayName().getString() + " with status " + PlayerStatus.PlayerGuiState.get(entry.getValue().getNbtCache().getInt(WatutNetworking.NBTDataPlayerStatus)));
                WatutNetworking.instance().serverSendToClientPlayer(entry.getValue().getNbtCache(), player);
            }
        }
    }
}
