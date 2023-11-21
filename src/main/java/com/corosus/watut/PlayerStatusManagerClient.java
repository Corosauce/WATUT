package com.corosus.watut;

import com.corosus.watut.math.Lerpables;
import com.corosus.watut.particle.ParticleAnimated;
import com.corosus.watut.particle.ParticleRotating;
import com.corosus.watut.particle.ParticleStatic;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
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
import net.minecraftforge.network.NetworkDirection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatusManagerClient extends PlayerStatusManager {

    private PlayerStatus selfPlayerStatus = new PlayerStatus(PlayerStatus.PlayerGuiState.NONE);
    public HashMap<UUID, PlayerStatus> lookupPlayerToStatusPrev = new HashMap<>();
    //public HashMap<UUID, ParticleAnimated> lookupPlayerToParticle = new HashMap<>();
    private long typingIdleTimeout = 60;

    //TODO: DEBUG VAL
    private boolean singleplayerTesting = true;

    private int armMouseTickRate = 5;

    public void tickPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.getUUID().equals(player.getUUID())) {
            tickLocalPlayerClient(player);

            if (singleplayerTesting) tickOtherPlayerClient(player);
        } else {
            tickOtherPlayerClient(player);
        }

        getStatus(player.getUUID()).tick();

        armMouseTickRate = 5;
    }

    public void tickLocalPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChatScreen) {
            ChatScreen chat = (ChatScreen) mc.screen;
            //Watut.dbg("chat: " + chat.input.getValue());
            if (checkIfTyping(chat.input.getValue(), player)) {
                sendStatus(PlayerStatus.PlayerGuiState.CHAT_TYPING);
            } else {
                //sendStatus(PlayerStatus.PlayerGuiState.CHAT_OPEN);
                sendStatus(PlayerStatus.PlayerGuiState.INVENTORY);
            }
        } else if (mc.screen instanceof InventoryScreen) {
            sendStatus(PlayerStatus.PlayerGuiState.INVENTORY);
        } else if (mc.screen instanceof CraftingScreen) {
            sendStatus(PlayerStatus.PlayerGuiState.CRAFTING);
        } else if (mc.screen != null) {
            sendStatus(PlayerStatus.PlayerGuiState.MISC);
        } else if (mc.screen == null) {
            sendStatus(PlayerStatus.PlayerGuiState.NONE);
        }

        if (mc.screen != null && mc.level.getGameTime() % armMouseTickRate == 0) {
            double xPercent = (mc.mouseHandler.xpos() / mc.getWindow().getScreenWidth()) - 0.5;
            double yPercent = (mc.mouseHandler.ypos() / mc.getWindow().getScreenHeight()) - 0.5;
            //TODO: factor in clients gui scale, aka a ratio of gui covering screen, adjust hand move scale accordingly
            //emphasize the movements
            double emphasis = 1.5;
            double edgeLimit = 0.5;
            xPercent *= emphasis;
            yPercent *= emphasis;
            xPercent = Math.max(Math.min(xPercent * emphasis, edgeLimit), -edgeLimit);
            //yPercent = Math.max(Math.min(yPercent * emphasis, edgeLimit), -edgeLimit);
            //System.out.println(xPercent);
            sendMouse((float) xPercent, (float) yPercent);
        }
    }

    public boolean checkIfTyping(String input, Player player) {
        PlayerStatus status = getStatus(player);
        if (input.length() > 0) {
            if (!input.startsWith("/")) {
                if (!input.equals(status.getLastTypeString())) {
                    status.setLastTypeString(input);
                    status.setLastTypeTime(player.level().getGameTime());
                }
            }
        } else {
            status.setLastTypeString(input);
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
                GameProfile profile = mc.getConnection().getPlayerInfo(entry.getKey()).getProfile();
                if (profile != null) {
                    str += profile.getName() + ", ";
                }
            }
        }
        String anim = "";
        int animRate = 6;
        long time = mc.level.getGameTime() % (animRate*4);
        while (time > animRate) {
            time -= animRate;
            anim += ".";
        }

        if (str.length() > 2) {
            str = str.substring(0, str.length() - 2) + " is typing" + anim;
        }
        return str;
    }

    public void tickOtherPlayerClient(Player player) {
        Vec3 pos = player.position();
        PlayerStatus playerStatus = getStatus(player);
        PlayerStatus playerStatusPrev = getStatusPrev(player);
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
                particle = new ParticleAnimated((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chat_idle_set);
                //particle = new ParticleStatic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.crafting);
            } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING) {
                particle = new ParticleAnimated((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chat_typing_set);
            } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.INVENTORY) {
                particle = new ParticleStatic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.inventory);
            } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.CRAFTING) {
                particle = new ParticleStatic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.crafting);
            } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.MISC) {
                particle = new ParticleStatic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.inventory);
            }
            if (particle != null) {
                lookupPlayerToStatus.get(player.getUUID()).setParticle(particle);
                Minecraft.getInstance().particleEngine.add(particle);
            }
        } else {
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

                    if (particle instanceof ParticleStatic) {
                        particle.setQuadSize((float) (0.3F + Math.sin((player.level().getGameTime() / 10F) % 360) * 0.01F));
                    }

                    particle.rotationYaw = -player.yBodyRot;
                    particle.prevRotationYaw = particle.rotationYaw;
                    particle.rotationPitch = 20;
                    particle.prevRotationPitch = particle.rotationPitch;

                }
            }
        }
        setStatusPrev(player, playerStatus.getPlayerGuiState());
    }

    public void setupRotationsHook(EntityModel model, Entity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        Minecraft mc = Minecraft.getInstance();
        boolean inOwnInventory = pEntity == mc.player && mc.screen instanceof InventoryScreen;
        if (model instanceof PlayerModel && pEntity instanceof Player && (!inOwnInventory)) {
            PlayerModel playerModel = (PlayerModel) model;
            Player player = (Player) pEntity;
            PlayerStatus playerStatus = getStatus(player);
            playerStatus.yRotHead = playerModel.head.yRot;
            playerStatus.xRotHead = playerModel.head.xRot;

            if (playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                playerStatus.yRotHeadBeforePoses = playerModel.head.yRot;
                playerStatus.xRotHeadBeforePoses = playerModel.head.xRot;
            }

            if (playerStatus.isLerping() || playerStatus.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE) {
                float partialTick = pAgeInTicks - ((int)pAgeInTicks);

                playerModel.rightArm.yRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.yRot, playerStatus.getLerpTarget().rightArm.yRot);
                playerModel.rightArm.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.xRot, playerStatus.getLerpTarget().rightArm.xRot);

                playerModel.leftArm.yRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().leftArm.yRot, playerStatus.getLerpTarget().leftArm.yRot);
                playerModel.leftArm.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().leftArm.xRot, playerStatus.getLerpTarget().leftArm.xRot);

                playerModel.head.yRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.yRot, playerStatus.getLerpTarget().head.yRot);
                playerModel.head.xRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.xRot, playerStatus.getLerpTarget().head.xRot);

                /*playerModel.rightArm.yRot += playerStatus.getLerpTarget().rightArm.yRot;
                playerModel.rightArm.xRot += playerStatus.getLerpTarget().rightArm.xRot;*/

                playerModel.rightSleeve.yRot = playerModel.rightArm.yRot;
                playerModel.rightSleeve.xRot = playerModel.rightArm.xRot;

                playerModel.leftSleeve.yRot = playerModel.leftArm.yRot;
                playerModel.leftSleeve.xRot = playerModel.leftArm.xRot;

                playerModel.hat.xRot = playerModel.head.xRot;
                playerModel.hat.yRot = playerModel.head.yRot;

                if (playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING) {
                    float typeAngle = (float) ((Math.toRadians(Math.sin((pAgeInTicks / 1F) % 360) * 15)));
                    float typeAngle2 = (float) ((Math.toRadians(-Math.sin((pAgeInTicks / 1F) % 360) * 15)));
                    playerModel.rightArm.xRot -= typeAngle;
                    playerModel.rightSleeve.xRot -= typeAngle;
                    playerModel.leftArm.xRot -= typeAngle2;
                    playerModel.leftSleeve.xRot -= typeAngle2;
                }
            }
        }
    }

    public void setupRotationsHookOld(EntityModel model, Entity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        Minecraft mc = Minecraft.getInstance();
        boolean inOwnInventory = pEntity == mc.player && mc.screen instanceof InventoryScreen;
        if (model instanceof PlayerModel && pEntity instanceof Player && (!inOwnInventory)) {
            PlayerModel playerModel = (PlayerModel) model;
            Player player = (Player) pEntity;
            PlayerStatus playerStatus = getStatus(player);
            playerStatus.yRotHead = playerModel.head.yRot;
            playerStatus.xRotHead = playerModel.head.xRot;
            if (playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING) {
                float typeAngle = (float) (45 + (Math.toRadians(Math.sin((pAgeInTicks / 1F) % 360) * 15)));
                float typeAngle2 = (float) (45 + (Math.toRadians(-Math.sin((pAgeInTicks / 1F) % 360) * 15)));
                playerModel.rightArm.xRot -= typeAngle;
                playerModel.rightSleeve.xRot -= typeAngle;
                playerModel.leftArm.xRot -= typeAngle2;
                playerModel.leftSleeve.xRot -= typeAngle2;

                double tiltIn = Math.toRadians(20);
                playerModel.rightArm.yRot -= tiltIn;
                playerModel.rightSleeve.yRot -= tiltIn;
                playerModel.leftArm.yRot += tiltIn;
                playerModel.leftSleeve.yRot += tiltIn;

                playerModel.head.yRot = playerModel.body.yRot;
                playerModel.head.xRot = (float) Math.toRadians(25);

                playerModel.hat.yRot = playerModel.head.yRot;
                playerModel.hat.xRot = playerModel.head.xRot;

            } else if (playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_OPEN) {

            } else if (playerStatus.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE) {
                /*double xPercent = playerStatus.getScreenPosPercentX();
                double yPercent = playerStatus.getScreenPosPercentY();
                double x = Math.toRadians(90) - Math.toRadians(22.5) - yPercent;
                double y = -Math.toRadians(15) + xPercent;
                //y = 0;
                playerModel.rightArm.yRot += y;
                playerModel.rightSleeve.yRot += y;
                playerModel.rightArm.xRot -= x;
                playerModel.rightSleeve.xRot -= x;*/

                float partialTick = pAgeInTicks - ((int)pAgeInTicks);

                playerModel.rightArm.yRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.yRot, playerStatus.getLerpTarget().rightArm.yRot);
                playerModel.rightArm.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.xRot, playerStatus.getLerpTarget().rightArm.xRot);

                playerModel.leftArm.yRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().leftArm.yRot, playerStatus.getLerpTarget().leftArm.yRot);
                playerModel.leftArm.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().leftArm.xRot, playerStatus.getLerpTarget().leftArm.xRot);

                playerModel.head.yRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.yRot, playerStatus.getLerpTarget().head.yRot);
                playerModel.head.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.xRot, playerStatus.getLerpTarget().head.xRot);

                /*playerModel.rightArm.yRot += playerStatus.getLerpTarget().rightArm.yRot;
                playerModel.rightArm.xRot += playerStatus.getLerpTarget().rightArm.xRot;*/

                playerModel.rightSleeve.yRot = playerModel.rightArm.yRot;
                playerModel.rightSleeve.xRot = playerModel.rightArm.xRot;

                playerModel.leftSleeve.yRot = playerModel.leftArm.yRot;
                playerModel.leftSleeve.xRot = playerModel.leftArm.xRot;

                playerModel.hat.xRot = playerModel.head.xRot;
                playerModel.hat.yRot = playerModel.head.yRot;
            }
        }
    }

    public void setHandsTarget(UUID uuid) {
        //System.out.println("setHandsTarget");
        PlayerStatus playerStatus = getStatus(uuid);

        playerStatus.getLerpPrev().rightArm = playerStatus.getLerpTarget().rightArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().rightArm);
        playerStatus.getLerpPrev().leftArm = playerStatus.getLerpTarget().leftArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().leftArm);
        playerStatus.getLerpPrev().head = playerStatus.getLerpTarget().head.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().head);
        playerStatus.setNewLerp(armMouseTickRate * 2);

        double xPercent = playerStatus.getScreenPosPercentX();
        double yPercent = playerStatus.getScreenPosPercentY();
        double x = Math.toRadians(90) - Math.toRadians(22.5) - yPercent;
        double y = -Math.toRadians(15) + xPercent;

        playerStatus.getLerpTarget().rightArm.yRot = (float) y;
        playerStatus.getLerpTarget().rightArm.xRot = (float) -x;

        playerStatus.getLerpTarget().leftArm.xRot = (float) -Math.toRadians(70);
        playerStatus.getLerpTarget().leftArm.yRot = (float) Math.toRadians(25);
    }

    public void setPoseTarget(UUID uuid) {
        //System.out.println("setPoseTarget");
        PlayerStatus playerStatus = getStatus(uuid);

        playerStatus.getLerpPrev().rightArm = playerStatus.getLerpTarget().rightArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().rightArm);
        playerStatus.getLerpPrev().leftArm = playerStatus.getLerpTarget().leftArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().leftArm);
        playerStatus.getLerpPrev().head = playerStatus.getLerpTarget().head.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().head);
        playerStatus.setNewLerp(armMouseTickRate * 2);

        boolean pointing = playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.INVENTORY ||
                playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CRAFTING ||
                playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.MISC;
        boolean typing = playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_TYPING;
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

            playerStatus.getLerpTarget().leftArm.xRot = (float) -Math.toRadians(70);
            playerStatus.getLerpTarget().leftArm.yRot = (float) Math.toRadians(25);
        } else if (typing) {
            double tiltIn = Math.toRadians(20);
            playerStatus.getLerpTarget().rightArm.yRot = (float) -tiltIn;
            playerStatus.getLerpTarget().leftArm.yRot = (float) tiltIn;
        }

        if (!pointing && !typing) {
            playerStatus.setLerpTarget(new Lerpables());
            playerStatus.getLerpTarget().head.xRot = playerStatus.xRotHeadBeforePoses;
            playerStatus.getLerpTarget().head.yRot = playerStatus.yRotHeadBeforePoses;
        }


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

    protected final Vec3 calculateViewVector(float pXRot, float pYRot) {
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
            WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        }
        selfPlayerStatus.setPlayerGuiState(playerStatus);
    }

    public void sendMouse(float x, float y) {
        if (selfPlayerStatus.getScreenPosPercentX() != x || selfPlayerStatus.getScreenPosPercentY() != y) {
            CompoundTag data = new CompoundTag();
            data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateMousePlayer);
            data.putFloat(WatutNetworking.NBTDataPlayerMouseX, x);
            data.putFloat(WatutNetworking.NBTDataPlayerMouseY, y);

            WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        }
        selfPlayerStatus.setScreenPosPercentX(x);
        selfPlayerStatus.setScreenPosPercentY(y);
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
            setPoseTarget(uuid);
            //setHandsTarget(uuid);
            //since were fully overriding the head position, this needs to happen
            if (getStatusPrev(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                getStatus(uuid).getLerpPrev().head.yRot = getStatus(uuid).yRotHead;
                getStatus(uuid).getLerpPrev().head.xRot = getStatus(uuid).xRotHead;
            }
        }
    }

    public void receiveMouse(UUID uuid, float x, float y) {
        PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
        //Watut.dbg("got moud on client: " + playerStatus + " for player name: " + playerInfo.getProfile().getName());
        //Watut.dbg("x: " + x);
        setMouse(uuid, x, y);
        setPoseTarget(uuid);
        //setHandsTarget(uuid);
    }

}
