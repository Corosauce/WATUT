package com.corosus.watut.server;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.client.animation.PlayerAnimator;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.network.WatutNetworking;
import com.corosus.watut.status.FakePlayerHelper;
import com.corosus.watut.status.InventorySnapshot;
import com.corosus.watut.status.PlayerStatus;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Rileva il trasferimento di oggetti tra il giocatore e i contenitori durante i click dell'inventario.
 */
public class ItemTransferDetector {

    public static void makeNewInventorySnapshot(AbstractContainerMenu menu, Player player, PlayerStatus status) {
        status.getInventorySnapshotPlayer().clear();
        status.getInventorySnapshotContainer().clear();
        status.getInventorySnapshotCarried().clear();

        for (ItemStack item : player.getInventory().getNonEquipmentItems()) {
            status.getInventorySnapshotPlayer().itemStackList.add(item.copy());
        }
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) {
                status.getInventorySnapshotContainer().itemStackList.add(slot.getItem().copy());
            }
        }
        if (!menu.getCarried().isEmpty()) {
            status.getInventorySnapshotCarried().itemStackList.add(menu.getCarried().copy());
        }
    }

    public static void doClickPre(AbstractContainerMenu menu, int slotId, int button, ContainerInput clickType, Player player, PlayerStatus status) {
        if (!ConfigServerControlledSyncedToClient.showItemsBeingTransferredBetweenPlayerAndContainer) return;
        if (FakePlayerHelper.isFakePlayer(player)) return;
        if (status.isPlayerGuiDontSendItemInfo() || status.getLastBlockOpened().equals(BlockPos.ZERO)) return;

        makeNewInventorySnapshot(menu, player, status);
    }

    public static void useBlock(Player player, BlockPos pos, PlayerStatus status) {
        if (!ConfigServerControlledSyncedToClient.showItemsBeingTransferredBetweenPlayerAndContainer) return;
        if (FakePlayerHelper.isFakePlayer(player)) return;
        if (status.isPlayerGuiDontSendItemInfo()) return;
        status.setLastBlockOpened(pos);
    }

    public static void doClickPost(AbstractContainerMenu menu, int slotId, int button, ContainerInput clickType, Player player, PlayerStatus status) {
        if (!ConfigServerControlledSyncedToClient.showItemsBeingTransferredBetweenPlayerAndContainer) return;
        if (FakePlayerHelper.isFakePlayer(player)) return;
        if (status.isPlayerGuiDontSendItemInfo() || status.getLastBlockOpened().equals(BlockPos.ZERO)) return;

        InventorySnapshot postPlayer = new InventorySnapshot();
        InventorySnapshot postContainer = new InventorySnapshot();
        InventorySnapshot postCarried = new InventorySnapshot();

        for (ItemStack item : player.getInventory().getNonEquipmentItems()) {
            postPlayer.itemStackList.add(item.copy());
        }
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) {
                postContainer.itemStackList.add(slot.getItem().copy());
            }
        }
        if (!menu.getCarried().isEmpty()) {
            postCarried.itemStackList.add(menu.getCarried().copy());
        }

        Pair<List<ItemStack>, List<ItemStack>> playerDiff = processInventorySnapshots(status.getInventorySnapshotPlayer().itemStackList, postPlayer.itemStackList);
        List<ItemStack> playerAdded = playerDiff.getFirst();
        List<ItemStack> playerRemoved = playerDiff.getSecond();

        Pair<List<ItemStack>, List<ItemStack>> containerDiff = processInventorySnapshots(status.getInventorySnapshotContainer().itemStackList, postContainer.itemStackList);
        List<ItemStack> containerAdded = containerDiff.getFirst();
        List<ItemStack> containerRemoved = containerDiff.getSecond();

        if (clickType == ContainerInput.PICKUP) {
            if (!menu.getCarried().isEmpty() && playerAdded.isEmpty() && containerAdded.isEmpty()) {
                ItemStack match = getMatchingItem(menu.getCarried(), playerRemoved);
                if (match != null) {
                    status.setCarriedItemFromPlayerInventory(true);
                } else {
                    match = getMatchingItem(menu.getCarried(), containerRemoved);
                    if (match != null) {
                        status.setCarriedItemFromPlayerInventory(false);
                    }
                }
            }

            if (!status.getInventorySnapshotCarried().itemStackList.isEmpty()) {
                ItemStack prevCarried = status.getInventorySnapshotCarried().itemStackList.get(0);
                for (ItemStack added : playerAdded) {
                    if (ItemStack.isSameItem(prevCarried, added) && !status.isCarriedItemFromPlayerInventory()) {
                        sendItemMove(player, added, false, status);
                    }
                }
                for (ItemStack added : containerAdded) {
                    if (ItemStack.isSameItem(prevCarried, added) && status.isCarriedItemFromPlayerInventory()) {
                        sendItemMove(player, added, true, status);
                    }
                }
            }
        } else if (clickType == ContainerInput.QUICK_MOVE) {
            for (ItemStack removed : playerRemoved) {
                ItemStack added = getMatchingItem(removed, containerAdded);
                if (added != null && added.getCount() >= removed.getCount()) {
                    sendItemMove(player, added, true, status);
                }
            }
            for (ItemStack removed : containerRemoved) {
                ItemStack added = getMatchingItem(removed, playerAdded);
                if (added != null && added.getCount() >= removed.getCount()) {
                    sendItemMove(player, added, false, status);
                }
            }
        }
    }

    public static Pair<List<ItemStack>, List<ItemStack>> processInventorySnapshots(List<ItemStack> pre, List<ItemStack> post) {
        List<ItemStack> addedItems = new ArrayList<>();
        List<ItemStack> removedItems = new ArrayList<>();

        int minSize = Math.min(pre.size(), post.size());
        for (int i = 0; i < minSize; i++) {
            ItemStack stackPre = pre.get(i);
            ItemStack stackPost = post.get(i);

            if (!ItemStack.isSameItem(stackPre, stackPost)) {
                if (stackPre.isEmpty() && !stackPost.isEmpty()) {
                    addedItems.add(stackPost.copy());
                } else if (!stackPre.isEmpty() && stackPost.isEmpty()) {
                    removedItems.add(stackPre.copy());
                }
            } else {
                if (stackPre.getCount() > stackPost.getCount()) {
                    ItemStack stack = stackPre.copy();
                    stack.setCount(stackPre.getCount() - stackPost.getCount());
                    removedItems.add(stack);
                } else if (stackPre.getCount() < stackPost.getCount()) {
                    ItemStack stack = stackPost.copy();
                    stack.setCount(stackPost.getCount() - stackPre.getCount());
                    addedItems.add(stack);
                }
            }
        }

        if (post.size() > minSize) {
            for (int i = minSize; i < post.size(); i++) {
                ItemStack stack = post.get(i);
                if (!stack.isEmpty()) addedItems.add(stack.copy());
            }
        } else if (pre.size() > minSize) {
            for (int i = minSize; i < pre.size(); i++) {
                ItemStack stack = pre.get(i);
                if (!stack.isEmpty()) removedItems.add(stack.copy());
            }
        }

        return Pair.of(addedItems, removedItems);
    }

    public static ItemStack getMatchingItem(ItemStack target, List<ItemStack> list) {
        for (ItemStack stack : list) {
            if (ItemStack.isSameItem(target, stack)) {
                return stack;
            }
        }
        return null;
    }

    public static void sendItemMove(Player player, ItemStack itemStack, boolean toContainer, PlayerStatus status) {
        BlockPos pos = status.getLastBlockOpened();
        float distFromFace = 0.55F;
        Vec3 lookVec = PlayerAnimator.calculateViewVector(player.getXRot(), player.yBodyRot).scale(distFromFace);

        if (toContainer) {
            sendItemMove(player, player.level(), itemStack, player.getX() + lookVec.x, player.getY() + 1.2, player.getZ() + lookVec.z, pos.getX() + 0.5F, pos.getY() + 0.7F, pos.getZ() + 0.5F);
        } else {
            sendItemMove(player, player.level(), itemStack, pos.getX() + 0.5F, pos.getY() + 0.7F, pos.getZ() + 0.5F, player.getX() + lookVec.x, player.getY() + 1.2, player.getZ() + lookVec.z);
        }
    }

    public static void sendItemMove(Player player, Level level, ItemStack itemStack, double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        if (level.getNearestPlayer(fromX, fromY, fromZ, ConfigServerControlledSyncedToClient.distanceRequiredToShowGUIInfo, (entity) -> entity != player) != null) {
            CompoundTag data = new CompoundTag();
            Tag itemData = ItemStack.OPTIONAL_CODEC.encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), itemStack).getOrThrow();
            if (itemData.sizeInBytes() > 31000) {
                CULog.dbg("itemstack too large for sending, using simple version");
                itemStack = new ItemStack(itemStack.getItem(), itemStack.getCount());
                itemData = ItemStack.OPTIONAL_CODEC.encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), itemStack).getOrThrow();
            }
            data.put(WatutNetworking.NBTDataItemTransferItemStack, itemData);
            data.putFloat(WatutNetworking.NBTDataItemTransferFromX, (float) fromX);
            data.putFloat(WatutNetworking.NBTDataItemTransferFromY, (float) fromY);
            data.putFloat(WatutNetworking.NBTDataItemTransferFromZ, (float) fromZ);
            data.putFloat(WatutNetworking.NBTDataItemTransferToX, (float) toX);
            data.putFloat(WatutNetworking.NBTDataItemTransferToY, (float) toY);
            data.putFloat(WatutNetworking.NBTDataItemTransferToZ, (float) toZ);

            WatutNetworking.instance().serverSendToClientNear(data, new Vec3(fromX, fromY, fromZ), ConfigServerControlledSyncedToClient.distanceRequiredToShowGUIInfo, level);
        }
    }
}
