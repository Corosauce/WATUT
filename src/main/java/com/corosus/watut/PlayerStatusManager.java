package com.corosus.watut;

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

    public PlayerStatus getStatus(Player player) {
        return getStatus(player.getUUID());
    }

    public PlayerStatus getStatus(UUID uuid) {
        PlayerStatus status = lookupPlayerToStatus.get(uuid);
        if (status == null) {
            status = new PlayerStatus(PlayerStatus.PlayerGuiState.NONE);
            lookupPlayerToStatus.put(uuid, status);
        }
        return status;
    }

    public void setGuiStatus(UUID uuid, PlayerStatus.PlayerGuiState statusType) {
        getStatus(uuid).setPlayerGuiState(statusType);
    }

    public void setMouse(UUID uuid, float x, float y, boolean pressed) {
        PlayerStatus status = getStatus(uuid);
        status.setScreenPosPercentX(x);
        status.setScreenPosPercentY(y);
        status.setPressing(pressed);
    }

}
