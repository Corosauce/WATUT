package com.corosus.watut;

import net.minecraft.client.particle.Particle;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class PlayerStatus {

    public enum PlayerGuiState {

        NONE,
        CHAT_OPEN,
        CHAT_TYPING,
        INVENTORY,
        CRAFTING,
        MISC;

        private static final Map<Integer, PlayerGuiState> lookup = new HashMap<>();

        static {
            for (PlayerGuiState e : EnumSet.allOf(PlayerGuiState.class)) {
                lookup.put(e.ordinal(), e);
            }
        }

        public static PlayerGuiState get(int intValue) {
            return lookup.get(intValue);
        }
    }

    private PlayerGuiState playerGuiState;
    private Particle particle;
    private long lastTypeTime;
    private String lastTypeString;
    private float screenPosPercentX = 0;
    private float screenPosPercentY = 0;

    public PlayerStatus(PlayerGuiState playerGuiState) {
        this.playerGuiState = playerGuiState;
    }

    public PlayerGuiState getPlayerGuiState() {
        return playerGuiState;
    }

    public void setPlayerGuiState(PlayerGuiState playerGuiState) {
        this.playerGuiState = playerGuiState;
    }

    public Particle getParticle() {
        return particle;
    }

    public void setParticle(Particle particle) {
        this.particle = particle;
    }

    public long getLastTypeTime() {
        return lastTypeTime;
    }

    public void setLastTypeTime(long lastTypeTime) {
        this.lastTypeTime = lastTypeTime;
    }

    public String getLastTypeString() {
        return lastTypeString;
    }

    public void setLastTypeString(String lastTypeString) {
        this.lastTypeString = lastTypeString;
    }

    public float getScreenPosPercentX() {
        return screenPosPercentX;
    }

    public void setScreenPosPercentX(float screenPosPercentX) {
        this.screenPosPercentX = screenPosPercentX;
    }

    public float getScreenPosPercentY() {
        return screenPosPercentY;
    }

    public void setScreenPosPercentY(float screenPosPercentY) {
        this.screenPosPercentY = screenPosPercentY;
    }
}
