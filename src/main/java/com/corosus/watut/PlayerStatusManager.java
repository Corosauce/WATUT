package com.corosus.watut;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.UUID;

public class PlayerStatusManager {

    public HashMap<UUID, PlayerStatus> lookupPlayerToStatus = new HashMap<>();

    public void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            tickPlayerClient(player);
        }
    }

    public void tickPlayerClient(Player player) {
    }

    public void setStatus(Player player, PlayerStatus statusType) {
        setStatus(player.getUUID(), statusType);
    }

    public void setStatus(UUID uuid, PlayerStatus statusType) {
        lookupPlayerToStatus.put(uuid, statusType);
    }

}
