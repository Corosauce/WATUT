package com.corosus.watut.client.status;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.client.animation.PlayerAnimator;
import com.corosus.watut.client.input.InputTracker;
import com.corosus.watut.client.particle.ItemTransferParticle;
import com.corosus.watut.client.particle.ParticleRegistry;
import com.corosus.watut.client.particle.SpriteInfo;
import com.corosus.watut.client.particle.WatutParticle;
import com.corosus.watut.client.screen.RenderHelper;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.config.ConfigServerSyncHelper;
import com.corosus.watut.network.WatutNetworking;
import com.corosus.watut.status.PlayerChatState;
import com.corosus.watut.status.PlayerGuiState;
import com.corosus.watut.status.PlayerStatus;
import com.ibm.icu.impl.Pair;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Gestore principale lato Client di WATUT:
 * - Aggiornamento tick di gioco e sincronizzazione
 * - Spawn e tracciamento particelle 3D sopra i giocatori
 * - Interazione con InputTracker e PlayerAnimator
 */
public class PlayerStatusClientManager {

    private final PlayerStatus selfPlayerStatus = new PlayerStatus(PlayerGuiState.NONE, null);
    private final PlayerStatus selfPlayerStatusPrev = new PlayerStatus(PlayerGuiState.NONE, null);

    public final Map<UUID, PlayerStatus> lookupPlayerToStatus = new java.util.concurrent.ConcurrentHashMap<>();
    public final Map<UUID, PlayerStatus> lookupPlayerToStatusPrev = new java.util.concurrent.ConcurrentHashMap<>();

    private final InputTracker inputTracker = new InputTracker();
    private final int armMouseTickRate = 5;
    private int steadyTickCounter = 0;
    private final int forcedSyncRate = 40;
    private Level lastLevel;
    private static Map<String, Boolean> lookupPlayersReceivedLatestGUIRender = new HashMap<>();

    public PlayerStatus getStatusLocal() {
        return selfPlayerStatus;
    }

    public PlayerStatus getStatusPrevLocal() {
        return selfPlayerStatusPrev;
    }

    public PlayerStatus getStatus(Player player) {
        if (player == null) return getStatusLocal();
        return getStatus(player.getUUID());
    }

    public PlayerStatus getStatus(UUID uuid) {
        if (uuid == null) return getStatusLocal();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            return getStatusLocal();
        }
        return lookupPlayerToStatus.computeIfAbsent(uuid, k -> new PlayerStatus(PlayerGuiState.NONE, k));
    }

    public PlayerStatus getStatusPrev(UUID uuid) {
        if (uuid == null) return getStatusPrevLocal();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            return getStatusPrevLocal();
        }
        return lookupPlayerToStatusPrev.computeIfAbsent(uuid, k -> new PlayerStatus(PlayerGuiState.NONE, k));
    }

    public void tickGame() {
        steadyTickCounter++;
        if (steadyTickCounter == Integer.MAX_VALUE) steadyTickCounter = 0;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        // Pulizia giocatori disconnessi o cambi di dimensione
        if (mc.getConnection() != null) {
            for (Iterator<Map.Entry<UUID, PlayerStatus>> it = lookupPlayerToStatus.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, PlayerStatus> entry = it.next();
                PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(entry.getKey());
                PlayerStatus playerStatus = entry.getValue();
                if (playerInfo == null) {
                    playerStatus.reset();
                    playerStatus.getScreenData().closeImage();
                    it.remove();
                    lookupPlayerToStatusPrev.remove(entry.getKey());
                } else if (level.getPlayerByUUID(entry.getKey()) == null) {
                    playerStatus.resetParticles();
                }
            }
        }

        if (lastLevel != level) {
            for (PlayerStatus status : lookupPlayerToStatus.values()) {
                status.resetParticles();
            }
            selfPlayerStatus.reset();
            selfPlayerStatusPrev.reset();
            selfPlayerStatus.getScreenData().setGameTicksSinceLastScreenSend(0);
        }
        lastLevel = level;

        // Elaborazione e upload delle texture dinamiche ricevute
        RenderHelper.guiRender();

        // Cattura sicura dello schermo locale se la GUI è aperta
        com.corosus.watut.client.screen.DynamicScreenManager.getInstance().tryCaptureCurrentScreen();

        if (RenderHelper.processor.hasProcessedBuffers()) {
            try {
                ByteBuffer result = RenderHelper.processor.getProcessedBuffer();
                if (result != null) {
                    selfPlayerStatus.getScreenData().setTexturePixelData(result);
                    sendScreenRenderData(selfPlayerStatus);
                }
            } catch (Exception ex) {
                CULog.dbg("Watut: error processing screen buffer: " + ex.getMessage());
            }
        }

        if (InputTracker.getCurrentScreen() == null) {
            selfPlayerStatus.getScreenData().setGameTicksSinceLastScreenSend(0);
        }
    }

    public boolean updateNearbyPlayerListAndCheckIfNewPlayerNear() {
        Map<String, Boolean> lookup = new HashMap<>();
        boolean newPlayer = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return false;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.position().distanceTo(new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ())) <= ConfigServerControlledSyncedToClient.distanceRequiredToShowGUIInfo) {
                lookup.put(player.getName().getString(), true);
                if (!lookupPlayersReceivedLatestGUIRender.containsKey(player.getName().getString())) {
                    newPlayer = true;
                }
            }
        }
        lookupPlayersReceivedLatestGUIRender = lookup;
        return newPlayer;
    }

    public void tickPlayer(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(player.getUUID())) {
            tickLocalPlayer(player);
        }

        tickOtherPlayer(player);
        getStatus(player.getUUID()).tick();

        PlayerStatus status = getStatus(player);
        float adjRateTyping = 0.1F;
        if (status.getTypingAmplifierSmooth() < status.getTypingAmplifier() - adjRateTyping) {
            status.setTypingAmplifierSmooth(status.getTypingAmplifierSmooth() + adjRateTyping);
        } else if (status.getTypingAmplifierSmooth() > status.getTypingAmplifier() + adjRateTyping) {
            status.setTypingAmplifierSmooth(status.getTypingAmplifierSmooth() - adjRateTyping);
        }
    }

    private void tickLocalPlayer(Player player) {
        Minecraft mc = Minecraft.getInstance();
        PlayerStatus statusLocal = getStatusLocal();
        PlayerStatus statusPrevLocal = getStatusPrevLocal();

        if (mc.player != null && mc.player.getUUID() != null && statusLocal.getUuid() == null) {
            statusLocal.setUuid(mc.player.getUUID());
            statusPrevLocal.setUuid(mc.player.getUUID());
        }

        statusPrevLocal.setPlayerGuiState(statusLocal.getPlayerGuiState());
        Screen currentScreen = InputTracker.getCurrentScreen();

        if (ConfigClient.sendActiveGui && !statusLocal.isIdle()) {
            sendGuiStatus(inputTracker.detectGuiState(currentScreen));
        } else {
            sendGuiStatus(PlayerGuiState.NONE);
        }

        String chatText = inputTracker.extractTextFromScreen(currentScreen);
        if (inputTracker.checkIfTyping(chatText, player, statusLocal, statusPrevLocal)) {
            sendChatStatus(PlayerChatState.CHAT_TYPING);
        } else if (inputTracker.isGuiFocusedOnTextBox(currentScreen)) {
            sendChatStatus(PlayerChatState.CHAT_FOCUSED);
        } else {
            sendChatStatus(PlayerChatState.NONE);
        }

        if (ConfigClient.sendMouseInfo && currentScreen != null && mc.level.getGameTime() % armMouseTickRate == 0) {
            PlayerGuiState guiState = statusLocal.getPlayerGuiState();
            if (PlayerGuiState.canPreventIdleInGui(guiState)) {
                Pair<Float, Float> pos = inputTracker.getMousePos();
                if (pos.first != statusLocal.getScreenPosPercentX() || pos.second != statusLocal.getScreenPosPercentY()) {
                    onAction();
                }
                sendMouse(pos, statusLocal.isPressing());
            }
        }

        if (statusPrevLocal.getTicksSinceLastAction() != statusLocal.getTicksSinceLastAction()) {
            statusPrevLocal.setTicksSinceLastAction(statusLocal.getTicksSinceLastAction());
        }

        if (ConfigClient.sendIdleState) {
            statusLocal.setTicksSinceLastAction(statusLocal.getTicksSinceLastAction() + 1);
            if (statusLocal.getTicksSinceLastAction() > statusLocal.getTicksToMarkPlayerIdleSyncedForClient()) {
                if (statusLocal.isIdle() != statusPrevLocal.isIdle()) {
                    sendIdle(statusLocal);
                }
            }
        } else {
            statusLocal.setTicksSinceLastAction(0);
        }

        if (!inputTracker.isWasMousePressed() && inputTracker.getMousePressedCountdown() > 0) {
            inputTracker.decrementMousePressedCountdown();
            if (inputTracker.getMousePressedCountdown() == 0) {
                sendMouse(inputTracker.getMousePos(), false);
            }
        }

        // Sincronizzazione periodica forzata
        if (steadyTickCounter % forcedSyncRate == 0) {
            sendIdle(statusLocal);
            sendGuiStatus(statusLocal.getPlayerGuiState(), true);
            sendTyping(statusLocal);
        }
    }

    public void tickOtherPlayer(Player player) {
        PlayerStatus playerStatus = getStatus(player);
        PlayerStatus playerStatusPrev = getStatusPrev(player.getUUID());

        long stableTime = steadyTickCounter;
        float sin = (float) Math.sin((stableTime / 30.0F) % 360);
        float cos = (float) Math.cos((stableTime / 30.0F) % 360);
        float idleY = (float) (2.6 + (cos * 0.03F));

        boolean idleChange = playerStatus.isIdle() != playerStatusPrev.isIdle() || playerStatus.getParticleIdle() == null;
        boolean statusChange = playerStatus.getPlayerGuiState() != playerStatusPrev.getPlayerGuiState() || playerStatus.getParticle() == null;
        boolean chatChange = playerStatus.getPlayerChatState() != playerStatusPrev.getPlayerChatState() && playerStatus.getPlayerGuiState() == PlayerGuiState.CHAT_SCREEN;

        Minecraft mc = Minecraft.getInstance();
        if ((!ConfigClient.showGuisForYourOwnPlayerIn3rdPerson || mc.options.getCameraType().isFirstPerson())
                && (getStatusLocal().getUuid() == null || java.util.Objects.equals(playerStatus.getUuid(), getStatusLocal().getUuid()))) {
            statusChange = false;
            chatChange = false;
        }

        boolean isDynamicScreenActive = playerStatus.getScreenData().isTextureReady()
                && playerStatus.getPlayerGuiState() != PlayerGuiState.NONE
                && playerStatus.getPlayerGuiState() != PlayerGuiState.CHAT_SCREEN;

        if (idleChange || !playerStatus.isIdle()) {
            if (playerStatus.getParticleIdle() != null) {
                playerStatus.getParticleIdle().remove();
                playerStatus.setParticleIdle(null);
            }
        }
        if (statusChange || chatChange || playerStatus.getPlayerGuiState() == PlayerGuiState.NONE) {
            if (playerStatus.getParticle() != null) {
                playerStatus.getParticle().remove();
                playerStatus.setParticle(null);
            }
            if (playerStatus.getPlayerGuiState() == PlayerGuiState.NONE || playerStatus.getPlayerGuiState() == PlayerGuiState.CHAT_SCREEN) {
                playerStatus.getScreenData().cleanup();
            }
        }

        // Se lo schermo olografico dinamico è attivo, rimuoviamo l'icona statica per evitare sovrapposizioni
        if (isDynamicScreenActive && playerStatus.getParticle() != null) {
            playerStatus.getParticle().remove();
            playerStatus.setParticle(null);
        }

        if (playerStatus.getParticle() != null && !playerStatus.getParticle().isAlive()) {
            playerStatus.getParticle().remove();
            playerStatus.setParticle(null);
        }
        if (playerStatus.getParticleIdle() != null && !playerStatus.getParticleIdle().isAlive()) {
            playerStatus.getParticleIdle().remove();
            playerStatus.setParticleIdle(null);
        }

        double quadSize = 0.3F + Math.sin((stableTime / 10.0F) % 360) * 0.01F;

        if (shouldAnimate(player) && !player.isInvisible()) {
            if (idleChange && ConfigClient.showIdleStatesInPlayerAboveHead && ConfigServerControlledSyncedToClient.showIdleStatesInPlayerAboveHead && playerStatus.isIdle()) {
                WatutParticle particle = new WatutParticle((ClientLevel) player.level(), player.position().x, player.position().y + idleY, player.position().z, ParticleRegistry.IDLE.getSprite(), 1.0F);
                playerStatus.setParticleIdle(particle);
                mc.particleEngine.add(particle);
                particle.setQuadSize((float) quadSize);
            }

            if ((statusChange || chatChange) && !isDynamicScreenActive) {
                WatutParticle particle = spawnStatusParticle(player, playerStatus);
                if (particle != null) {
                    playerStatus.setParticle(particle);
                    mc.particleEngine.add(particle);
                }
            }
        }

        if (playerStatus.getParticleIdle() instanceof WatutParticle idleParticle && idleParticle.isAlive()) {
            idleParticle.keepAlive();
            idleParticle.setPos(player.position().x, player.position().y + idleY, player.position().z);
            idleParticle.setPosPrev(player.position().x, player.position().y + idleY, player.position().z);
            idleParticle.setParticleSpeed(0, 0, 0);
            idleParticle.rotationYaw = -player.yBodyRot + 180;
            idleParticle.prevRotationYaw = idleParticle.rotationYaw;
            idleParticle.rotationRoll = cos * 5;
            idleParticle.prevRotationRoll = idleParticle.rotationRoll;
            idleParticle.setQuadSize(0.15F + sin * 0.03F);
            idleParticle.setAlpha(0.5F);
        }

        if (playerStatus.getParticle() instanceof WatutParticle statusParticle && statusParticle.isAlive()) {
            statusParticle.keepAlive();
            Vec3 posParticle = getParticlePosition(player);
            statusParticle.setPos(posParticle.x, posParticle.y, posParticle.z);
            statusParticle.setParticleSpeed(0, 0, 0);

            if (mc.getCameraEntity() != null) {
                double distToCamera = mc.getCameraEntity().distanceTo(player);
                double distToCameraCapped = Math.max(3F, Math.min(10F, distToCamera));
                double distToCameraCapped2 = Math.max(3F, Math.min(6F, distToCamera));
                float alpha = (float) Math.max(0.35F, 1F - (distToCameraCapped / 10F)) + 0.15F;
                float brightness = (float) Math.max(0.55F, 1F - (distToCameraCapped / 10F)) + 0.15F;
                float huh = (float) Math.max(0.0F, 1F - (distToCameraCapped2 / 6F));
                quadSize = 0.3F + ((Math.sin((stableTime / 10F) % 360) * 0.01F) * huh);
                statusParticle.setAlpha(alpha);
                statusParticle.setBrightness(brightness);
                statusParticle.setQuadSize((float) quadSize);
                statusParticle.updateLoDFromDistance((float) distToCamera);
            } else {
                statusParticle.setAlpha(0.5F);
            }

            statusParticle.rotationYaw = -player.yBodyRot;
            statusParticle.prevRotationYaw = statusParticle.rotationYaw;
            statusParticle.rotationPitch = 20;
            statusParticle.prevRotationPitch = statusParticle.rotationPitch;
        }

        playerStatusPrev.setPlayerGuiState(playerStatus.getPlayerGuiState());
        playerStatusPrev.setPlayerChatState(playerStatus.getPlayerChatState());
        if (playerStatusPrev.getTicksSinceLastAction() != playerStatus.getTicksSinceLastAction()) {
            playerStatusPrev.setTicksSinceLastAction(playerStatus.getTicksSinceLastAction());
        }
    }

    private WatutParticle spawnStatusParticle(Player player, PlayerStatus playerStatus) {
        Vec3 pos = getParticlePosition(player);
        ClientLevel level = (ClientLevel) player.level();

        if (ConfigClient.showPlayerActiveChatGui && ConfigServerControlledSyncedToClient.showPlayerActiveChatGui) {
            if (PlayerGuiState.isTypingGui(playerStatus.getPlayerGuiState())) {
                if (playerStatus.getPlayerChatState() == PlayerChatState.CHAT_FOCUSED) {
                    return new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.CHAT_IDLE.getSpriteSet(), 1.0F, false);
                } else if (playerStatus.getPlayerChatState() == PlayerChatState.CHAT_TYPING) {
                    return new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.CHAT_TYPING.getSpriteSet(), 1.0F, false);
                }
            }
        }

        if (ConfigClient.showPlayerActiveNonChatGui && ConfigServerControlledSyncedToClient.showPlayerActiveNonChatGui) {
            PlayerGuiState state = playerStatus.getPlayerGuiState();
            return switch (state) {
                case INVENTORY        -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.INVENTORY.getSpriteSet(), 1.0F, true);
                case CRAFTING         -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.CRAFTING.getSpriteSet(), 1.0F, true);
                case ESCAPE           -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.ESCAPE.getSpriteSet(), 1.0F, true);
                case CHEST            -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.CHEST.getSpriteSet(), 1.0F, true);
                case EDIT_BOOK        -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.BOOK.getSprite(), 0.7F);
                case EDIT_SIGN        -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.SIGN.getSprite(), 0.7F);
                case ENCHANTING_TABLE -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.ENCHANTING_TABLE.getSprite(), 0.7F, 176, 166);
                case ANVIL            -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.ANVIL.getSprite(), 0.7F, 176, 166);
                case BEACON           -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.BEACON.getSprite(), 0.7F, 231, 219);
                case BREWING_STAND    -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.BREWING_STAND.getSprite(), 0.7F, 176, 166);
                case DISPENSER        -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.DISPENSER.getSprite(), 0.7F, 176, 166);
                case FURNACE          -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.FURNACE.getSprite(), 0.7F, 176, 166);
                case GRINDSTONE       -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.GRINDSTONE.getSprite(), 0.7F, 176, 166);
                case HOPPER           -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.HOPPER.getSprite(), 0.7F, 176, 134);
                case HORSE            -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.HORSE.getSprite(), 0.7F, 176, 166);
                case LOOM             -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.LOOM.getSprite(), 0.7F, 176, 166);
                case VILLAGER         -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.VILLAGER.getSprite(), 0.7F, 277, 167);
                case COMMAND_BLOCK    -> new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.COMMAND_BLOCK.getSprite(), 0.7F, 308, 213);
                case MISC             -> ConfigClient.showPlayerActiveGuiIfNotExactMatch ? new WatutParticle(level, pos.x, pos.y, pos.z, ParticleRegistry.CHEST.getSpriteSet(), 1.0F, true) : null;
                default               -> null;
            };
        }

        return null;
    }

    public boolean shouldAnimate(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return player != mc.player || !mc.options.getCameraType().isFirstPerson();
    }

    public Vec3 getParticlePosition(Player player) {
        Vec3 pos = player.position();
        float distFromFace = 0.85F;
        Vec3 lookVec = PlayerAnimator.calculateViewVector(player.getXRot(), player.yBodyRot).scale(distFromFace);
        return new Vec3(pos.x + lookVec.x, pos.y + 1.2D, pos.z + lookVec.z);
    }

    public void setupRotationsHook(PlayerModel playerModel, AvatarRenderState state) {
        Player player = Minecraft.getInstance().level != null ? (Player) Minecraft.getInstance().level.getEntity(state.id) : null;
        if (player != null) {
            PlayerAnimator.setupRotations(playerModel, state, getStatus(player), false);
        }
    }

    public void setPoseTarget(UUID uuid, boolean becauseMousePress) {
        PlayerAnimator.setPoseTarget(getStatus(uuid), getStatusPrev(uuid), becauseMousePress, armMouseTickRate);
    }

    public boolean extractPingIconHook(GuiGraphicsExtractor extractor, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo) {
        if (pPlayerInfo == null || pPlayerInfo.getProfile() == null || !ConfigClient.showIdleStatesInPlayerList || !ConfigServerControlledSyncedToClient.showIdleStatesInPlayerList) return false;
        PlayerStatus playerStatus = getStatus(pPlayerInfo.getProfile().id());
        if (playerStatus.isIdle()) {
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath(WatutMod.MODID, "icon/idle"), p_282801_ + p_281809_ - 11, pY, 10, 8);
            return true;
        }
        return false;
    }

    public void renderChatTypingOverlay(GuiGraphicsExtractor extractor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;
        if (!ConfigClient.screenTypingVisible || !ConfigServerControlledSyncedToClient.screenTypingVisible) return;

        java.util.List<String> typingPlayerNames = new java.util.ArrayList<>();
        for (PlayerStatus status : lookupPlayerToStatus.values()) {
            if (status.getUuid() != null && !status.getUuid().equals(mc.player.getUUID())) {
                if (status.getPlayerChatState() == PlayerChatState.CHAT_TYPING || status.getPlayerChatState() == PlayerChatState.CHAT_FOCUSED) {
                    Player other = mc.level.getPlayerByUUID(status.getUuid());
                    if (other != null) {
                        typingPlayerNames.add(other.getName().getString());
                    }
                }
            }
        }

        if (typingPlayerNames.isEmpty()) return;

        String fullText;
        if (typingPlayerNames.size() == 1) {
            fullText = typingPlayerNames.get(0) + ConfigClient.screenTypingText;
        } else if (typingPlayerNames.size() <= 3) {
            fullText = String.join(", ", typingPlayerNames) + ConfigClient.screenTypingText;
        } else {
            fullText = ConfigClient.screenTypingMultiplePlayersText;
        }

        int x = 2 + ConfigClient.screenTypingRelativePosition_X;
        int y = mc.getWindow().getGuiScaledHeight() - 26 + ConfigClient.screenTypingRelativePosition_Y;

        extractor.text(mc.font, fullText, x, y, 0xCCCCCC, true);
    }

    public void onMouse(boolean pressedAnything) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            Screen currentScreen = InputTracker.getCurrentScreen();
            PlayerGuiState playerGuiState = getStatus(mc.player).getPlayerGuiState();
            if (ConfigClient.sendMouseInfo && PlayerGuiState.canPreventIdleInGui(playerGuiState)) {
                if (pressedAnything) {
                    inputTracker.setMousePressedCountdown(3);
                    inputTracker.setWasMousePressed(true);
                } else {
                    inputTracker.setWasMousePressed(false);
                }
                sendMouse(inputTracker.getMousePos(), inputTracker.getMousePressedCountdown() > 0);
            }

            if (currentScreen == null || (pressedAnything && PlayerGuiState.canPreventIdleInGui(playerGuiState))) {
                onAction();
            }
        }
    }

    public void onKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            Screen currentScreen = InputTracker.getCurrentScreen();
            if (currentScreen == null || inputTracker.isGuiFocusedOnTextBox(currentScreen)) {
                onAction();
            }
        }
    }

    public void onAction() {
        if (!ConfigClient.sendIdleState) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            PlayerStatus statusLocal = getStatusLocal();
            if (statusLocal.isIdle()) {
                statusLocal.setTicksSinceLastAction(0);
                sendIdle(statusLocal);
            } else {
                statusLocal.setTicksSinceLastAction(0);
            }
        }
    }

    public void sendGuiStatus(PlayerGuiState playerStatus) {
        sendGuiStatus(playerStatus, false);
    }

    public void sendGuiStatus(PlayerGuiState playerStatus, boolean force) {
        if (getStatusLocal().getPlayerGuiState() != playerStatus || force) {
            CompoundTag data = new CompoundTag();
            data.putInt(WatutNetworking.NBTDataPlayerGuiStatus, playerStatus.ordinal());
            data.putBoolean(WatutNetworking.NBTDataPlayerGuiDontSendDetailedGUIInfo, ConfigClient.dontSendDetailedGUIInfo);
            data.putBoolean(WatutNetworking.NBTDataPlayerGuiDontSendItemInfo, ConfigClient.dontSendItemInfo);
            WatutNetworking.instance().clientSendToServer(data);
        }
        getStatusLocal().setPlayerGuiState(playerStatus);
    }

    public void sendChatStatus(PlayerChatState playerStatus) {
        sendChatStatus(playerStatus, false);
    }

    public void sendChatStatus(PlayerChatState playerStatus, boolean force) {
        if (getStatusLocal().getPlayerChatState() != playerStatus || force) {
            CompoundTag data = new CompoundTag();
            data.putInt(WatutNetworking.NBTDataPlayerChatStatus, playerStatus.ordinal());
            WatutNetworking.instance().clientSendToServer(data);
        }
        getStatusLocal().setPlayerChatState(playerStatus);
    }

    public void sendMouse(Pair<Float, Float> pos, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        float x = pos.first;
        float y = pos.second;

        if (mc.level != null && mc.player != null && mc.level.getNearestPlayer(mc.player.getX(), mc.player.getY(), mc.player.getZ(), ConfigServerControlledSyncedToClient.distanceRequiredToShowGUIInfo, (entity) -> entity != mc.player) != null) {
            if (getStatusLocal().getScreenPosPercentX() != x || getStatusLocal().getScreenPosPercentY() != y || getStatusLocal().isPressing() != pressed) {
                CompoundTag data = new CompoundTag();
                data.putFloat(WatutNetworking.NBTDataPlayerMouseX, x);
                data.putFloat(WatutNetworking.NBTDataPlayerMouseY, y);
                data.putBoolean(WatutNetworking.NBTDataPlayerMousePressed, pressed);
                WatutNetworking.instance().clientSendToServer(data);
            }
        }
        getStatusLocal().setScreenPosPercentX(x);
        getStatusLocal().setScreenPosPercentY(y);
        getStatusLocal().setPressing(pressed);
    }

    public void sendScreenRenderData(PlayerStatus status) {
        if (status.getScreenData().getTexturePixelData() == null) return;

        CompoundTag data = new CompoundTag();
        int packetSizeLimit = 31000;
        int sizeByteCount = status.getScreenData().getTexturePixelData().remaining();
        byte[] inputBytes = new byte[sizeByteCount];
        status.getScreenData().getTexturePixelData().get(inputBytes);

        int uncompressedSize = 256 * 256 * 4;
        int screenWidth = status.getScreenData().getWidth();
        int screenHeight = status.getScreenData().getHeight();

        if (status.getScreenData().getTexturePixelData().limit() < packetSizeLimit) {
            data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataSize, uncompressedSize);
            data.putInt(WatutNetworking.NBTDataPlayerScreenWidth, screenWidth);
            data.putInt(WatutNetworking.NBTDataPlayerScreenHeight, screenHeight);
            data.putByteArray(WatutNetworking.NBTDataPlayerScreenCompressedPixelData, inputBytes);
            data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount, 1);
            data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex, 0);
            WatutNetworking.instance().clientSendToServer(data);
        } else {
            int packetCount = Mth.ceil((float) sizeByteCount / (float) packetSizeLimit);
            int packetBytesIndex = 0;

            for (int i = 0; i < packetCount; i++) {
                byte[] inputBytesPartial;
                if (packetBytesIndex + packetSizeLimit < sizeByteCount) {
                    inputBytesPartial = Arrays.copyOfRange(inputBytes, packetBytesIndex, packetBytesIndex + packetSizeLimit);
                } else {
                    inputBytesPartial = Arrays.copyOfRange(inputBytes, packetBytesIndex, sizeByteCount);
                }
                packetBytesIndex += inputBytesPartial.length;

                data = new CompoundTag();
                data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataSize, uncompressedSize);
                data.putInt(WatutNetworking.NBTDataPlayerScreenWidth, screenWidth);
                data.putInt(WatutNetworking.NBTDataPlayerScreenHeight, screenHeight);
                data.putByteArray(WatutNetworking.NBTDataPlayerScreenCompressedPixelData, inputBytesPartial);
                data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount, packetCount);
                data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex, i);
                WatutNetworking.instance().clientSendToServer(data);
            }
        }
        status.getScreenData().getTexturePixelData().flip();
    }

    public void sendTyping(PlayerStatus status) {
        CompoundTag data = new CompoundTag();
        data.putFloat(WatutNetworking.NBTDataPlayerTypingAmp, status.getTypingAmplifier());
        WatutNetworking.instance().clientSendToServer(data);
    }

    public void sendIdle(PlayerStatus status) {
        CompoundTag data = new CompoundTag();
        data.putInt(WatutNetworking.NBTDataPlayerIdleTicks, status.getTicksSinceLastAction());
        WatutNetworking.instance().clientSendToServer(data);
    }

    public void receiveAny(UUID uuid, CompoundTag data) {
        PlayerStatus status = getStatus(uuid);
        PlayerStatus statusPrev = getStatusPrev(uuid);

        if (data.contains(WatutNetworking.NBTDataPlayerTypingAmp)) {
            status.setTypingAmplifier(data.getFloatOr(WatutNetworking.NBTDataPlayerTypingAmp, 0f));
        }

        if (data.contains(WatutNetworking.NBTDataPlayerMouseX)) {
            float x = data.getFloatOr(WatutNetworking.NBTDataPlayerMouseX, 0f);
            float y = data.getFloatOr(WatutNetworking.NBTDataPlayerMouseY, 0f);
            boolean pressed = data.getBooleanOr(WatutNetworking.NBTDataPlayerMousePressed, false);
            boolean differentPress = status.isPressing() != pressed;
            status.setScreenPosPercentX(x);
            status.setScreenPosPercentY(y);
            status.setPressing(pressed);
            setPoseTarget(uuid, differentPress);

            if (pressed && differentPress) {
                Player player = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getPlayerByUUID(uuid) : null;
                if (player != null && ConfigClient.playMouseClickSounds && ConfigServerControlledSyncedToClient.playMouseClickSounds && player != Minecraft.getInstance().player) {
                    player.level().playLocalSound(player.getOnPos(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.05F, 0.1F, false);
                }
            }
        }

        if (data.contains(WatutNetworking.NBTDataPlayerGuiStatus)) {
            PlayerGuiState playerGuiState = PlayerGuiState.get(data.getIntOr(WatutNetworking.NBTDataPlayerGuiStatus, 0));
            status.setPlayerGuiState(playerGuiState);
            if (data.contains(WatutNetworking.NBTDataPlayerGuiDontSendDetailedGUIInfo)) status.setPlayerGuiDontSendDetailedGUIInfo(data.getBooleanOr(WatutNetworking.NBTDataPlayerGuiDontSendDetailedGUIInfo, false));
            if (data.contains(WatutNetworking.NBTDataPlayerGuiDontSendItemInfo)) status.setPlayerGuiDontSendItemInfo(data.getBooleanOr(WatutNetworking.NBTDataPlayerGuiDontSendItemInfo, false));

            if (status.getPlayerGuiState() != statusPrev.getPlayerGuiState()) {
                if (statusPrev.getPlayerGuiState() == PlayerGuiState.NONE) {
                    status.setLerpTarget(new com.corosus.watut.client.animation.Lerpables());
                }
                setPoseTarget(uuid, false);
                Player player = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getPlayerByUUID(uuid) : null;
                if (player != null && ConfigClient.playScreenOpenSounds && ConfigServerControlledSyncedToClient.playScreenOpenSounds && player != Minecraft.getInstance().player) {
                    PlayerGuiState prev = statusPrev.getPlayerGuiState();
                    if (PlayerGuiState.isSoundMakerGui(playerGuiState) || PlayerGuiState.isSoundMakerGui(prev) || playerGuiState == PlayerGuiState.INVENTORY || playerGuiState == PlayerGuiState.CRAFTING || playerGuiState == PlayerGuiState.MISC
                            || prev == PlayerGuiState.INVENTORY || prev == PlayerGuiState.CRAFTING || prev == PlayerGuiState.MISC) {
                        player.level().playLocalSound(player.getOnPos(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.9F, 1.0F, false);
                    }
                }
            }
        }

        if (data.contains(WatutNetworking.NBTDataPlayerChatStatus)) {
            PlayerChatState state = PlayerChatState.get(data.getIntOr(WatutNetworking.NBTDataPlayerChatStatus, 0));
            status.setPlayerChatState(state);
            if (status.getPlayerChatState() != statusPrev.getPlayerChatState()) {
                if (statusPrev.getPlayerChatState() == PlayerChatState.NONE) {
                    status.setLerpTarget(new com.corosus.watut.client.animation.Lerpables());
                }
                if (status.getPlayerChatState() == PlayerChatState.CHAT_FOCUSED) {
                    status.setTypingAmplifier(1.0F);
                    status.setTypingAmplifierSmooth(1.0F);
                }
                setPoseTarget(uuid, false);
            }
        }

        if (data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) {
            status.setTicksSinceLastAction(data.getIntOr(WatutNetworking.NBTDataPlayerIdleTicks, 0));
            int ticksIdle = data.getIntOr(WatutNetworking.NBTDataPlayerTicksToGoIdle, 0);
            status.setTicksToMarkPlayerIdleSyncedForClient(ticksIdle);
            statusPrev.setTicksToMarkPlayerIdleSyncedForClient(ticksIdle);
            getStatusLocal().setTicksToMarkPlayerIdleSyncedForClient(ticksIdle);
            getStatusPrevLocal().setTicksToMarkPlayerIdleSyncedForClient(ticksIdle);
            if (statusPrev.isIdle() != status.isIdle()) {
                setPoseTarget(uuid, false);
            }
        }

        if (data.contains(WatutNetworking.NBTDataPlayerScreenCompressedPixelData)) {
            byte[] pixelData = data.getByteArray(WatutNetworking.NBTDataPlayerScreenCompressedPixelData).orElse(new byte[0]);
            int decompressedSize = data.getIntOr(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataSize, 0);
            int packetCount = data.getIntOr(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount, 0);
            int packetIndex = data.getIntOr(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex, 0);
            status.getScreenData().setWidth(data.getIntOr(WatutNetworking.NBTDataPlayerScreenWidth, 256));
            status.getScreenData().setHeight(data.getIntOr(WatutNetworking.NBTDataPlayerScreenHeight, 256));
            long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
            int timeout = 10;

            if (packetCount > 1) {
                if (packetIndex == 0) {
                    status.getScreenData().setGameTicksSinceFirstPacket(gameTime);
                    status.getScreenData().setLastIndexReceived(0);
                    status.getScreenData().setTexturePixelDataPartial(pixelData);
                } else if (status.getScreenData().getTexturePixelDataPartial() != null
                        && packetIndex == status.getScreenData().getLastIndexReceived() + 1
                        && gameTime <= status.getScreenData().getGameTicksSinceFirstPacket() + timeout) {
                    status.getScreenData().setLastIndexReceived(packetIndex);
                    if (pixelData.length > 0) {
                        byte[] dataBytes = status.getScreenData().getTexturePixelDataPartial();
                        byte[] combined = new byte[dataBytes.length + pixelData.length];
                        System.arraycopy(dataBytes, 0, combined, 0, dataBytes.length);
                        System.arraycopy(pixelData, 0, combined, dataBytes.length, pixelData.length);
                        status.getScreenData().setTexturePixelDataPartial(combined);

                        if (packetIndex == packetCount - 1) {
                            try {
                                status.getScreenData().setTexturePixelData(RenderHelper.decompress(status.getScreenData(), ByteBuffer.wrap(status.getScreenData().getTexturePixelDataPartial()), decompressedSize));
                                status.getScreenData().markNeedsNewRenderFromPixelData(true);
                                status.getScreenData().getIsBufferReady().set(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                status.getScreenData().setTexturePixelDataPartial(null);
                            }
                        }
                    }
                } else {
                    // Reset on out-of-order or timed-out packets
                    status.getScreenData().setTexturePixelDataPartial(null);
                    status.getScreenData().setLastIndexReceived(-1);
                }
            } else {
                try {
                    status.getScreenData().setTexturePixelData(RenderHelper.decompress(status.getScreenData(), ByteBuffer.wrap(pixelData), decompressedSize));
                    status.getScreenData().markNeedsNewRenderFromPixelData(true);
                    status.getScreenData().getIsBufferReady().set(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void receiveItemMove(CompoundTag data) {
        if (data.contains(WatutNetworking.NBTDataItemTransferItemStack) && Minecraft.getInstance().level != null) {
            ItemStack itemStack = ItemStack.OPTIONAL_CODEC.parse(Minecraft.getInstance().level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), data.get(WatutNetworking.NBTDataItemTransferItemStack)).result().orElse(ItemStack.EMPTY);
            ItemTransferParticle particle = new ItemTransferParticle(
                    Minecraft.getInstance().level, 1.0F, itemStack,
                    data.getFloatOr(WatutNetworking.NBTDataItemTransferFromX, 0f),
                    data.getFloatOr(WatutNetworking.NBTDataItemTransferFromY, 0f),
                    data.getFloatOr(WatutNetworking.NBTDataItemTransferFromZ, 0f),
                    data.getFloatOr(WatutNetworking.NBTDataItemTransferToX, 0f),
                    data.getFloatOr(WatutNetworking.NBTDataItemTransferToY, 0f),
                    data.getFloatOr(WatutNetworking.NBTDataItemTransferToZ, 0f)
            );
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }

    public void receiveServerConfig(CompoundTag nbt) {
        CULog.dbg("receiving server config sync");
        ConfigServerSyncHelper.getInstance().updateSyncableConfigOnClient(nbt);
    }
}
