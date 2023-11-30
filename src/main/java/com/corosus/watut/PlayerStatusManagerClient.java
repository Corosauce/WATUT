package com.corosus.watut;

import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigCommon;
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
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

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
    private long lastActionTime = 0;
    private int lastMinuteSentIdleStat = -1;

    private int typeRatePollCounter = 0;
    private int animateTime = 0;

    private Level lastLevel;

    public void tickGame(TickEvent.ClientTickEvent event) {
        animateTime++;
        if (animateTime == Integer.MAX_VALUE) animateTime = 0;
        for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
            PlayerStatus playerStatus = entry.getValue();
            if (event.phase == TickEvent.Phase.START) {
                playerStatus.setFlagForRemoval(true);
            } else {
                if (playerStatus.isFlagForRemoval()) {
                    playerStatus.remove();
                }
            }
        }
        Level level = Minecraft.getInstance().level;
        if (lastLevel != level) {
            Watut.dbg("resetting player status");
            for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
                entry.getValue().remove();
            }
            selfPlayerStatus.remove();
            selfPlayerStatusPrev.remove();
            lastActionTime = 0;
        }
        lastLevel = level;
    }

    public void tickPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.getUUID().equals(player.getUUID())) {
            tickLocalPlayerClient(player);

            tickOtherPlayerClient(player);
        } else {
            tickOtherPlayerClient(player);
        }

        getStatus(player.getUUID()).tick();

        PlayerStatus status = getStatus(player);
        status.setFlagForRemoval(false);

        float adjRateTyping = 0.1F;
        if (status.getTypingAmplifierSmooth() < status.getTypingAmplifier() - adjRateTyping) {
            status.setTypingAmplifierSmooth(status.getTypingAmplifierSmooth() + adjRateTyping);
        } else if (status.getTypingAmplifierSmooth() > status.getTypingAmplifier() + adjRateTyping) {
            status.setTypingAmplifierSmooth(status.getTypingAmplifierSmooth() - adjRateTyping);
        }
    }

    public void tickLocalPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        PlayerStatus status = getStatusLocal();
        PlayerStatus statusPrev = getStatusPrevLocal();
        if (ConfigClient.sendActiveGui && !status.isIdle()) {
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
            } else if (mc.screen != null) {
                sendStatus(PlayerStatus.PlayerGuiState.MISC);
            } else if (mc.screen == null) {
                sendStatus(PlayerStatus.PlayerGuiState.NONE);
                //Watut.dbg(mc.gui);
            }
        } else {
            sendStatus(PlayerStatus.PlayerGuiState.NONE);
        }

        if (ConfigClient.sendMouseInfo && mc.screen != null && mc.level.getGameTime() % armMouseTickRate == 0) {
            PlayerStatus.PlayerGuiState playerGuiState = status.getPlayerGuiState();
            if (playerGuiState == PlayerStatus.PlayerGuiState.INVENTORY || playerGuiState == PlayerStatus.PlayerGuiState.CRAFTING || playerGuiState == PlayerStatus.PlayerGuiState.MISC) {
                sendMouse(getMousePos(), status.isPressing());
            }
        }

        //init
        if (lastActionTime == 0) {
            lastActionTime = player.level().getGameTime();
        }

        //idle detection
        long ticksIdle = player.level().getGameTime() - lastActionTime;
        statusPrev.setIdleTicks(status.getIdleTicks());
        if (ConfigClient.sendIdleState && ticksIdle > ConfigCommon.ticksToMarkPlayerIdle) {
            int minutesIdle = (int)(ticksIdle / 20 / 60);
            status.setIdleTicks((int) ticksIdle);
            if (status.isIdle() != statusPrev.isIdle()) {
                lastMinuteSentIdleStat = minutesIdle;
                Watut.dbg("send idle: " + ticksIdle);
                sendIdle(status);
            }
        }
    }

    @Override
    public void disconnectPlayer(Player player) {
        PlayerStatus status = getStatus(player);
        if (status.getParticle() != null) {
            status.getParticle().remove();
            status.setParticle(null);
        }
    }

    public void onMouse(InputEvent.MouseButton.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.player.level() != null) {
            if (ConfigClient.sendMouseInfo) sendMouse(getMousePos(), event.getAction() != 0);

            if (mc.screen == null) {
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
            PlayerStatus status = getStatusLocal();
            if (status.isIdle()) {
                status.setIdleTicks(0);
                Watut.dbg("send idle: " + 0);
                sendIdle(status);
            }
            lastActionTime = mc.player.level().getGameTime();
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
        xPercent *= emphasis;
        yPercent *= emphasis;
        xPercent = Math.max(Math.min(xPercent * emphasis, edgeLimit), -edgeLimit);
        //yPercent = Math.max(Math.min(yPercent * emphasis, edgeLimit), -edgeLimit);
        return Pair.of((float) xPercent, (float) yPercent);
    }

    public boolean checkIfTyping(String input, Player player) {
        PlayerStatus status = getStatusLocal();
        typeRatePollCounter++;
        if (input.length() > 0) {
            if (!input.startsWith("/")) {
                if (!input.equals(status.getLastTypeString())) {
                    status.setLastTypeString(input);
                    status.setLastTypeTime(player.level().getGameTime());
                }

                if (typeRatePollCounter >= 10) {
                    typeRatePollCounter = 0;
                    int lengthPrev = status.getLastTypeStringForAmp().length();
                    if (!input.equals(status.getLastTypeStringForAmp())) {
                        status.setLastTypeStringForAmp(input);
                        status.setLastTypeTimeForAmp(player.level().getGameTime());
                        int length = input.length();
                        int newDiff = length - lengthPrev;
                        //cap amp to 8
                        float amp = Math.max(0, Math.min(8, (newDiff / (float)6) * 2F));
                        if (ConfigClient.sendTypingSpeed) {
                            status.setTypingAmplifier(amp);
                        } else {
                            status.setTypingAmplifier(1F);
                        }
                        sendTyping(status);
                    } else {
                        if (ConfigClient.sendTypingSpeed) status.setTypingAmplifier(0);
                    }
                }

            }
        } else {
            status.setLastTypeString(input);
            status.setLastTypeDiff(0);
            return false;
        }
        if (status.getLastTypeTime() + typingIdleTimeout >= player.level().getGameTime()) {
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

        long stableTime = animateTime;
        float sin = (float) Math.sin((stableTime / 30F) % 360);
        float cos = (float) Math.cos((stableTime / 30F) % 360);
        float idleY = (float) (2.6 + (cos * 0.03F));

        if (shouldAnimate(player)) {
            if ((playerStatus.isIdle()) != (playerStatusPrev.isIdle()) || playerStatus.getParticleIdle() == null) {
                if (playerStatus.getParticleIdle() != null) {
                    playerStatus.getParticleIdle().remove();
                    playerStatus.setParticleIdle(null);
                }
                if (ConfigClient.showIdleStatesInPlayerAboveHead && playerStatus.isIdle()) {
                    ParticleRotating particle = new ParticleStatic((ClientLevel) player.level(), player.position().x, player.position().y + idleY, player.position().z, ParticleRegistry.idle.getSprite());
                    if (particle != null) {
                        playerStatus.setParticleIdle(particle);
                        Minecraft.getInstance().particleEngine.add(particle);
                        particle.setQuadSize((float) (0.3F + Math.sin((stableTime / 10F) % 360) * 0.01F));

                        Watut.dbg("spawning idle particle for " + player.getUUID());
                    }
                }
            }
            if (playerStatus.getPlayerGuiState() != playerStatusPrev.getPlayerGuiState() || playerStatus.getParticle() == null) {
                if (playerStatus.getParticle() != null) {
                    playerStatus.getParticle().remove();
                    playerStatus.setParticle(null);
                }
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
                    particle.setQuadSize((float) (0.3F + Math.sin((stableTime / 10F) % 360) * 0.01F));
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
        playerStatusPrev.setIdleTicks(playerStatus.getIdleTicks());
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
        Minecraft mc = Minecraft.getInstance();
        boolean inOwnInventory = pEntity == mc.player && (mc.screen instanceof EffectRenderingInventoryScreen);
        if (model instanceof PlayerModel && pEntity instanceof Player && ((!inOwnInventory && shouldAnimate((Player) pEntity)) || singleplayerTesting)) {
            PlayerModel playerModel = (PlayerModel) model;
            Player player = (Player) pEntity;
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

        playerStatus.getLerpPrev().rightArm = playerStatus.getLerpTarget().rightArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().rightArm);
        playerStatus.getLerpPrev().leftArm = playerStatus.getLerpTarget().leftArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().leftArm);
        playerStatus.getLerpPrev().head = playerStatus.getLerpTarget().head.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().head);

        //rightArm can become NaN for some reason???????
        if (Float.isNaN(playerStatus.getLerpPrev().rightArm.yRot)) {
            playerStatus.getLerpPrev().rightArm.yRot = 0;
        }
        if (Float.isNaN(playerStatus.getLerpPrev().rightArm.xRot)) {
            playerStatus.getLerpPrev().rightArm.xRot = 0;
        }

        boolean pointing = playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.INVENTORY ||
                playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CRAFTING ||
                playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.MISC;
        boolean typing = playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING;

        if (becauseMousePress) {
            playerStatus.setNewLerp(armMouseTickRate * 0.5F);
        } else {
            playerStatus.setNewLerp(armMouseTickRate * 2F);
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
        if (getStatusLocal().getPlayerGuiState() != playerStatus) {
            CompoundTag data = new CompoundTag();
            data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateStatusPlayer);
            data.putInt(WatutNetworking.NBTDataPlayerStatus, playerStatus.ordinal());
            Watut.dbg("sending status from client: " + playerStatus + " for " + Minecraft.getInstance().player.getUUID());
            WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        }
        getStatusLocal().setPlayerGuiState(playerStatus);
    }

    public void sendMouse(Pair<Float, Float> pos, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level.getNearestPlayer(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mouseDataSendDist, (entity) -> entity != mc.player) == null) return;
        float x = pos.first;
        float y = pos.second;
        if (getStatusLocal().getScreenPosPercentX() != x || getStatusLocal().getScreenPosPercentY() != y || getStatusLocal().isPressing() != pressed) {
            CompoundTag data = new CompoundTag();
            data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateMousePlayer);
            data.putFloat(WatutNetworking.NBTDataPlayerMouseX, x);
            data.putFloat(WatutNetworking.NBTDataPlayerMouseY, y);
            data.putBoolean(WatutNetworking.NBTDataPlayerMousePressed, pressed);

            WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        }
        getStatusLocal().setScreenPosPercentX(x);
        getStatusLocal().setScreenPosPercentY(y);
        getStatusLocal().setPressing(pressed);
    }

    public void sendTyping(PlayerStatus status) {
        CompoundTag data = new CompoundTag();
        data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateStatusAny);
        data.putFloat(WatutNetworking.NBTDataPlayerTypingAmp, status.getTypingAmplifier());

        WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
    }

    public void sendIdle(PlayerStatus status) {
        CompoundTag data = new CompoundTag();
        data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateStatusAny);
        data.putInt(WatutNetworking.NBTDataPlayerIdleTicks, status.getIdleTicks());

        WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
    }

    public void receiveStatus(UUID uuid, PlayerStatus.PlayerGuiState playerGuiState) {
        Watut.dbg("receive status " + playerGuiState + " for " + uuid);
        getStatus(uuid).setPlayerGuiState(playerGuiState);
        if (getStatus(uuid).getPlayerGuiState() != getStatusPrev(uuid).getPlayerGuiState()) {
            if (getStatusPrev(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                getStatus(uuid).setLerpTarget(new Lerpables());
            }
            setPoseTarget(uuid, false);
        }
    }

    public void receiveMouse(UUID uuid, float x, float y, boolean pressed) {
        boolean differentPress = getStatus(uuid).isPressing() != pressed;
        setMouse(uuid, x, y, pressed);
        setPoseTarget(uuid, differentPress);
    }

    public void receiveAny(UUID uuid, CompoundTag data) {
        PlayerStatus status = getStatus(uuid);
        if (data.contains(WatutNetworking.NBTDataPlayerTypingAmp)) status.setTypingAmplifier(data.getFloat(WatutNetworking.NBTDataPlayerTypingAmp));
        if (data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) {
            status.setIdleTicks(data.getInt(WatutNetworking.NBTDataPlayerIdleTicks));
            if (getStatusPrev(uuid).isIdle() != status.isIdle()) {
                setPoseTarget(uuid, false);
            }
        }
    }

}
