package com.corosus.watut;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.config.ConfigCommon;
import com.corosus.watut.config.ConfigServer;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerStatusManagerServer extends PlayerStatusManager {

    @Override
    public void tickPlayer(Player player) {
        getStatus(player).setTicksToMarkPlayerIdleSyncedForClient(ConfigCommon.ticksToMarkPlayerIdle);
        super.tickPlayer(player);

        /*if (player.level().getGameTime() % 20 == 0) {
            sendItemMove(player.level(), new ItemStack(Items.CHEST), player.getX(), player.getY() + 1, player.getZ(), player.getX() + 2, player.getY(), player.getZ());
        }*/
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

        if (data.contains(WatutNetworking.NBTDataPlayerGuiStatus) || data.contains(WatutNetworking.NBTDataPlayerIdleTicks) || data.contains(WatutNetworking.NBTDataPlayerChatStatus)/* || data.contains(WatutNetworking.NBTDataPlayerScreenCompressedPixelData)*/) {
            WatutNetworking.instance().serverSendToClientAll(data);
        } else {
            WatutNetworking.instance().serverSendToClientNear(data, player.position(), ConfigServer.distanceRequiredToShowGUIInfo, player.level());
        }
    }

    public void handleIdleState(Player player, int idleTicks) {
        if (WatutMod.instance().getPlayerList() == null) return;
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
        if (WatutMod.instance().getPlayerList() == null) return;
        if (ConfigCommon.announceIdleStatesInChat) {
            WatutMod.instance().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
        }
    }

    @Override
    public void playerLoggedIn(Player player) {
        super.playerLoggedIn(player);

        WatutMod.dbg("player logged in " + player.getName());
        if (player instanceof ServerPlayer) {
            for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
                WatutMod.dbg("sending update all packet for " + entry.getKey().toString() + " to " + player.getDisplayName().getString() + " with status " + PlayerStatus.PlayerGuiState.get(entry.getValue().getNbtCache().getInt(WatutNetworking.NBTDataPlayerGuiStatus)));
                WatutNetworking.instance().serverSendToClientPlayer(entry.getValue().getNbtCache(), player);
            }

            //sync server config to client
            CULog.dbg("sending server config sync to " + player.getName());
            WatutNetworking.instance().serverSendToClientPlayer(getServerConfigNBT(), player);
        }
    }

    public void syncServerConfigToAllPlayers() {
        if (WatutMod.instance().getPlayerList() == null) return;
        for (ServerPlayer serverPlayer : WatutMod.instance().getPlayerList().getPlayers()) {
            CULog.dbg("sending server config sync to " + serverPlayer.getName());
            WatutNetworking.instance().serverSendToClientPlayer(getServerConfigNBT(), serverPlayer);
        }
    }

    public CompoundTag getServerConfigNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean(WatutNetworking.NBTDataServerConfig, true);
        nbt.putBoolean(WatutNetworking.NBTData_useOldSimpleGUIVisual, ConfigServer.dynamicGuiUseOldSimpleGUIVisual);
        nbt.putInt(WatutNetworking.NBTData_tickSendRateOfGUIUpdates, ConfigServer.dynamicGuiTickSendRateOfGUIUpdates);
        nbt.putInt(WatutNetworking.NBTData_blurLevel, ConfigServer.dynamicGuiBlurLevel);
        nbt.putDouble(WatutNetworking.NBTData_sizeRadiusInPixelsToShow, ConfigServer.dynamicGuiSizeRadiusInPixelsToShow);
        nbt.putBoolean(WatutNetworking.NBTData_dynamicGuiShowClientsEntireScreen, ConfigServer.dynamicGuiShowClientsEntireScreen);
        nbt.putBoolean(WatutNetworking.NBTData_dynamicGuiDisableBackgroundRendering, ConfigServer.dynamicGuiDisableBackgroundRendering);
        nbt.putBoolean(WatutNetworking.NBTData_showItemsBeingTransferredBetweenPlayerAndContainer, ConfigServer.showItemsBeingTransferredBetweenPlayerAndContainer);
        nbt.putBoolean(WatutNetworking.NBTData_dynamicGuiDontSendConstantGUIUpdates, ConfigServer.dynamicGuiDontSendConstantGUIUpdates);
        nbt.putInt(WatutNetworking.NBTData_distanceRequiredToShowGUIInfo, ConfigServer.distanceRequiredToShowGUIInfo);
        return nbt;
    }

    public void sendItemMove(Player player, Level level, ItemStack itemStack, double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        this.sendItemMove(player, level, itemStack, (float)fromX, (float)fromY, (float)fromZ, (float)toX, (float)toY, (float)toZ);
    }

    public void sendItemMove(Player player, Level level, ItemStack itemStack, float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        if (level.getNearestPlayer(fromX, fromY, fromZ, ConfigServer.distanceRequiredToShowGUIInfo, (entity) -> entity != player) != null) {
            CompoundTag data = new CompoundTag();
            data.put(WatutNetworking.NBTDataItemTransferItemStack, itemStack.save(new CompoundTag()));
            data.putFloat(WatutNetworking.NBTDataItemTransferFromX, fromX);
            data.putFloat(WatutNetworking.NBTDataItemTransferFromY, fromY);
            data.putFloat(WatutNetworking.NBTDataItemTransferFromZ, fromZ);
            data.putFloat(WatutNetworking.NBTDataItemTransferToX, toX);
            data.putFloat(WatutNetworking.NBTDataItemTransferToY, toY);
            data.putFloat(WatutNetworking.NBTDataItemTransferToZ, toZ);

            //CULog.dbg("sending mouse status from client for " + Minecraft.getInstance().player.getUUID());
            //CULog.dbg("data: " + data);
            //WatutNetworking.instance().serverSendToClientNear(data);
            WatutNetworking.instance().serverSendToClientNear(data, new Vec3(fromX, fromY, fromZ), ConfigServer.distanceRequiredToShowGUIInfo, level);
        }
    }

    public void makeNewInventorySnapshot(AbstractContainerMenu abstractContainerMenu, Player player) {
        PlayerStatus playerStatus = getStatus(player);
        playerStatus.getInventorySnapshotPlayer().itemStackList.clear();
        playerStatus.getInventorySnapshotContainer().itemStackList.clear();
        playerStatus.getInventorySnapshotCarried().itemStackList.clear();
        for (ItemStack item : player.getInventory().items) {
            playerStatus.getInventorySnapshotPlayer().itemStackList.add(item.copy());
        }
        for (Slot slot : abstractContainerMenu.slots) {
            //filter out players slots
            if (!(slot.container instanceof Inventory)) {
                playerStatus.getInventorySnapshotContainer().itemStackList.add(slot.getItem().copy());
            }
        }
        if (!abstractContainerMenu.getCarried().isEmpty()) {
            playerStatus.getInventorySnapshotCarried().itemStackList.add(abstractContainerMenu.getCarried().copy());
        }

    }

    public void doClickPre(AbstractContainerMenu abstractContainerMenu, int pSlotId, int pButton, ClickType pClickType, Player player) {
        //System.out.println("? " + pClickType);

        if (!ConfigServer.showItemsBeingTransferredBetweenPlayerAndContainer) return;
        if (FakePlayerHelper.isFakePlayer(player)) return;
        PlayerStatus playerStatus = getStatus(player);
        if (playerStatus.isPlayerGuiDontSendItemInfo()) return;
        if (playerStatus.getLastBlockOpened().equals(BlockPos.ZERO)) return;

        //we only handle pickup states in post, also only support pickup or swap
        /*if (pClickType != ClickType.SWAP) {
            return;
        }*/

        makeNewInventorySnapshot(abstractContainerMenu, player);
    }

    public void useBlock(Player player, BlockPos pos) {
        if (!ConfigServer.showItemsBeingTransferredBetweenPlayerAndContainer) return;
        if (FakePlayerHelper.isFakePlayer(player)) return;
        PlayerStatus playerStatus = getStatus(player);
        if (playerStatus.isPlayerGuiDontSendItemInfo()) return;
        playerStatus.setLastBlockOpened(pos);
    }

    public void doClickPost(AbstractContainerMenu abstractContainerMenu, int pSlotId, int pButton, ClickType pClickType, Player player) {
        //TODO: click type handling, handle actual point when item moves
        if (!ConfigServer.showItemsBeingTransferredBetweenPlayerAndContainer) return;
        if (FakePlayerHelper.isFakePlayer(player)) return;
        PlayerStatus playerStatus = getStatus(player);
        if (playerStatus.isPlayerGuiDontSendItemInfo()) return;
        if (playerStatus.getLastBlockOpened().equals(BlockPos.ZERO)) return;

        InventorySnapshot inventorySnapshotPlayerPost = new InventorySnapshot();
        InventorySnapshot inventorySnapshotContainerPost = new InventorySnapshot();
        InventorySnapshot inventorySnapshotCarriedPost = new InventorySnapshot();
        for (ItemStack item : player.getInventory().items) {
            inventorySnapshotPlayerPost.itemStackList.add(item.copy());
        }
        for (Slot slot : abstractContainerMenu.slots) {
            //filter out players slots
            if (!(slot.container instanceof Inventory)) {
                inventorySnapshotContainerPost.itemStackList.add(slot.getItem().copy());
            }
        }
        if (!abstractContainerMenu.getCarried().isEmpty()) {
            inventorySnapshotCarriedPost.itemStackList.add(abstractContainerMenu.getCarried().copy());
        }

        Pair<List<ItemStack>, List<ItemStack>> listsPlayer = processInventorySnapshots(playerStatus.getInventorySnapshotPlayer().itemStackList, inventorySnapshotPlayerPost.itemStackList);
        List<ItemStack> playerAddedItems = listsPlayer.getFirst();
        List<ItemStack> playerRemovedItems = listsPlayer.getSecond();

        Pair<List<ItemStack>, List<ItemStack>> listsContainer = processInventorySnapshots(playerStatus.getInventorySnapshotContainer().itemStackList, inventorySnapshotContainerPost.itemStackList);
        List<ItemStack> containerAddedItems = listsContainer.getFirst();
        List<ItemStack> containerRemovedItems = listsContainer.getSecond();


        CULog.dbg("playerAddedItems " + playerAddedItems);
        CULog.dbg("playerRemovedItems " + playerRemovedItems);
        CULog.dbg("containerAddedItems " + containerAddedItems);
        CULog.dbg("containerRemovedItems " + containerRemovedItems);
        CULog.dbg("getInventorySnapshotCarriedPre " + playerStatus.getInventorySnapshotCarried().itemStackList);
        CULog.dbg("getInventorySnapshotCarriedPost " + inventorySnapshotCarriedPost.itemStackList);

        /**
         * v2 compare:
         *
         * compare player and container post against pre
         * - itemize the item and its count change, both up and down (be it 1 stack count change to the entire thing)
         * -- separate lists, items gained, items lost, easier for me
         * - for items lost, compare to container, if its item and count are in items gained (count doesnt have to match, just at least be that amount)
         * -- count it as a transfer
         * -- same for the inverse as youd expect
         */

        if (pClickType == ClickType.PICKUP) {
            CULog.dbg("? " + abstractContainerMenu.getCarried());


            //if (!abstractContainerMenu.getCarried().isEmpty()) {
            //make sure this only fires when only new item is to carried slot
            if (!abstractContainerMenu.getCarried().isEmpty() && playerAddedItems.size() == 0 && containerAddedItems.size() == 0) {
                //log which inventory the item was moved from
                ItemStack itemStackCarriedMatch = getMatchingItem(abstractContainerMenu.getCarried(), playerRemovedItems);
                if (itemStackCarriedMatch != null) {
                    playerStatus.setCarriedItemFromPlayerInventory(true);
                } else {
                    itemStackCarriedMatch = getMatchingItem(abstractContainerMenu.getCarried(), containerRemovedItems);
                    if (itemStackCarriedMatch != null) {
                        playerStatus.setCarriedItemFromPlayerInventory(false);
                    }
                }
                /*if (playerRemovedItems.size() > 0) {
                    playerStatus.setCarriedItemFromPlayerInventory(true);
                } else if (containerRemovedItems.size() > 0) {
                    playerStatus.setCarriedItemFromPlayerInventory(false);
                }*/
            }

            //handling moving carried to anything, also partially emptying carried to anything

            if (playerStatus.getInventorySnapshotCarried().itemStackList.size() > 0) {
                ItemStack prevCarried = playerStatus.getInventorySnapshotCarried().itemStackList.get(0);
                for (ItemStack itemStackAdded : playerAddedItems) {
                    if (ItemStack.isSameItem(prevCarried, itemStackAdded)) {
                        System.out.println("to player");
                        //prevent zipping to player if it was picked up from player inv
                        if (!playerStatus.isCarriedItemFromPlayerInventory()) {
                            sendItemMove(player, itemStackAdded, false);
                        }

                    }
                    /*ItemStack itemStackAddedMatch = getMatchingItem(itemStackAdded, containerAddedItems);
                    if (itemStackAddedMatch != null) {
                        //if (itemStackAddedMatch.getCount() >= itemStackAddedMatch.getCount()) {
                        sendItemMove(player, itemStackAdded, true);
                        //}
                    }*/
                }

                for (ItemStack itemStackAdded : containerAddedItems) {
                    if (ItemStack.isSameItem(prevCarried, itemStackAdded)) {
                        System.out.println("to container");
                        //prevent zipping to player if it was picked up from player inv
                        if (playerStatus.isCarriedItemFromPlayerInventory()) {
                            sendItemMove(player, itemStackAdded, true);
                        }
                    }
                }
            }

            /*if (playerStatus.getInventorySnapshotCarried().itemStackList.size() > 0) {
                ItemStack prevCarried = playerStatus.getInventorySnapshotCarried().itemStackList.get(0);
                ItemStack itemStackCarriedMatch = getMatchingItem(prevCarried, playerAddedItems);
                if (itemStackCarriedMatch != null) {
                    //playerStatus.setCarriedItemFromPlayerInventory(true);
                    System.out.println("to player");
                    sendItemMove(player, itemStackCarriedMatch, false);
                } else {
                    itemStackCarriedMatch = getMatchingItem(prevCarried, containerAddedItems);
                    if (itemStackCarriedMatch != null) {
                        //playerStatus.setCarriedItemFromPlayerInventory(false);
                        System.out.println("to container");
                        sendItemMove(player, itemStackCarriedMatch, true);
                    }
                }
            }*/
        } else if (pClickType == ClickType.QUICK_MOVE) {
            //compare player removed items against container added items
            for (ItemStack itemStackRemoved : playerRemovedItems) {
                ItemStack itemStackAdded = getMatchingItem(itemStackRemoved, containerAddedItems);
                if (itemStackAdded != null) {
                    if (itemStackAdded.getCount() >= itemStackRemoved.getCount()) {
                        sendItemMove(player, itemStackAdded, true);
                    }
                }
            }

            //compare container removed items against player added items
            for (ItemStack itemStackRemoved : containerRemovedItems) {
                ItemStack itemStackAdded = getMatchingItem(itemStackRemoved, playerAddedItems);
                if (itemStackAdded != null) {
                    if (itemStackAdded.getCount() >= itemStackRemoved.getCount()) {
                        sendItemMove(player, itemStackAdded, false);
                    }
                }
            }
        }


    }

    /**
     * wipe the item clean of extra data to avoid heavy packets
     * any items that use nbt to define their visual are screwed by this, evaluate the impact on this at some point
     * - potions
     */
    public ItemStack getSimpleItemStack(ItemStack itemStack) {
        return new ItemStack(itemStack.getItem(), itemStack.getCount());
    }

    public Pair<List<ItemStack>, List<ItemStack>> processInventorySnapshots(List<ItemStack> pre, List<ItemStack> post) {
        List<ItemStack> addedItems = new ArrayList<>();
        List<ItemStack> removedItems = new ArrayList<>();

        for (int i = 0; i < post.size(); i++) {
            ItemStack stackPre = pre.get(i);
            ItemStack stackPost = post.get(i);

            if (!ItemStack.isSameItem(stackPre, stackPost)) {
                if (stackPre.isEmpty() && !stackPost.isEmpty()) {
                    addedItems.add(getSimpleItemStack(stackPost));
                } else if (!stackPre.isEmpty() && stackPost.isEmpty()) {
                    removedItems.add(getSimpleItemStack(stackPre));
                }
            } else {
                if (stackPre.getCount() > stackPost.getCount()) {
                    ItemStack stack = getSimpleItemStack(stackPre);
                    stack.setCount(stackPre.getCount() - stackPost.getCount());
                    removedItems.add(stack);
                } else if (stackPre.getCount() < stackPost.getCount()) {
                    ItemStack stack = getSimpleItemStack(stackPost);
                    stack.setCount(stackPost.getCount() - stackPre.getCount());
                    addedItems.add(stack);
                }
            }
        }

        return Pair.of(addedItems, removedItems);
    }

    public ItemStack getMatchingItem(ItemStack itemStack, List<ItemStack> itemStackList) {
        for (ItemStack itemStack1 : itemStackList) {
            if (ItemStack.isSameItem(itemStack, itemStack1)) {
                return itemStack1;
            }
        }
        return null;
    }

    public void sendItemMove(Player player, ItemStack itemStack, boolean toContainer) {
        PlayerStatus playerStatus = getStatus(player);
        BlockPos pos = playerStatus.getLastBlockOpened();
        float distFromFace = 0.55F;
        //float distFromFace = -3.5F;
        Vec3 lookVec = getBodyAngle(player).scale(distFromFace);
        if (toContainer) {
            sendItemMove(player, player.level(), itemStack, player.getX() + lookVec.x, player.getY() + 1.2, player.getZ() + lookVec.z, pos.getX() + 0.5F, pos.getY() + 0.7F, pos.getZ() + 0.5F);
        } else {
            sendItemMove(player, player.level(), itemStack, pos.getX() + 0.5F, pos.getY() + 0.7F, pos.getZ() + 0.5F, player.getX() + lookVec.x, player.getY() + 1.2, player.getZ() + lookVec.z);
        }
    }
}
