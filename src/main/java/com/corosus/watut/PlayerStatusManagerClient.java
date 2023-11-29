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
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.particle.Particle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatusManagerClient extends PlayerStatusManager {

    //TODO: some things are using the lookup and not this, maybe deprecate this for that
    private PlayerStatus selfPlayerStatus = new PlayerStatus(PlayerStatus.PlayerGuiState.NONE);
    public HashMap<UUID, PlayerStatus> lookupPlayerToStatusPrev = new HashMap<>();
    //public HashMap<UUID, ParticleAnimated> lookupPlayerToParticle = new HashMap<>();
    private long typingIdleTimeout = 60;

    private int armMouseTickRate = 5;
    private int idleTimeThreshold = 20*60*5;
    private long lastActionTime = 0;
    private int lastMinuteSentIdleStat = -1;

    private int typeRatePollCounter = 0;

    public void tickGame(TickEvent.ClientTickEvent event) {
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
    }

    public void tickPlayerClient(Player player) {
        if (singleplayerTesting) {
            idleTimeThreshold = 20*3;
        }
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
        //System.out.println("?? " + status.getTypingAmplifierSmooth());
    }

    public void tickLocalPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (ConfigClient.sendActiveGui) {
            if (mc.screen instanceof ChatScreen) {
                ChatScreen chat = (ChatScreen) mc.screen;
                //Watut.dbg("chat: " + chat.input.getValue());
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
                //System.out.println(mc.gui);
            }
        } else {
            sendStatus(PlayerStatus.PlayerGuiState.NONE);
        }

        if (ConfigClient.sendMouseInfo && mc.screen != null && mc.level.getGameTime() % armMouseTickRate == 0) {
            sendMouse(getMousePos(), selfPlayerStatus.isPressing());
        }

        //init
        if (lastActionTime == 0) {
            lastActionTime = player.level().getGameTime();
        }

        long ticksIdle = player.level().getGameTime() - lastActionTime;
        if (ConfigClient.sendIdleState && ticksIdle > idleTimeThreshold) {
            int minutesIdle = (int)(ticksIdle / 10 / 20);
            if (minutesIdle != lastMinuteSentIdleStat) {
                lastMinuteSentIdleStat = minutesIdle;
                //System.out.println("sending idle, " + lastMinuteSentIdleStat);
                PlayerStatus status = getStatus(player);
                status.setIdleTicks((int) ticksIdle);
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

    public boolean isIdle(UUID uuid) {
        return getStatus(uuid).getIdleTicks() > 0;
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
            if (isIdle(mc.player.getUUID())) {
                PlayerStatus status = getStatus(mc.player);
                status.setIdleTicks(0);
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
        //System.out.println(xPercent);
        return Pair.of((float) xPercent, (float) yPercent);
    }

    public boolean checkIfTyping(String input, Player player) {
        PlayerStatus status = getStatus(player);
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
        if (shouldAnimate(player)) {
            if (playerStatus.getPlayerGuiState() != playerStatusPrev.getPlayerGuiState() || lookupPlayerToStatus.get(player.getUUID()).getParticle() == null) {
                //System.out.println(playerStatus.getPlayerGuiState() + " vs " + playerStatusPrev.getPlayerGuiState());
                if (lookupPlayerToStatus.get(player.getUUID()).getParticle() != null) {
                    //Watut.dbg("remove particle");
                    lookupPlayerToStatus.get(player.getUUID()).getParticle().remove();
                    lookupPlayerToStatus.get(player.getUUID()).setParticle(null);
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
                    lookupPlayerToStatus.get(player.getUUID()).setParticle(particle);
                    Minecraft.getInstance().particleEngine.add(particle);
                }
            } else {

            }
        }
        if (lookupPlayerToStatus.get(player.getUUID()).getParticle() != null) {
            ParticleRotating particle = (ParticleRotating)lookupPlayerToStatus.get(player.getUUID()).getParticle();
            if (!particle.isAlive()) {
                lookupPlayerToStatus.get(player.getUUID()).getParticle().remove();
                lookupPlayerToStatus.get(player.getUUID()).setParticle(null);
            } else {
                updateParticle(player, particle);
                Vec3 posParticle = getParticlePosition(player);
                particle.setPos(posParticle.x, posParticle.y, posParticle.z);
                particle.setParticleSpeed(0, 0, 0);

                if (particle instanceof ParticleStaticLoD) {
                    particle.setQuadSize((float) (0.3F + Math.sin((player.level().getGameTime() / 10F) % 360) * 0.01F));
                    if (Minecraft.getInstance().cameraEntity != null) {
                        double distToCamera = Minecraft.getInstance().cameraEntity.distanceTo(player);
                        double distToCameraCapped = Math.max(3F, Math.min(10F, distToCamera));
                        //System.out.println(distToCamera);
                        float alpha = (float) Math.max(0.35F, 1F - (distToCamera / 10F));
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
        setStatusPrev(player, playerStatus.getPlayerGuiState());
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
                        //System.out.println("setting head data for " + playerStatus.yRotHeadBeforeOverriding);
                    }
                } else {
                    if (playerModel.head.yRot <= Math.PI) {
                        playerStatus.yRotHeadWhileOverriding = playerModel.head.yRot;
                        playerStatus.xRotHeadWhileOverriding = playerModel.head.xRot;
                    }
                }
            }

            if (playerStatus.isLerping() || playerStatus.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE) {
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
                //if (!player.isCreative()) {
                //if head diff < 90 degrees
                if (Math.abs(yRotDiff) < Math.PI / 2) {
                    playerModel.head.yRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.yRot, playerStatus.getLerpTarget().head.yRot);
                    if (player.level().getGameTime() % 5 == 0) {
                        //System.out.println("yRot: " + playerModel.head.yRot);
                    }
                }

                playerModel.head.xRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.xRot, playerStatus.getLerpTarget().head.xRot);

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


            }
        }
    }

    public void setPoseTarget(UUID uuid, boolean becauseMousePress) {
        //System.out.println("setPoseTarget");
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

            //x = Math.toRadians(180);

            playerStatus.getLerpTarget().rightArm.yRot = (float) y;
            playerStatus.getLerpTarget().rightArm.xRot = (float) -x;

            if (playerStatus.isPressing()) {
                /*playerStatus.getLerpTarget().rightArm.z = (float) (1 * Math.sin(-x));
                playerStatus.getLerpTarget().rightArm.y = (float) (1 * Math.cos(-x));*/
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
        if (!pointing && !typing) {
            playerStatus.setLerpTarget(new Lerpables());
            playerStatus.getLerpTarget().head.xRot = playerStatus.xRotHeadBeforeOverriding;
            playerStatus.getLerpTarget().head.yRot = playerStatus.yRotHeadBeforeOverriding;

            /*System.out.println("playerStatus.xRotHeadBeforePoses: " + playerStatus.xRotHeadBeforePoses);
            System.out.println("playerStatus.yRotHeadBeforePoses: " + playerStatus.yRotHeadBeforePoses);*/
        }




        /*System.out.println("yRot: " + playerStatus.getLerpTarget().head.yRot);
        System.out.println(": " + playerStatus.getLerpPrev().head.yRot + " - " + playerStatus.getLerpTarget().head.yRot);*/


    }

    public static float lerp(float pDelta, float pStart, float pEnd) {
        return pStart + pDelta * (pEnd - pStart);
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

    public void updateParticle(Player player, Particle particle) {
        Vec3 pos = player.position();
        float distFromFace = 1.5F;
        Vec3 lookVec = player.getLookAngle().scale(distFromFace);
    }

    public PlayerStatus getStatusPrev(Player player) {
        return getStatusPrev(player.getUUID());
    }

    public PlayerStatus getStatusPrev(UUID uuid) {
        checkPrev(uuid);
        return lookupPlayerToStatusPrev.get(uuid);
    }

    public void setStatusPrev(Player player, PlayerStatus.PlayerGuiState statusType) {
        checkPrev(player.getUUID());
        setStatusPrev(player.getUUID(), statusType);
    }

    public void setStatusPrev(UUID uuid, PlayerStatus.PlayerGuiState statusType) {
        checkPrev(uuid);
        lookupPlayerToStatusPrev.get(uuid).setPlayerGuiState(statusType);
    }

    public void checkPrev(UUID uuid) {
        if (!lookupPlayerToStatusPrev.containsKey(uuid)) {
            lookupPlayerToStatusPrev.put(uuid, new PlayerStatus(PlayerStatus.PlayerGuiState.NONE));
        }
    }

    public void sendStatus(PlayerStatus.PlayerGuiState playerStatus) {
        if (selfPlayerStatus.getPlayerGuiState() != playerStatus) {
            CompoundTag data = new CompoundTag();
            data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateStatusPlayer);
            data.putInt(WatutNetworking.NBTDataPlayerStatus, playerStatus.ordinal());
            //data.putFloat(WatutNetworking.NBTDataPlayerTypingAmp, playerStatus);
            WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        }
        selfPlayerStatus.setPlayerGuiState(playerStatus);
    }

    public void sendMouse(Pair<Float, Float> pos, boolean pressed) {
        float x = pos.first;
        float y = pos.second;
        if (selfPlayerStatus.getScreenPosPercentX() != x || selfPlayerStatus.getScreenPosPercentY() != y || selfPlayerStatus.isPressing() != pressed) {
            CompoundTag data = new CompoundTag();
            data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateMousePlayer);
            data.putFloat(WatutNetworking.NBTDataPlayerMouseX, x);
            data.putFloat(WatutNetworking.NBTDataPlayerMouseY, y);
            data.putBoolean(WatutNetworking.NBTDataPlayerMousePressed, pressed);

            WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        }
        selfPlayerStatus.setScreenPosPercentX(x);
        selfPlayerStatus.setScreenPosPercentY(y);
        selfPlayerStatus.setPressing(pressed);
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

    public void receiveStatus(UUID uuid, PlayerStatus.PlayerGuiState playerStatus) {
        PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
        //Watut.dbg("got status on client: " + playerStatus + " for player name: " + playerInfo.getProfile().getName());
        setGuiStatus(uuid, playerStatus);
        //setPoseTarget(uuid);
        //if were starting from NONE, reset data for proper lerp
        //if (getStatusPrev(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE && getStatus(uuid).getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE) {
        if (getStatus(uuid).getPlayerGuiState() != getStatusPrev(uuid).getPlayerGuiState()) {
            if (getStatusPrev(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                getStatus(uuid).setLerpTarget(new Lerpables());
            }
            setPoseTarget(uuid, false);
            //setHandsTarget(uuid);
            //since were fully overriding the head position, this needs to happen
            //for lerping back to neutral pos when exiting a different state
            if (getStatusPrev(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                //System.out.println("setup new prev head: " + getStatus(uuid).yRotHeadBeforeOverriding);
                getStatus(uuid).getLerpPrev().head.yRot = getStatus(uuid).yRotHeadWhileOverriding;
                getStatus(uuid).getLerpPrev().head.xRot = getStatus(uuid).xRotHeadWhileOverriding;
            }/* else if (getStatus(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                //make sure our previous are set to where the head was most recently when overrides are active
                System.out.println("setup new head start position: " + getStatus(uuid).yRotHeadBeforeOverriding);
                getStatus(uuid).getLerpTarget().head.xRot = getStatus(uuid).xRotHeadBeforeOverriding;
                getStatus(uuid).getLerpTarget().head.yRot = getStatus(uuid).yRotHeadBeforeOverriding;
            }*/
        }
    }

    public void receiveMouse(UUID uuid, float x, float y, boolean pressed) {
        PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
        //Watut.dbg("got moud on client: " + playerStatus + " for player name: " + playerInfo.getProfile().getName());
        //Watut.dbg("x: " + x);
        boolean differentPress = getStatus(uuid).isPressing() != pressed;
        setMouse(uuid, x, y, pressed);
        setPoseTarget(uuid, differentPress);
        //setHandsTarget(uuid);
    }

    public void receiveAny(UUID uuid, CompoundTag data) {
        PlayerStatus status = getStatus(uuid);
        if (data.contains(WatutNetworking.NBTDataPlayerTypingAmp)) status.setTypingAmplifier(data.getFloat(WatutNetworking.NBTDataPlayerTypingAmp));
        if (data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) status.setIdleTicks(data.getInt(WatutNetworking.NBTDataPlayerIdleTicks));
    }

    /*public void setIfExists(PlayerStatus status, CompoundTag data, String key, Function function) {
        if (data.contains(key)) {
            function.apply(data.getFloat(key));
        }
    }*/

}
