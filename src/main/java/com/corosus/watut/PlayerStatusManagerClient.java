package com.corosus.watut;

import com.corosus.watut.math.Lerpables;
import com.corosus.watut.particle.ParticleAnimated;
import com.corosus.watut.particle.ParticleRotating;
import com.corosus.watut.particle.ParticleStatic;
import com.ibm.icu.impl.Pair;
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
import net.minecraftforge.client.event.InputEvent;
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
    private boolean singleplayerTesting = false;

    private int armMouseTickRate = 5;

    public void tickPlayerClient(Player player) {
        singleplayerTesting = true;
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
                sendStatus(PlayerStatus.PlayerGuiState.CHAT_OPEN);
                //sendStatus(PlayerStatus.PlayerGuiState.CHAT_TYPING);
                //sendStatus(PlayerStatus.PlayerGuiState.INVENTORY);
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
            sendMouse(getMousePos(), selfPlayerStatus.isPressing());
        }
    }

    public void onMouse(InputEvent.MouseButton.Post event) {
        /*System.out.println("getButton: " + event.getButton());
        System.out.println("getAction: " + event.getAction());
        System.out.println("getModifiers: " + event.getModifiers());*/

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            sendMouse(getMousePos(), event.getAction() != 0);
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
                /*while (playerStatus.yRotHeadBeforePoses > Math.PI/2) {
                    playerStatus.yRotHeadBeforePoses -= Math.PI;
                }
                if (playerStatus.yRotHeadBeforePoses > Math.PI/2) {
                    playerStatus.yRotHeadBeforePoses = 0;
                }*/
                /*if (player.level().getGameTime() % 40 == 0) {
                    System.out.println("yrot: " + playerModel.head.yRot);
                }*/
                playerStatus.xRotHeadBeforePoses = playerModel.head.xRot;
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
                if (!player.isCreative()) {
                    playerModel.head.yRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.yRot, playerStatus.getLerpTarget().head.yRot);
                }

                playerModel.head.xRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.xRot, playerStatus.getLerpTarget().head.xRot);

                //System.out.println("playerModel.head.yRot: " + playerModel.head.yRot);

                //System.out.println("!!! playerStatus.getLerpPrev().head.yRot: " + playerStatus.getLerpPrev().head.yRot + " - " + playerStatus.getLerpTarget().head.yRot);

                /*playerModel.rightArm.yRot += playerStatus.getLerpTarget().rightArm.yRot;
                playerModel.rightArm.xRot += playerStatus.getLerpTarget().rightArm.xRot;*/

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

    public void setPoseTarget(UUID uuid, boolean becauseMousePress) {
        //System.out.println("setPoseTarget");
        PlayerStatus playerStatus = getStatus(uuid);

        playerStatus.getLerpPrev().rightArm = playerStatus.getLerpTarget().rightArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().rightArm);
        playerStatus.getLerpPrev().leftArm = playerStatus.getLerpTarget().leftArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().leftArm);
        playerStatus.getLerpPrev().head = playerStatus.getLerpTarget().head.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().head);

        /*if (playerStatus.getLerpPrev().head.yRot > Math.PI/2) {
            playerStatus.getLerpPrev().head.yRot = 0;
        }*/

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

        if (!pointing && !typing) {
            playerStatus.setLerpTarget(new Lerpables());
            playerStatus.getLerpTarget().head.xRot = playerStatus.xRotHeadBeforePoses;
            playerStatus.getLerpTarget().head.yRot = playerStatus.yRotHeadBeforePoses;

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
            WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        }
        selfPlayerStatus.setPlayerGuiState(playerStatus);
    }

    public void sendMouse(Pair<Float, Float>  pos, boolean pressed) {
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
            if (getStatusPrev(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                getStatus(uuid).getLerpPrev().head.yRot = getStatus(uuid).yRotHead;
                getStatus(uuid).getLerpPrev().head.xRot = getStatus(uuid).xRotHead;
            }
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

}
