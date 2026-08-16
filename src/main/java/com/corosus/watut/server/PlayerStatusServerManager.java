package com.corosus.watut.server;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigCommon;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.config.ConfigServerSyncHelper;
import com.corosus.watut.network.WatutNetworking;
import com.corosus.watut.status.PlayerChatState;
import com.corosus.watut.status.PlayerGuiState;
import com.corosus.watut.status.PlayerStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestore dello stato dei giocatori sul Server (ricezione pacchetti C2S, relay a client vicini, sync config e annunci).
 */
public class PlayerStatusServerManager {

    private final Map<UUID, PlayerStatus> lookupPlayerToStatus = new HashMap<>();
    private boolean singleplayerTesting = false;

    public PlayerStatus getStatus(Player player) {
        return getStatus(player.getUUID());
    }

    public PlayerStatus getStatus(UUID uuid) {
        return lookupPlayerToStatus.computeIfAbsent(uuid, k -> new PlayerStatus(PlayerGuiState.NONE, k));
    }

    public void tickPlayer(Player player) {
        getStatus(player).setTicksToMarkPlayerIdleSyncedForClient(ConfigCommon.ticksToMarkPlayerIdle);
    }

    public void receiveAny(Player player, CompoundTag data) {
        data.putString(WatutNetworking.NBTDataPlayerUUID, player.getUUID().toString());

        if (data.contains(WatutNetworking.NBTDataPlayerGuiStatus)) {
            PlayerGuiState state = PlayerGuiState.get(data.getIntOr(WatutNetworking.NBTDataPlayerGuiStatus, 0));
            getStatus(player).setPlayerGuiState(state);
        }

        if (data.contains(WatutNetworking.NBTDataPlayerChatStatus)) {
            PlayerChatState state = PlayerChatState.get(data.getIntOr(WatutNetworking.NBTDataPlayerChatStatus, 0));
            getStatus(player).setPlayerChatState(state);
        }

        if (data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) {
            handleIdleState(player, data.getIntOr(WatutNetworking.NBTDataPlayerIdleTicks, 0));
            data.putInt(WatutNetworking.NBTDataPlayerTicksToGoIdle, ConfigCommon.ticksToMarkPlayerIdle);
        }

        if (data.contains(WatutNetworking.NBTDataPlayerMouseX)) {
            float x = data.getFloatOr(WatutNetworking.NBTDataPlayerMouseX, 0f);
            float y = data.getFloatOr(WatutNetworking.NBTDataPlayerMouseY, 0f);
            boolean pressed = data.getBooleanOr(WatutNetworking.NBTDataPlayerMousePressed, false);
            PlayerStatus status = getStatus(player);
            status.setScreenPosPercentX(x);
            status.setScreenPosPercentY(y);
            status.setPressing(pressed);
        }

        getStatus(player).getNbtCache().merge(data);

        if (data.contains(WatutNetworking.NBTDataPlayerGuiStatus)
                || data.contains(WatutNetworking.NBTDataPlayerIdleTicks)
                || data.contains(WatutNetworking.NBTDataPlayerChatStatus)) {
            WatutNetworking.instance().serverSendToClientAll(data);
        } else {
            WatutNetworking.instance().serverSendToClientNear(data, player.position(), ConfigServerControlledSyncedToClient.distanceRequiredToShowGUIInfo, player.level());
        }
    }

    public void handleIdleState(Player player, int idleTicks) {
        if (WatutMod.minecraftServer == null) return;
        PlayerStatus status = getStatus(player);
        if (WatutMod.minecraftServer.getPlayerList().getPlayerCount() > 1 || singleplayerTesting) {
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
        if (WatutMod.minecraftServer == null) return;
        if (ConfigCommon.announceIdleStatesInChat) {
            WatutMod.minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
        }
    }

    public void playerLoggedIn(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
                WatutNetworking.instance().serverSendToClientPlayer(entry.getValue().getNbtCache(), player);
            }
            WatutNetworking.instance().serverSendToClientPlayer(getServerConfigNBT(), serverPlayer);
        }
    }

    public void syncServerConfigToAllPlayers() {
        if (WatutMod.minecraftServer == null) return;
        for (ServerPlayer serverPlayer : WatutMod.minecraftServer.getPlayerList().getPlayers()) {
            WatutNetworking.instance().serverSendToClientPlayer(getServerConfigNBT(), serverPlayer);
        }
    }

    public CompoundTag getServerConfigNBT() {
        CompoundTag nbt = ConfigServerSyncHelper.getInstance().getSyncableConfigOnServer();
        nbt.putBoolean(WatutNetworking.NBTDataServerConfig, true);
        return nbt;
    }

    public void doClickPre(AbstractContainerMenu menu, int slotId, int button, ContainerInput clickType, Player player) {
        ItemTransferDetector.doClickPre(menu, slotId, button, clickType, player, getStatus(player));
    }

    public void doClickPost(AbstractContainerMenu menu, int slotId, int button, ContainerInput clickType, Player player) {
        ItemTransferDetector.doClickPost(menu, slotId, button, clickType, player, getStatus(player));
    }

    public void useBlock(Player player, BlockPos pos) {
        ItemTransferDetector.useBlock(player, pos, getStatus(player));
    }
}
