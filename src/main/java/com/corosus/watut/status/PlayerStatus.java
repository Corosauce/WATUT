package com.corosus.watut.status;

import com.corosus.watut.client.animation.Lerpables;
import com.corosus.watut.client.screen.ScreenData;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Mantiene lo stato di visualizzazione e animazione di un singolo giocatore.
 */
public class PlayerStatus {

    // Valori sincronizzati via rete
    private PlayerGuiState playerGuiState = PlayerGuiState.NONE;
    private boolean playerGuiDontSendDetailedGUIInfo;
    private boolean playerGuiDontSendItemInfo;
    private PlayerChatState playerChatState = PlayerChatState.NONE;
    private float typingAmplifier = 1.0F;
    private float typingAmplifierSmooth = 0.5F;
    private float screenPosPercentX = 0.0F;
    private float screenPosPercentY = 0.0F;
    private boolean isPressing = false;
    private int ticksSinceLastAction = 0;
    private int ticksToMarkPlayerIdleSyncedForClient = 20 * 60 * 5;

    // Particelle (lato client)
    private Particle particle;
    private Particle particleIdle;

    // Tracciamento digitazione (lato client locale)
    private long lastTypeTime;
    private String lastTypeString = "";
    private long lastTypeTimeForAmp;
    private String lastTypeStringForAmp = "";
    private int lastTypeDiff;

    // Interpolazione scheletrica del modello 3D (lerp)
    private Lerpables lerpTarget = new Lerpables();
    private Lerpables lerpPrev = new Lerpables();
    public float lerpTicks = 0;
    public float lerpTicksPrev = 0;
    public float lerpTicksMax = 5;
    public float lastPartialTick = 0;

    public float yRotHeadWhileOverriding = 0;
    public float xRotHeadWhileOverriding = 0;
    public float yRotHeadBeforeOverriding = 0;
    public float xRotHeadBeforeOverriding = 0;

    private final CompoundTag nbtCache = new CompoundTag();
    private ScreenData screenData;
    private BlockPos lastBlockOpened = BlockPos.ZERO;

    // Snapshot inventario per tracciamento particelle item transfer
    private InventorySnapshot inventorySnapshotPlayer = new InventorySnapshot();
    private InventorySnapshot inventorySnapshotContainer = new InventorySnapshot();
    private InventorySnapshot inventorySnapshotCarried = new InventorySnapshot();
    private boolean isCarriedItemFromPlayerInventory = false;

    private UUID uuid;

    public PlayerStatus(PlayerGuiState playerGuiState, UUID uuid) {
        this.playerGuiState = playerGuiState != null ? playerGuiState : PlayerGuiState.NONE;
        this.uuid = uuid;
    }

    public void tick() {
        this.lerpTicksPrev = lerpTicks;
        if (isLerping()) {
            this.lerpTicks++;
        }
    }

    public void setNewLerp(float ticks) {
        this.lerpTicksMax = ticks;
        this.lerpTicks = 0;
        this.lerpTicksPrev = 0;
    }

    public float getPartialLerp(float partialTick) {
        float prev = (lerpTicksPrev / lerpTicksMax);
        float current = (lerpTicks / lerpTicksMax);
        return Math.min(prev + ((current - prev) * partialTick), lerpTicksMax);
    }

    public void resetParticles() {
        if (particle != null) {
            particle.remove();
            particle = null;
        }
        if (particleIdle != null) {
            particleIdle.remove();
            particleIdle = null;
        }
    }

    public void reset() {
        resetParticles();
        ticksSinceLastAction = 0;
    }

    public boolean isLerping() {
        return this.lerpTicks < this.lerpTicksMax;
    }

    public boolean isIdle() {
        return ticksSinceLastAction > ticksToMarkPlayerIdleSyncedForClient;
    }

    // --- Getters & Setters ---

    public PlayerGuiState getPlayerGuiState() {
        return playerGuiState;
    }

    public void setPlayerGuiState(PlayerGuiState playerGuiState) {
        this.playerGuiState = playerGuiState != null ? playerGuiState : PlayerGuiState.NONE;
    }

    public boolean isPlayerGuiDontSendDetailedGUIInfo() {
        return playerGuiDontSendDetailedGUIInfo;
    }

    public void setPlayerGuiDontSendDetailedGUIInfo(boolean val) {
        this.playerGuiDontSendDetailedGUIInfo = val;
    }

    public boolean isPlayerGuiDontSendItemInfo() {
        return playerGuiDontSendItemInfo;
    }

    public void setPlayerGuiDontSendItemInfo(boolean val) {
        this.playerGuiDontSendItemInfo = val;
    }

    public PlayerChatState getPlayerChatState() {
        return playerChatState;
    }

    public void setPlayerChatState(PlayerChatState playerChatState) {
        this.playerChatState = playerChatState != null ? playerChatState : PlayerChatState.NONE;
    }

    public float getTypingAmplifier() {
        return typingAmplifier;
    }

    public void setTypingAmplifier(float typingAmplifier) {
        this.typingAmplifier = typingAmplifier;
    }

    public float getTypingAmplifierSmooth() {
        return typingAmplifierSmooth;
    }

    public void setTypingAmplifierSmooth(float typingAmplifierSmooth) {
        this.typingAmplifierSmooth = typingAmplifierSmooth;
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

    public boolean isPressing() {
        return isPressing;
    }

    public void setPressing(boolean pressing) {
        isPressing = pressing;
    }

    public int getTicksSinceLastAction() {
        return ticksSinceLastAction;
    }

    public void setTicksSinceLastAction(int ticksSinceLastAction) {
        this.ticksSinceLastAction = ticksSinceLastAction;
    }

    public int getTicksToMarkPlayerIdleSyncedForClient() {
        return ticksToMarkPlayerIdleSyncedForClient;
    }

    public void setTicksToMarkPlayerIdleSyncedForClient(int val) {
        this.ticksToMarkPlayerIdleSyncedForClient = val;
    }

    public Particle getParticle() {
        return particle;
    }

    public void setParticle(Particle particle) {
        this.particle = particle;
    }

    public Particle getParticleIdle() {
        return particleIdle;
    }

    public void setParticleIdle(Particle particleIdle) {
        this.particleIdle = particleIdle;
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

    public int getLastTypeDiff() {
        return lastTypeDiff;
    }

    public void setLastTypeDiff(int lastTypeDiff) {
        this.lastTypeDiff = lastTypeDiff;
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

    public CompoundTag getNbtCache() {
        return nbtCache;
    }

    public ScreenData getScreenData() {
        if (screenData == null) {
            screenData = new ScreenData();
        }
        return screenData;
    }

    public void setScreenData(ScreenData screenData) {
        this.screenData = screenData;
    }

    public BlockPos getLastBlockOpened() {
        return lastBlockOpened;
    }

    public void setLastBlockOpened(BlockPos lastBlockOpened) {
        this.lastBlockOpened = lastBlockOpened;
    }

    public InventorySnapshot getInventorySnapshotPlayer() {
        return inventorySnapshotPlayer;
    }

    public void setInventorySnapshotPlayer(InventorySnapshot snapshot) {
        this.inventorySnapshotPlayer = snapshot;
    }

    public InventorySnapshot getInventorySnapshotContainer() {
        return inventorySnapshotContainer;
    }

    public void setInventorySnapshotContainer(InventorySnapshot snapshot) {
        this.inventorySnapshotContainer = snapshot;
    }

    public InventorySnapshot getInventorySnapshotCarried() {
        return inventorySnapshotCarried;
    }

    public void setInventorySnapshotCarried(InventorySnapshot snapshot) {
        this.inventorySnapshotCarried = snapshot;
    }

    public boolean isCarriedItemFromPlayerInventory() {
        return isCarriedItemFromPlayerInventory;
    }

    public void setCarriedItemFromPlayerInventory(boolean val) {
        this.isCarriedItemFromPlayerInventory = val;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
