package com.corosus.watut;

import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.math.Lerpables;
import com.corosus.watut.particle.ParticleAnimated;
import com.corosus.watut.particle.ParticleRotating;
import com.corosus.watut.particle.ParticleStatic;
import com.corosus.watut.particle.ParticleStaticLoD;
import com.ibm.icu.impl.Pair;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PlayerStatusManagerClient extends PlayerStatusManager {

    //selfPlayer statuses are important, a use case: tracking things to send packets for locally,
    //then we allow for packet to be sent to self as well, where we also use the lookup to then compare previous state so we can correctly setup pose for self as well as others
    //local use case:
    //idle state tracking and comparison when it changes from selfPlayer previous
    //remote use case:
    //idle state pose setup when it changes from NON selfPlayer previous, setting up lerp
    private PlayerStatus selfPlayerStatus = new PlayerStatus(PlayerStatus.PlayerGuiState.NONE);
    private PlayerStatus selfPlayerStatusPrev = new PlayerStatus(PlayerStatus.PlayerGuiState.NONE);

    public HashMap<UUID, PlayerStatus> lookupPlayerToStatusPrev = new HashMap<>();
    private long typingIdleTimeout = 60;
    private int armMouseTickRate = 5;
    private int typeRatePollCounter = 0;

    //using gametime is buggy on client, gets synced, skips ticks, etc, this is better
    private int steadyTickCounter = 0;
    private int forcedSyncRate = 40;

    private Level lastLevel;

    public void tickGame(TickEvent.ClientTickEvent event) {
        steadyTickCounter++;
        if (steadyTickCounter == Integer.MAX_VALUE) steadyTickCounter = 0;
        if (Minecraft.getInstance().getConnection() != null) {
            for (Iterator<Map.Entry<UUID, PlayerStatus>> it = lookupPlayerToStatus.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, PlayerStatus> entry = it.next();
                PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(entry.getKey());
                PlayerStatus playerStatus = entry.getValue();
                if (playerInfo == null) {
                    WatutMod.dbg("remove playerstatus for no longer existing player: " + entry.getKey());
                    playerStatus.reset();
                    it.remove();
                }
            }
        }
        Level level = Minecraft.getInstance().level;
        if (lastLevel != level) {
            WatutMod.dbg("resetting player status");
            for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
                WatutMod.dbg("reset player particles for " + entry.getKey() + " hash: " + entry.getValue());
                entry.getValue().resetParticles();
            }
            selfPlayerStatus.reset();
            selfPlayerStatusPrev.reset();
        }
        lastLevel = level;
    }

    public void tickPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.getUUID().equals(player.getUUID())) {
            tickLocalPlayerClient(player);
        }

        tickOtherPlayerClient(player);
        getStatus(player.getUUID()).tick();

        PlayerStatus status = getStatus(player);
        float adjRateTyping = 0.1F;
        if (status.getTypingAmplifierSmooth() < status.getTypingAmplifier() - adjRateTyping) {
            status.setTypingAmplifierSmooth(status.getTypingAmplifierSmooth() + adjRateTyping);
        } else if (status.getTypingAmplifierSmooth() > status.getTypingAmplifier() + adjRateTyping) {
            status.setTypingAmplifierSmooth(status.getTypingAmplifierSmooth() - adjRateTyping);
        }
    }

    public void tickLocalPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        PlayerStatus statusLocal = getStatusLocal();
        PlayerStatus statusPrevLocal = getStatusPrevLocal();
        if (ConfigClient.sendActiveGui && !statusLocal.isIdle()) {
            if (mc.screen instanceof ChatScreen) {
                ChatScreen chat = (ChatScreen) mc.screen;
                if (checkIfTyping(chat.input.getValue(), player)) {
                    sendStatus(PlayerStatus.PlayerGuiState.CHAT_TYPING);
                } else {
                    if (singleplayerTesting && false) {
                        sendStatus(PlayerStatus.PlayerGuiState.INVENTORY);
                    } else {
                        //sendStatus(PlayerStatus.PlayerGuiState.INVENTORY);
                        sendStatus(PlayerStatus.PlayerGuiState.CHAT_OPEN);
                    }
                }
            } else if (mc.screen instanceof EffectRenderingInventoryScreen) {
                sendStatus(PlayerStatus.PlayerGuiState.INVENTORY);
            } else if (mc.screen instanceof CraftingScreen) {
                sendStatus(PlayerStatus.PlayerGuiState.CRAFTING);
            } else if (mc.screen instanceof PauseScreen) {
                sendStatus(PlayerStatus.PlayerGuiState.ESCAPE);
            } else if (mc.screen != null) {
                sendStatus(PlayerStatus.PlayerGuiState.MISC);
            } else if (mc.screen == null) {
                sendStatus(PlayerStatus.PlayerGuiState.NONE);
                //Watut.dbg(mc.gui);
            }
        } else {
            sendStatus(PlayerStatus.PlayerGuiState.NONE);
        }

        if (ConfigClient.sendMouseInfo && mc.screen != null && mc.level.getGameTime() % (armMouseTickRate) == 0) {
            PlayerStatus.PlayerGuiState playerGuiState = statusLocal.getPlayerGuiState();
            //this wont trigger if theyre already idle, might be a good thing, just prevent idle, dont bring back from idle
            if (playerGuiState == PlayerStatus.PlayerGuiState.INVENTORY || playerGuiState == PlayerStatus.PlayerGuiState.CRAFTING || playerGuiState == PlayerStatus.PlayerGuiState.MISC || playerGuiState == PlayerStatus.PlayerGuiState.ESCAPE) {
                Pair<Float, Float> pos = getMousePos();
                if (pos.first != statusLocal.getScreenPosPercentX() || pos.second != statusLocal.getScreenPosPercentY()) {
                    onAction();
                }
                sendMouse(getMousePos(), statusLocal.isPressing());
            }
        }

        if (statusPrevLocal.getTicksSinceLastAction() != statusLocal.getTicksSinceLastAction()) {
            statusPrevLocal.setTicksSinceLastAction(statusLocal.getTicksSinceLastAction());
        }

        if (ConfigClient.sendIdleState) {
            statusLocal.setTicksSinceLastAction(statusLocal.getTicksSinceLastAction()+1);
            //tickSyncing mostly handles this, but if they JUST went idle, send the packet right away
            if (statusLocal.getTicksSinceLastAction() > statusLocal.getTicksToMarkPlayerIdleSyncedForClient()) {
                //System.out.println("receive idle ticks from server: " + ticksIdle + " for " + player.getUUID());
                if (statusLocal.isIdle() != statusPrevLocal.isIdle()) {
                    WatutMod.dbg("send idle getTicksSinceLastAction: " + statusLocal.getTicksSinceLastAction() + " - " + statusPrevLocal.getTicksSinceLastAction());
                    sendIdle(statusLocal);
                }
            }
        } else {
            statusLocal.setTicksSinceLastAction(0);
        }

        tickSyncing(player);
    }

    /**
     * Occasional syncing, force send state so server refreshes their side to other clients too
     *
     * this is to solve the reported problems of idle and typing states getting stuck on clients
     */
    public void tickSyncing(Player player) {
        if (steadyTickCounter % forcedSyncRate == 0) {
            PlayerStatus playerStatusLocal = getStatusLocal();
            sendIdle(playerStatusLocal);
            sendStatus(playerStatusLocal.getPlayerGuiState(), true);
            sendTyping(playerStatusLocal);
        }
    }

    public void onMouse(InputEvent.MouseButton.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.player.level() != null) {
            PlayerStatus.PlayerGuiState playerGuiState = getStatus(mc.player).getPlayerGuiState();
            if (ConfigClient.sendMouseInfo) {
                if (playerGuiState == PlayerStatus.PlayerGuiState.INVENTORY || playerGuiState == PlayerStatus.PlayerGuiState.CRAFTING || playerGuiState == PlayerStatus.PlayerGuiState.MISC) {
                    sendMouse(getMousePos(), event.getAction() != 0);
                }
            }

            //this wont trigger if theyre already idle, might be a good thing, just prevent idle, dont bring back from idle
            if (mc.screen == null || (event.getAction() != 0 && (playerGuiState == PlayerStatus.PlayerGuiState.INVENTORY || playerGuiState == PlayerStatus.PlayerGuiState.CRAFTING))) {
                onAction();
            }
        }
    }

    public void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.player.level() != null) {
            if (mc.screen == null || mc.screen instanceof ChatScreen) {
                onAction();
            }
        }
    }

    public void onAction() {
        if (!ConfigClient.sendIdleState) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.player.level() != null) {
            PlayerStatus statusLocal = getStatusLocal();
            if (statusLocal.isIdle()) {
                statusLocal.setTicksSinceLastAction(0);
                WatutMod.dbg("send idle: " + 0);
                sendIdle(statusLocal);
            } else {
                statusLocal.setTicksSinceLastAction(0);
            }
        }
    }

    public Pair<Float, Float> getMousePos() {
        Minecraft mc = Minecraft.getInstance();
        double xPercent = (mc.mouseHandler.xpos() / mc.getWindow().getScreenWidth()) - 0.5;
        double yPercent = (mc.mouseHandler.ypos() / mc.getWindow().getScreenHeight()) - 0.5;
        //TODO: factor in clients gui scale, aka a ratio of gui covering screen, adjust hand move scale accordingly
        //emphasize the movements
        double emphasis = 1.5;
        //emphasis = 3;
        double edgeLimit = 0.5;
        double edgeLimitYLower = 0.2;
        xPercent *= emphasis;
        yPercent *= emphasis;
        xPercent = Math.max(Math.min(xPercent, edgeLimit), -edgeLimit);
        //prevent hand in pants
        yPercent = Math.min(yPercent, edgeLimitYLower);
        return Pair.of((float) xPercent, (float) yPercent);
    }

    public boolean checkIfTyping(String input, Player player) {
        PlayerStatus statusLocal = getStatusLocal();
        typeRatePollCounter++;
        if (input.length() > 0) {
            if (!input.startsWith("/")) {
                if (!input.equals(statusLocal.getLastTypeString())) {
                    statusLocal.setLastTypeString(input);
                    statusLocal.setLastTypeTime(player.level().getGameTime());
                }

                if (typeRatePollCounter >= 10) {
                    typeRatePollCounter = 0;
                    int lengthPrev = statusLocal.getLastTypeStringForAmp().length();
                    if (!input.equals(statusLocal.getLastTypeStringForAmp())) {
                        statusLocal.setLastTypeStringForAmp(input);
                        statusLocal.setLastTypeTimeForAmp(player.level().getGameTime());
                        int length = input.length();
                        int newDiff = length - lengthPrev;
                        //cap amp to 8
                        float amp = Math.max(0, Math.min(8, (newDiff / (float)6) * 2F));
                        if (ConfigClient.sendTypingSpeed) {
                            statusLocal.setTypingAmplifier(amp);
                        } else {
                            statusLocal.setTypingAmplifier(1F);
                        }
                        sendTyping(statusLocal);
                    } else {
                        if (ConfigClient.sendTypingSpeed) statusLocal.setTypingAmplifier(0);
                    }
                }

            }
        } else {
            statusLocal.setLastTypeString(input);
            statusLocal.setLastTypeDiff(0);
            return false;
        }
        if (statusLocal.getLastTypeTime() + typingIdleTimeout >= player.level().getGameTime()) {
            return true;
        }
        return false;
    }

    public String getTypingPlayers() {
        Minecraft mc = Minecraft.getInstance();
        String str = "";

        for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
            if (entry.getValue().getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(entry.getKey());
                if (info != null) {
                    GameProfile profile = info.getProfile();
                    if (profile != null) {
                        str += profile.getName() + ", ";
                    }
                }
            }
        }

        int playersLengthStr = str.length();
        String anim = "";
        int animRate = 6;
        long time = mc.level.getGameTime() % (animRate*4);
        while (time > animRate) {
            time -= animRate;
            anim += ".";
        }

        if (playersLengthStr > 50) {
            str = "Several people are typing" + anim;
        } else if (str.length() > 2) {
            str = str.substring(0, str.length() - 2) + " is typing" + anim;
        }
        return str;
    }

    public boolean shouldAnimate(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return player != mc.player || !mc.options.getCameraType().isFirstPerson();
    }

    public void tickOtherPlayerClient(Player player) {
        PlayerStatus playerStatus = getStatus(player);
        PlayerStatus playerStatusPrev = getStatusPrev(player);

        long stableTime = steadyTickCounter;
        float sin = (float) Math.sin((stableTime / 30F) % 360);
        float cos = (float) Math.cos((stableTime / 30F) % 360);
        float idleY = (float) (2.6 + (cos * 0.03F));

        boolean idleParticleChangeOrGone = playerStatus.isIdle() != playerStatusPrev.isIdle() || playerStatus.getParticleIdle() == null;
        boolean statusParticleChangeOrGone = playerStatus.getPlayerGuiState() != playerStatusPrev.getPlayerGuiState() || playerStatus.getParticle() == null;

        if (idleParticleChangeOrGone || !playerStatus.isIdle()) {
            if (playerStatus.getParticleIdle() != null) {
                playerStatus.getParticleIdle().remove();
                playerStatus.setParticleIdle(null);
            }
        }
        if (statusParticleChangeOrGone || playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
            if (playerStatus.getParticle() != null) {
                playerStatus.getParticle().remove();
                playerStatus.setParticle(null);
            }
        }

        double quadSize = 0.3F + Math.sin((stableTime / 10F) % 360) * 0.01F;

        if (shouldAnimate(player)) {
            if (idleParticleChangeOrGone) {
                if (ConfigClient.showIdleStatesInPlayerAboveHead && playerStatus.isIdle()) {
                    ParticleRotating particle = new ParticleStatic((ClientLevel) player.level(), player.position().x, player.position().y + idleY, player.position().z, ParticleRegistry.idle.getSprite());
                    if (particle != null) {
                        playerStatus.setParticleIdle(particle);
                        Minecraft.getInstance().particleEngine.add(particle);
                        particle.setQuadSize((float) quadSize);

                        WatutMod.dbg("spawning idle particle for " + player.getUUID());
                    }
                }
            }
            if (statusParticleChangeOrGone) {
                Particle particle = null;
                Vec3 posParticle = getParticlePosition(player);
                if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_OPEN) {
                    particle = new ParticleAnimated((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chat_idle.getSpriteSet());
                } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING) {
                    particle = new ParticleAnimated((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chat_typing.getSpriteSet());
                } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.INVENTORY) {
                    particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.inventory.getSpriteSet());
                } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.CRAFTING) {
                    particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.crafting.getSpriteSet());
                } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.ESCAPE) {
                    particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.escape.getSpriteSet());
                } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.MISC) {
                    particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chest.getSpriteSet());
                }
                if (particle != null) {
                    playerStatus.setParticle(particle);
                    Minecraft.getInstance().particleEngine.add(particle);
                }
            } else {

            }
        }

        if (playerStatus.getParticleIdle() != null) {
            ParticleStatic particle = (ParticleStatic)playerStatus.getParticleIdle();
            if (!particle.isAlive()) {
                playerStatus.getParticleIdle().remove();
                playerStatus.setParticleIdle(null);
            } else {
                particle.setPos(player.position().x, player.position().y + idleY, player.position().z);
                particle.setPosPrev(player.position().x, player.position().y + idleY, player.position().z);
                particle.setParticleSpeed(0, 0, 0);
                particle.rotationYaw = -player.yBodyRot + 180;
                particle.prevRotationYaw = particle.rotationYaw;
                particle.rotationRoll = cos * 5;
                particle.prevRotationRoll = particle.rotationRoll;
                particle.setQuadSize(0.15F + sin * 0.03F);
                particle.setAlpha(0.5F);
            }
        }

        if (playerStatus.getParticle() != null) {
            ParticleRotating particle = (ParticleRotating)playerStatus.getParticle();
            if (!particle.isAlive()) {
                playerStatus.getParticle().remove();
                playerStatus.setParticle(null);
            } else {
                Vec3 posParticle = getParticlePosition(player);
                particle.setPos(posParticle.x, posParticle.y, posParticle.z);
                particle.setParticleSpeed(0, 0, 0);

                if (particle instanceof ParticleStaticLoD) {
                    particle.setQuadSize((float) quadSize);
                    if (Minecraft.getInstance().cameraEntity != null) {
                        double distToCamera = Minecraft.getInstance().cameraEntity.distanceTo(player);
                        double distToCameraCapped = Math.max(3F, Math.min(10F, distToCamera));
                        //Watut.dbg(distToCamera);
                        float alpha = (float) Math.max(0.35F, 1F - (distToCameraCapped / 10F));
                        particle.setAlpha(alpha);
                        ((ParticleStaticLoD) particle).setParticleFromDistanceToCamera((float) distToCamera);
                    } else {
                        particle.setAlpha(0.5F);
                    }

                }

                particle.rotationYaw = -player.yBodyRot;
                particle.prevRotationYaw = particle.rotationYaw;
                particle.rotationPitch = 20;
                particle.prevRotationPitch = particle.rotationPitch;

            }
        }

        playerStatusPrev.setPlayerGuiState(playerStatus.getPlayerGuiState());
        if (playerStatusPrev.getTicksSinceLastAction() != playerStatus.getTicksSinceLastAction()) {
            playerStatusPrev.setTicksSinceLastAction(playerStatus.getTicksSinceLastAction());
        }
    }

    public boolean renderPingIconHook(PlayerTabOverlay playerTabOverlay, GuiGraphics pGuiGraphics, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo) {
        if (Minecraft.getInstance().particleEngine == null || pPlayerInfo == null || pPlayerInfo.getProfile() == null || !ConfigClient.showIdleStatesInPlayerList) return false;
        PlayerStatus playerStatus = getStatus(pPlayerInfo.getProfile().getId());
        if (playerStatus.isIdle()) {
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0F, 0.0F, 101F);
            TextureAtlasSprite sprite = ParticleRegistry.idle.getSprite();
            int x = (int) (Minecraft.getInstance().particleEngine.textureAtlas.width * sprite.getU0());
            int y = (int) (Minecraft.getInstance().particleEngine.textureAtlas.height * sprite.getV0());
            pGuiGraphics.blit(sprite.atlasLocation(), p_282801_ + p_281809_ - 11, pY, x, y, 10, 8, Minecraft.getInstance().particleEngine.textureAtlas.width, Minecraft.getInstance().particleEngine.textureAtlas.height);
            pGuiGraphics.pose().popPose();
            return true;
        }
        return false;
    }

    public void setupRotationsHook(EntityModel model, Entity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        if (!ConfigClient.showPlayerAnimations) return;
        Minecraft mc = Minecraft.getInstance();
        boolean inOwnInventory = pEntity == mc.player && (mc.screen instanceof EffectRenderingInventoryScreen) && pEntity.isAlive();
        //boolean isRealPlayer = pEntity.tickCount > 10;
        boolean isRealPlayer = pEntity.level().players().contains(pEntity);
        if (model instanceof PlayerModel playerModel && pEntity instanceof Player player && isRealPlayer && ((!inOwnInventory && shouldAnimate((Player) pEntity)) || singleplayerTesting)) {
            PlayerStatus playerStatus = getStatus(player);
            //try to filter out paper model, could use a better context clue, this is using a quirk of rotation not getting wrapped
            boolean contextIsInventoryPaperDoll = playerModel.head.yRot > Math.PI;
            if (!contextIsInventoryPaperDoll) {
                if (playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                    playerStatus.yRotHeadBeforeOverriding = playerModel.head.yRot;
                    playerStatus.xRotHeadBeforeOverriding = playerModel.head.xRot;
                    if (player.level().getGameTime() % 5 == 0) {
                        //Watut.dbg("setting head data for " + playerStatus.yRotHeadBeforeOverriding);
                    }
                } else {
                    if (playerModel.head.yRot <= Math.PI) {
                        playerStatus.yRotHeadWhileOverriding = playerModel.head.yRot;
                        playerStatus.xRotHeadWhileOverriding = playerModel.head.xRot;
                    }
                }
            }

            if (playerStatus.isLerping() || playerStatus.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE || playerStatus.isIdle()) {
                float partialTick = pAgeInTicks - ((int)pAgeInTicks);
                playerStatus.lastPartialTick = partialTick;

                playerModel.rightArm.yRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.yRot, playerStatus.getLerpTarget().rightArm.yRot);
                playerModel.rightArm.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.xRot, playerStatus.getLerpTarget().rightArm.xRot);
                playerModel.rightArm.x += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.x, playerStatus.getLerpTarget().rightArm.x);
                playerModel.rightArm.y += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.y, playerStatus.getLerpTarget().rightArm.y);
                playerModel.rightArm.z += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.z, playerStatus.getLerpTarget().rightArm.z);

                playerModel.leftArm.yRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().leftArm.yRot, playerStatus.getLerpTarget().leftArm.yRot);
                playerModel.leftArm.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().leftArm.xRot, playerStatus.getLerpTarget().leftArm.xRot);

                //TODO: workaround to a weird issue of just the y rotation in creative mode being super out of wack
                // likely because of paper doll in inventory screen, couldnt fix by removing Math.PI until within range
                // still happening even for non creative, wat
                // well we still need to find the optimal direction to rotate and fix the target rotation value to that so it doesnt invert and spin i guess
                float yRotDiff = playerStatus.getLerpTarget().head.yRot - playerStatus.getLerpPrev().head.yRot;
                if (Math.abs(yRotDiff) < Math.PI / 2) {
                    playerModel.head.yRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.yRot, playerStatus.getLerpTarget().head.yRot);
                    if (player.level().getGameTime() % 5 == 0) {
                        //Watut.dbg("yRot: " + playerModel.head.yRot);
                    }
                }

                playerModel.head.xRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.xRot, playerStatus.getLerpTarget().head.xRot);
                playerModel.head.zRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.zRot, playerStatus.getLerpTarget().head.zRot);

                playerModel.rightSleeve.yRot = playerModel.rightArm.yRot;
                playerModel.rightSleeve.xRot = playerModel.rightArm.xRot;
                playerModel.rightSleeve.x = playerModel.rightArm.x;
                playerModel.rightSleeve.y = playerModel.rightArm.y;
                playerModel.rightSleeve.z = playerModel.rightArm.z;

                playerModel.leftSleeve.yRot = playerModel.leftArm.yRot;
                playerModel.leftSleeve.xRot = playerModel.leftArm.xRot;

                playerModel.hat.xRot = playerModel.head.xRot;
                playerModel.hat.yRot = playerModel.head.yRot;

                if (playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING) {
                    float amp = playerStatus.getTypingAmplifierSmooth();
                    float typeAngle = (float) ((Math.toRadians(Math.sin((pAgeInTicks * 1F) % 360) * 15 * amp)));
                    float typeAngle2 = (float) ((Math.toRadians(-Math.sin((pAgeInTicks * 1F) % 360) * 15 * amp)));
                    playerModel.rightArm.xRot -= typeAngle;
                    playerModel.rightSleeve.xRot -= typeAngle;
                    playerModel.leftArm.xRot -= typeAngle2;
                    playerModel.leftSleeve.xRot -= typeAngle2;
                }

                if (playerStatus.isIdle()) {
                    float angle = (float) ((Math.toRadians(Math.sin((pAgeInTicks * 0.05F) % 360) * 15)));
                    float angle2 = (float) ((Math.toRadians(Math.cos((pAgeInTicks * 0.05F) % 360) * 7)));
                    playerModel.head.xRot += angle2;
                    playerModel.head.zRot += angle;
                }
            }
            playerModel.hat.xRot = playerModel.head.xRot;
            playerModel.hat.yRot = playerModel.head.yRot;
            playerModel.hat.zRot = playerModel.head.zRot;
        }
    }

    public void setPoseTarget(UUID uuid, boolean becauseMousePress) {
        //Watut.dbg("setPoseTarget");
        PlayerStatus playerStatus = getStatus(uuid);

        playerStatus.getLerpPrev().rightArm = playerStatus.getLerpTarget().rightArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().rightArm, playerStatus.lastPartialTick);
        playerStatus.getLerpPrev().leftArm = playerStatus.getLerpTarget().leftArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().leftArm, playerStatus.lastPartialTick);
        playerStatus.getLerpPrev().head = playerStatus.getLerpTarget().head.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().head, playerStatus.lastPartialTick);

        //rightArm can become NaN for some reason???????
        if (Float.isNaN(playerStatus.getLerpPrev().rightArm.yRot)) {
            playerStatus.getLerpPrev().rightArm.yRot = 0;
        }
        if (Float.isNaN(playerStatus.getLerpPrev().rightArm.xRot)) {
            playerStatus.getLerpPrev().rightArm.xRot = 0;
        }

        boolean pointing =
                playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.INVENTORY ||
                playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CRAFTING ||
                playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.MISC ||
                playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.ESCAPE;
        boolean typing = playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING;

        if (becauseMousePress) {
            playerStatus.setNewLerp(armMouseTickRate * 0.5F);
        } else {
            playerStatus.setNewLerp(armMouseTickRate * 1F);
        }

        if (pointing || typing) {
            playerStatus.getLerpTarget().head.xRot = (float) Math.toRadians(15);
            playerStatus.getLerpTarget().head.yRot = 0;
        }

        if (pointing) {
            double xPercent = playerStatus.getScreenPosPercentX();
            double yPercent = playerStatus.getScreenPosPercentY();
            double x = Math.toRadians(90) - Math.toRadians(22.5) - yPercent;
            double y = -Math.toRadians(15) + xPercent;

            playerStatus.getLerpTarget().rightArm.yRot = (float) y;
            playerStatus.getLerpTarget().rightArm.xRot = (float) -x;

            if (playerStatus.isPressing()) {
                Vec3 vec = calculateViewVector((float) Math.toDegrees(y), (float) Math.toDegrees(x));
                float press = 1;
                playerStatus.getLerpTarget().rightArm.x = (float) (press * vec.y);
                playerStatus.getLerpTarget().rightArm.y = (float) (press * vec.z);
                playerStatus.getLerpTarget().rightArm.z = (float) (press * vec.x);
            } else {
                playerStatus.getLerpTarget().rightArm.x = (float) 0;
                playerStatus.getLerpTarget().rightArm.z = (float) 0;
                playerStatus.getLerpTarget().rightArm.y = (float) 0;
            }

            playerStatus.getLerpTarget().leftArm.xRot = (float) -Math.toRadians(70);
            playerStatus.getLerpTarget().leftArm.yRot = (float) Math.toRadians(25);


        } else if (typing) {
            double x = Math.toRadians(90) - Math.toRadians(22.5);
            playerStatus.getLerpTarget().rightArm.xRot = (float) -x;
            playerStatus.getLerpTarget().leftArm.xRot = (float) -x;

            double tiltIn = Math.toRadians(20);
            playerStatus.getLerpTarget().rightArm.yRot = (float) -tiltIn;
            playerStatus.getLerpTarget().leftArm.yRot = (float) tiltIn;
        }

        //reset to neutral
        if (!pointing && !typing && !playerStatus.isIdle()) {
            playerStatus.setLerpTarget(new Lerpables());
            playerStatus.getLerpTarget().head.xRot = playerStatus.xRotHeadBeforeOverriding;
            playerStatus.getLerpTarget().head.yRot = playerStatus.yRotHeadBeforeOverriding;
        }

        if (playerStatus.isIdle()) {
            playerStatus.getLerpTarget().head.xRot = (float) Math.toRadians(70);
            playerStatus.setNewLerp(40);
        }

        //setup head lerp from to be where our head was before overrides started
        if (getStatusPrev(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE && getStatus(uuid).getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE) {
            playerStatus.getLerpPrev().head.xRot = playerStatus.xRotHeadBeforeOverriding;
            playerStatus.getLerpPrev().head.yRot = playerStatus.yRotHeadBeforeOverriding;
        }


    }

    public Vec3 getParticlePosition(Player player) {
        Vec3 pos = player.position();
        float distFromFace = 0.75F;
        Vec3 lookVec = getBodyAngle(player).scale(distFromFace);
        return new Vec3(pos.x + lookVec.x, pos.y + 1.2D, pos.z + lookVec.z);
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

    public PlayerStatus getStatusLocal() {
        return selfPlayerStatus;
    }

    public PlayerStatus getStatusPrevLocal() {
        return selfPlayerStatusPrev;
    }

    public PlayerStatus getStatusPrev(Player player) {
        return getStatusPrev(player.getUUID());
    }

    public PlayerStatus getStatusPrev(UUID uuid) {
        return getStatusPrev(uuid, false);
    }

    public PlayerStatus getStatusPrev(UUID uuid, boolean local) {
        if (local) return getStatusPrevLocal();
        checkPrev(uuid);
        return lookupPlayerToStatusPrev.get(uuid);
    }

    public void checkPrev(UUID uuid) {
        if (!lookupPlayerToStatusPrev.containsKey(uuid)) {
            lookupPlayerToStatusPrev.put(uuid, new PlayerStatus(PlayerStatus.PlayerGuiState.NONE));
        }
    }

    public void sendStatus(PlayerStatus.PlayerGuiState playerStatus) {
        sendStatus(playerStatus, false);
    }

    public void sendStatus(PlayerStatus.PlayerGuiState playerStatus, boolean force) {
        if (getStatusLocal().getPlayerGuiState() != playerStatus || force) {
            CompoundTag data = new CompoundTag();
            data.putInt(WatutNetworking.NBTDataPlayerStatus, playerStatus.ordinal());
            //Watut.dbg("sending status from client: " + playerStatus + " for " + Minecraft.getInstance().player.getUUID());
            WatutNetworking.instance().clientSendToServer(data);
        }
        getStatusLocal().setPlayerGuiState(playerStatus);
    }

    public void sendMouse(Pair<Float, Float> pos, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        float x = pos.first;
        float y = pos.second;
        if (mc.level.getNearestPlayer(mc.player.getX(), mc.player.getY(), mc.player.getZ(), nearbyPlayerDataSendDist, (entity) -> entity != mc.player) != null) {
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
            status.setTypingAmplifier(data.getFloat(WatutNetworking.NBTDataPlayerTypingAmp));
        }

        if (data.contains(WatutNetworking.NBTDataPlayerMouseX)) {
            float x = data.getFloat(WatutNetworking.NBTDataPlayerMouseX);
            float y = data.getFloat(WatutNetworking.NBTDataPlayerMouseY);
            boolean pressed = data.getBoolean(WatutNetworking.NBTDataPlayerMousePressed);
            boolean differentPress = status.isPressing() != pressed;
            setMouse(uuid, x, y, pressed);
            setPoseTarget(uuid, differentPress);
            if (pressed) {
                Player player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
                if (player != null && ConfigClient.playMouseClickSounds && player != Minecraft.getInstance().player) {
                    WatutMod.dbg("play sound for " + uuid + " name " + player.getDisplayName().getString());
                    player.level().playLocalSound(player.getOnPos(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.05F, 0.1F, false);
                }
            }
        }

        if (data.contains(WatutNetworking.NBTDataPlayerStatus)) {
            PlayerStatus.PlayerGuiState playerGuiState = PlayerStatus.PlayerGuiState.get(data.getInt(WatutNetworking.NBTDataPlayerStatus));
            status.setPlayerGuiState(playerGuiState);
            if (status.getPlayerGuiState() != statusPrev.getPlayerGuiState()) {
                WatutMod.dbg("New gui player state and new pose target set relating to: " + status.getPlayerGuiState() + " for " + uuid);
                if (statusPrev.getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                    status.setLerpTarget(new Lerpables());
                }
                if (status.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_OPEN) {
                    status.setTypingAmplifier(1F);
                    status.setTypingAmplifierSmooth(1F);
                }
                setPoseTarget(uuid, false);
                Player player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
                if (player != null && ConfigClient.playScreenOpenSounds && player != Minecraft.getInstance().player) {
                    PlayerStatus.PlayerGuiState playerGuiStatePrev = statusPrev.getPlayerGuiState();
                    if (playerGuiState == PlayerStatus.PlayerGuiState.INVENTORY || playerGuiState == PlayerStatus.PlayerGuiState.CRAFTING || playerGuiState == PlayerStatus.PlayerGuiState.MISC ||
                            playerGuiStatePrev == PlayerStatus.PlayerGuiState.INVENTORY || playerGuiStatePrev == PlayerStatus.PlayerGuiState.CRAFTING || playerGuiStatePrev == PlayerStatus.PlayerGuiState.MISC) {
                        player.level().playLocalSound(player.getOnPos(), SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 0.9F, 1F, false);
                    }
                }
            }
        }

        if (data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) {
            //Watut.dbg("receive idle ticks from server: " + data.getInt(WatutNetworking.NBTDataPlayerIdleTicks) + " for " + uuid + " playerStatus hash: " + status);
            status.setTicksSinceLastAction(data.getInt(WatutNetworking.NBTDataPlayerIdleTicks));
            status.setTicksToMarkPlayerIdleSyncedForClient(data.getInt(WatutNetworking.NBTDataPlayerTicksToGoIdle));
            statusPrev.setTicksToMarkPlayerIdleSyncedForClient(data.getInt(WatutNetworking.NBTDataPlayerTicksToGoIdle));
            getStatusLocal().setTicksToMarkPlayerIdleSyncedForClient(data.getInt(WatutNetworking.NBTDataPlayerTicksToGoIdle));
            getStatusPrevLocal().setTicksToMarkPlayerIdleSyncedForClient(data.getInt(WatutNetworking.NBTDataPlayerTicksToGoIdle));
            if (statusPrev.isIdle() != status.isIdle()) {
                WatutMod.dbg("New idle player state and new pose target set relating to idle state: " + status.isIdle() + " for " + uuid);
                setPoseTarget(uuid, false);
            }
        }
    }

}
