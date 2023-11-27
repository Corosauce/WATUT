package com.corosus.watut;

import com.corosus.watut.math.Lerpables;
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

    //synced values
    private PlayerGuiState playerGuiState;
    private float typingAmplifier = 1F;
    private float screenPosPercentX = 0;
    private float screenPosPercentY = 0;
    private boolean isPressing = false;

    //misc values used on either transmitting client or receiving client
    private Particle particle;
    private long lastTypeTime;
    private String lastTypeString = "";

    private long lastTypeTimeForAmp;
    private String lastTypeStringForAmp = "";
    private int lastTypeDiff;
    //so we can orient the particle to the bodys orientation
    //private ModelPart body;
    private Lerpables lerpTarget = new Lerpables();
    private Lerpables lerpPrev = new Lerpables();

    public float lerpTicks = 0;
    //for partial ticks
    public float lerpTicksPrev = 0;
    public float lerpTicksMax = 5;

    public float yRotHeadWhileOverriding = 0;
    public float xRotHeadWhileOverriding = 0;

    public float yRotHeadBeforeOverriding = 0;
    public float xRotHeadBeforeOverriding = 0;

    public PlayerStatus(PlayerGuiState playerGuiState) {
        this.playerGuiState = playerGuiState;
    }

    public void tick() {
        this.lerpTicksPrev = lerpTicks;
        if (isLerping()) {
            this.lerpTicks++;
        }
    }

    public void setNewLerp(float ticks) {
        lerpTicksMax = ticks;
        lerpTicks = 0;
        lerpTicksPrev = 0;
    }

    public float getPartialLerp(float partialTick) {
        float lerpPrev = (lerpTicksPrev / lerpTicksMax);
        float lerp = (lerpTicks / lerpTicksMax);
        return Math.min(lerpPrev + ((lerp - lerpPrev) * partialTick), lerpTicksMax);
    }

    public boolean isLerping() {
        return this.lerpTicks < this.lerpTicksMax;
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

    public Lerpables getLerpTarget() {
        return lerpTarget;
    }

    public void setLerpTarget(Lerpables lerpTarget) {
        this.lerpTarget = lerpTarget;
    }

    public Lerpables getLerpPrev() {
        return lerpPrev;
    }

    public void setLerpPrev(Lerpables lerpPrev) {
        this.lerpPrev = lerpPrev;
    }

    public boolean isPressing() {
        return isPressing;
    }

    public void setPressing(boolean pressing) {
        isPressing = pressing;
    }

    public float getTypingAmplifier() {
        return typingAmplifier;
    }

    public void setTypingAmplifier(float typingAmplifier) {
        this.typingAmplifier = typingAmplifier;
    }

    public int getLastTypeDiff() {
        return lastTypeDiff;
    }

    public void setLastTypeDiff(int lastTypeDiff) {
        this.lastTypeDiff = lastTypeDiff;
    }

    public long getLastTypeTimeForAmp() {
        return lastTypeTimeForAmp;
    }

    public void setLastTypeTimeForAmp(long lastTypeTimeForAmp) {
        this.lastTypeTimeForAmp = lastTypeTimeForAmp;
    }

    public String getLastTypeStringForAmp() {
        return lastTypeStringForAmp;
    }

    public void setLastTypeStringForAmp(String lastTypeStringForAmp) {
        this.lastTypeStringForAmp = lastTypeStringForAmp;
    }
}
