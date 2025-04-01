package com.corosus.watut;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.UUID;

public class PlayerStatusManager {

    public HashMap<UUID, PlayerStatus> lookupPlayerToStatus = new HashMap<>();

    //** DEBUG VAL **/
    protected boolean singleplayerTesting = false;

    public void tickPlayer(Player player) {
        singleplayerTesting = false;
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
        return getStatus(uuid, false);
    }

    public PlayerStatus getStatus(UUID uuid, boolean local) {
        if (local) return getStatusLocal();
        PlayerStatus status = lookupPlayerToStatus.get(uuid);
        if (status == null) {
            status = new PlayerStatus(PlayerStatus.PlayerGuiState.NONE, uuid);
            lookupPlayerToStatus.put(uuid, status);
        }
        return status;
    }

    public PlayerStatus getStatusLocal() {
        return null;
    }

    public void setMouse(UUID uuid, float x, float y, boolean pressed) {
        PlayerStatus status = getStatus(uuid);
        status.setScreenPosPercentX(x);
        status.setScreenPosPercentY(y);
        status.setPressing(pressed);
    }

    public void playerLoggedIn(Player player) {

    }

    public Vec3 getBodyAngle(Player player) {
        return this.calculateViewVector(player.getXRot(), player.yBodyRot);
    }

    public Vec3 calculateViewVector(float pXRot, float pYRot) {
        float f = pXRot * ((float)Math.PI / 180F);
        float f1 = -pYRot * ((float)Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
    }

}
