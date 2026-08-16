package com.corosus.watut.status;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot dell'inventario per tracciare lo spostamento di item tra giocatore e contenitori.
 */
public class InventorySnapshot {
    public final List<ItemStack> itemStackList = new ArrayList<>();

    public void clear() {
        itemStackList.clear();
    }
}
