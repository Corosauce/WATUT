package com.corosus.watut;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.config.ConfigCommon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;

import java.util.Map;
import java.util.UUID;

public class PlayerStatusManagerServer extends PlayerStatusManager {

    @Override
    public void tickPlayer(Player player) {
        getStatus(player).setTicksToMarkPlayerIdleSyncedForClient(ConfigCommon.ticksToMarkPlayerIdle);
        super.tickPlayer(player);

        int dayNumber = ((ServerPlayer)player).getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 24000;
        int resetTime = 24000 * ConfigCommon.dc_resetDay;

        boolean stopProgress = false;

        if (player.getTags().contains("no_hordes") || (ConfigCommon.dc_noHordesIfWatutIdle && getStatus(player).isIdle())) {
            stopProgress = true;
        }

        if (ConfigCommon.dc_noHordesIfGameTimePaused && !player.level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            stopProgress = true;
        }

        if (ConfigCommon.dc_noHordesIfInCreative && player.isCreative()) {
            stopProgress = true;
        }

        if (stopProgress) {
            player.awardStat(Stats.CUSTOM.get(Stats.PLAY_TIME), -1);
        }

        if (ConfigCommon.dc_useResetBeforeDay104) {
            if (dayNumber >= 104) {
                if (ConfigCommon.dc_dbg) CULog.log("player " + player.getScoreboardName() + " reset play_time to: " + resetTime);
                ((ServerPlayer)player).getStats().setValue(player, Stats.CUSTOM.get(Stats.PLAY_TIME), resetTime);
            }
        }

        //((ServerPlayer)player).getStats().setValue(player, Stats.CUSTOM.get(Stats.PLAY_TIME), 24000 * 104);

        if (player.level.getGameTime() % 200 == 0 && ConfigCommon.dc_dbg) {
            CULog.log("player " + player.getScoreboardName() + " play_time: " + ((ServerPlayer)player).getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) + ", day Number: " + dayNumber);
        }
    }

    /**
     * receive data from client, inject the relevant player uuid, and send it right back to the rest of the relevant clients
     *
     * @param player
     * @param data
     */
    public void receiveAny(Player player, CompoundTag data) {
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

    public void handleIdleState(Player player, int idleTicks) {
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
            WatutMod.instance().getPlayerList().broadcastMessage(new TextComponent(msg), ChatType.CHAT, new UUID(0, 0));
        }
    }

    @Override
    public void playerLoggedIn(Player player) {
        super.playerLoggedIn(player);

        WatutMod.dbg("player logged in");
        if (player instanceof ServerPlayer) {
            for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
                WatutMod.dbg("sending update all packet for " + entry.getKey().toString() + " to " + player.getDisplayName().getString() + " with status " + PlayerStatus.PlayerGuiState.get(entry.getValue().getNbtCache().getInt(WatutNetworking.NBTDataPlayerStatus)));
                WatutNetworking.instance().serverSendToClientPlayer(entry.getValue().getNbtCache(), player);
            }
        }
    }
}
