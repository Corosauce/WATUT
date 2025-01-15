package com.corosus.watut;

import com.corosus.watut.config.ConfigCommon;
import com.corosus.watut.mixin.AbstractContainerMenuDoClick;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

public class PlayerStatusManagerServer extends PlayerStatusManager {

    @Override
    public void tickPlayer(Player player) {
        getStatus(player).setTicksToMarkPlayerIdleSyncedForClient(ConfigCommon.ticksToMarkPlayerIdle);
        super.tickPlayer(player);
    }

    /**
     * receive data from client, inject the relevant player uuid, and send it right back to the rest of the relevant clients
     *
     * @param player
     * @param data
     */
    public void receiveAny(Player player, CompoundTag data) {
        data.putString(WatutNetworking.NBTDataPlayerUUID, player.getUUID().toString());

        if (data.contains(WatutNetworking.NBTDataPlayerGuiStatus)) {
            PlayerStatus.PlayerGuiState playerGuiState = PlayerStatus.PlayerGuiState.get(data.getInt(WatutNetworking.NBTDataPlayerGuiStatus));
            getStatus(player).setPlayerGuiState(playerGuiState);
        }

        if (data.contains(WatutNetworking.NBTDataPlayerChatStatus)) {
            PlayerStatus.PlayerChatState state = PlayerStatus.PlayerChatState.get(data.getInt(WatutNetworking.NBTDataPlayerChatStatus));
            getStatus(player).setPlayerChatState(state);
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

        if (data.contains(WatutNetworking.NBTDataPlayerGuiStatus) || data.contains(WatutNetworking.NBTDataPlayerIdleTicks)/* || data.contains(WatutNetworking.NBTDataPlayerScreenCompressedPixelData)*/) {
            WatutNetworking.instance().serverSendToClientAll(data);
        } else {
            WatutNetworking.instance().serverSendToClientNear(data, player.position(), nearbyPlayerDataSendDist, player.level());
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
            WatutMod.instance().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
        }
    }

    @Override
    public void playerLoggedIn(Player player) {
        super.playerLoggedIn(player);

        WatutMod.dbg("player logged in");
        if (player instanceof ServerPlayer) {
            for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
                WatutMod.dbg("sending update all packet for " + entry.getKey().toString() + " to " + player.getDisplayName().getString() + " with status " + PlayerStatus.PlayerGuiState.get(entry.getValue().getNbtCache().getInt(WatutNetworking.NBTDataPlayerGuiStatus)));
                WatutNetworking.instance().serverSendToClientPlayer(entry.getValue().getNbtCache(), player);
            }
        }
    }

    private InventorySnapshot inventorySnapshotPlayer;
    private InventorySnapshot inventorySnapshotContainer;

    public void doClickPre(AbstractContainerMenu abstractContainerMenu, int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_) {
        System.out.println("pre");
        //TODO: verify its a real player not a fake player so we dont cause wasted overhead
        System.out.println(abstractContainerMenu.slots.size());
        inventorySnapshotPlayer = new InventorySnapshot();
        inventorySnapshotContainer = new InventorySnapshot();
        for (ItemStack item : p_150434_.getInventory().items) {
            inventorySnapshotPlayer.itemStackList.add(item.copy());
        }
        for (Slot slot : abstractContainerMenu.slots) {
            inventorySnapshotContainer.itemStackList.add(slot.getItem().copy());
        }
    }

    public void doClickPost(AbstractContainerMenu abstractContainerMenu, int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_) {
        System.out.println("post");
        //TODO: verify its a real player not a fake player so we dont cause wasted overhead
        InventorySnapshot inventorySnapshotPlayerPost = new InventorySnapshot();
        InventorySnapshot inventorySnapshotContainerPost = new InventorySnapshot();
        for (ItemStack item : p_150434_.getInventory().items) {
            inventorySnapshotPlayerPost.itemStackList.add(item.copy());
        }
        for (Slot slot : abstractContainerMenu.slots) {
            inventorySnapshotContainerPost.itemStackList.add(slot.getItem().copy());
        }

        System.out.println("player:");

        for (int i = 0; i < inventorySnapshotPlayerPost.itemStackList.size(); i++) {
            ItemStack stack1 = inventorySnapshotPlayer.itemStackList.get(i);
            ItemStack stack2 = inventorySnapshotPlayerPost.itemStackList.get(i);
            if (!stack1.equals(stack2, false)) {
                System.out.println(i + " prev: " + stack1 + " vs now: " + stack2);
            }
        }

        System.out.println("container:");

        for (int i = 0; i < inventorySnapshotContainerPost.itemStackList.size(); i++) {
            ItemStack stack1 = inventorySnapshotContainer.itemStackList.get(i);
            ItemStack stack2 = inventorySnapshotContainerPost.itemStackList.get(i);
            if (!stack1.equals(stack2, false)) {
                System.out.println(i + " prev: " + stack1 + " vs now: " + stack2);
            }
        }
    }
}
