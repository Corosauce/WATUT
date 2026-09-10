package com.corosus.watut.client.animation;

import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.config.CustomArmCorrections;
import com.corosus.watut.status.PlayerChatState;
import com.corosus.watut.status.PlayerGuiState;
import com.corosus.watut.status.PlayerStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Gestisce i calcoli matematici e l'applicazione delle pose/animazioni 3D
 * sul modello del giocatore (PlayerModel):
 * 1. Puntamento GUI con il braccio e rotazione testa
 * 2. Oscillazione digitazione tastiera
 * 3. Ciondolamento testa durante lo stato Idle/AFK
 */
public class PlayerAnimator {

    public static void setupRotations(PlayerModel playerModel, AvatarRenderState state, PlayerStatus playerStatus, boolean singleplayerTesting) {
        if (!ConfigClient.showPlayerAnimations || !ConfigServerControlledSyncedToClient.showPlayerAnimations) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || playerStatus == null) return;

        Entity entity = mc.level.getEntity(state.id);
        if (!(entity instanceof Player player)) return;

        boolean inOwnInventory = entity == mc.player && (mc.gui != null && mc.gui.screen() instanceof InventoryScreen) && entity.isAlive();
        boolean isRealPlayer = entity.level().players().contains(entity);

        if (isRealPlayer && ((!inOwnInventory && (player != mc.player || !mc.options.getCameraType().isFirstPerson())) || singleplayerTesting)) {
            boolean isInventoryPaperDoll = playerModel.head.yRot > Math.PI;
            if (!isInventoryPaperDoll) {
                if (playerStatus.getPlayerGuiState() == PlayerGuiState.NONE) {
                    playerStatus.yRotHeadBeforeOverriding = playerModel.head.yRot;
                    playerStatus.xRotHeadBeforeOverriding = playerModel.head.xRot;
                } else {
                    if (playerModel.head.yRot <= Math.PI) {
                        playerStatus.yRotHeadWhileOverriding = playerModel.head.yRot;
                        playerStatus.xRotHeadWhileOverriding = playerModel.head.xRot;
                    }
                }
            }

            if (playerStatus.isLerping() || playerStatus.getPlayerGuiState() != PlayerGuiState.NONE || playerStatus.isIdle()) {
                float pAgeInTicks = state.ageInTicks;
                float partialTick = pAgeInTicks - ((int) pAgeInTicks);
                playerStatus.lastPartialTick = partialTick;
                float lerpFactor = playerStatus.getPartialLerp(partialTick);

                Vector3f adjRightArm;
                Vector3f adjLeftArm;
                HumanoidArm mainArm = player.getMainArm();
                if (mainArm == HumanoidArm.RIGHT) {
                    adjRightArm = CustomArmCorrections.getAdjustmentForArm(player.getItemBySlot(EquipmentSlot.MAINHAND), player.getItemBySlot(EquipmentSlot.OFFHAND), EquipmentSlot.MAINHAND);
                    adjLeftArm = CustomArmCorrections.getAdjustmentForArm(player.getItemBySlot(EquipmentSlot.OFFHAND), player.getItemBySlot(EquipmentSlot.MAINHAND), EquipmentSlot.OFFHAND);
                } else {
                    adjLeftArm = CustomArmCorrections.getAdjustmentForArm(player.getItemBySlot(EquipmentSlot.MAINHAND), player.getItemBySlot(EquipmentSlot.OFFHAND), EquipmentSlot.MAINHAND);
                    adjRightArm = CustomArmCorrections.getAdjustmentForArm(player.getItemBySlot(EquipmentSlot.OFFHAND), player.getItemBySlot(EquipmentSlot.MAINHAND), EquipmentSlot.OFFHAND);
                }

                // Float.MAX_VALUE rappresenta "disabilitato"
                if (adjRightArm.y != Float.MAX_VALUE) playerModel.rightArm.yRot += Mth.lerp(lerpFactor, playerStatus.getLerpPrev().rightArm.yRot, playerStatus.getLerpTarget().rightArm.yRot);
                if (adjRightArm.x != Float.MAX_VALUE) playerModel.rightArm.xRot += Mth.lerp(lerpFactor, playerStatus.getLerpPrev().rightArm.xRot, playerStatus.getLerpTarget().rightArm.xRot);
                playerModel.rightArm.x += Mth.lerp(lerpFactor, playerStatus.getLerpPrev().rightArm.x, playerStatus.getLerpTarget().rightArm.x);
                playerModel.rightArm.y += Mth.lerp(lerpFactor, playerStatus.getLerpPrev().rightArm.y, playerStatus.getLerpTarget().rightArm.y);
                playerModel.rightArm.z += Mth.lerp(lerpFactor, playerStatus.getLerpPrev().rightArm.z, playerStatus.getLerpTarget().rightArm.z);

                if (adjLeftArm.y != Float.MAX_VALUE) playerModel.leftArm.yRot += Mth.lerp(lerpFactor, playerStatus.getLerpPrev().leftArm.yRot, playerStatus.getLerpTarget().leftArm.yRot);
                if (adjLeftArm.x != Float.MAX_VALUE) playerModel.leftArm.xRot += Mth.lerp(lerpFactor, playerStatus.getLerpPrev().leftArm.xRot, playerStatus.getLerpTarget().leftArm.xRot);

                float yRotDiff = playerStatus.getLerpTarget().head.yRot - playerStatus.getLerpPrev().head.yRot;
                if (Math.abs(yRotDiff) < Math.PI / 2) {
                    playerModel.head.yRot = Mth.lerp(lerpFactor, playerStatus.getLerpPrev().head.yRot, playerStatus.getLerpTarget().head.yRot);
                }

                playerModel.head.xRot = Mth.lerp(lerpFactor, playerStatus.getLerpPrev().head.xRot, playerStatus.getLerpTarget().head.xRot);
                playerModel.head.zRot = Mth.lerp(lerpFactor, playerStatus.getLerpPrev().head.zRot, playerStatus.getLerpTarget().head.zRot);

                // Animazione di digitazione
                if (ConfigClient.showPlayerAnimation_Typing && ConfigServerControlledSyncedToClient.showPlayerAnimation_Typing && playerStatus.getPlayerChatState() == PlayerChatState.CHAT_TYPING) {
                    float amp = playerStatus.getTypingAmplifierSmooth();
                    float typeAngle = (float) Math.toRadians(Math.sin((pAgeInTicks * 1.0F) % 360) * 15 * amp);
                    float typeAngle2 = (float) Math.toRadians(-Math.sin((pAgeInTicks * 1.0F) % 360) * 15 * amp);
                    if (adjRightArm.x != Float.MAX_VALUE) playerModel.rightArm.xRot -= typeAngle;
                    if (adjLeftArm.x != Float.MAX_VALUE) playerModel.leftArm.xRot -= typeAngle2;
                }

                // Animazione Idle/AFK
                if (ConfigClient.showPlayerAnimation_Idle && ConfigServerControlledSyncedToClient.showPlayerAnimation_Idle && playerStatus.isIdle()) {
                    float angle = (float) Math.toRadians(Math.sin((pAgeInTicks * 0.05F) % 360) * 15);
                    float angle2 = (float) Math.toRadians(Math.cos((pAgeInTicks * 0.05F) % 360) * 7);
                    playerModel.head.xRot += angle2;
                    playerModel.head.zRot += angle;
                }
            }
        }
    }

    public static void setPoseTarget(PlayerStatus playerStatus, PlayerStatus playerStatusPrev, boolean becauseMousePress, int armMouseTickRate) {
        playerStatus.getLerpPrev().rightArm = playerStatus.getLerpTarget().rightArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().rightArm, playerStatus.lastPartialTick);
        playerStatus.getLerpPrev().leftArm = playerStatus.getLerpTarget().leftArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().leftArm, playerStatus.lastPartialTick);
        playerStatus.getLerpPrev().head = playerStatus.getLerpTarget().head.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().head, playerStatus.lastPartialTick);

        if (Float.isNaN(playerStatus.getLerpPrev().rightArm.yRot)) playerStatus.getLerpPrev().rightArm.yRot = 0;
        if (Float.isNaN(playerStatus.getLerpPrev().rightArm.xRot)) playerStatus.getLerpPrev().rightArm.xRot = 0;

        boolean pointing = PlayerGuiState.isPointingGui(playerStatus.getPlayerGuiState());
        boolean typing = playerStatus.getPlayerChatState() == PlayerChatState.CHAT_TYPING;
        boolean idle = playerStatus.isIdle();

        if (!ConfigClient.showPlayerAnimation_Gui || !ConfigServerControlledSyncedToClient.showPlayerAnimation_Gui) pointing = false;
        if (!ConfigClient.showPlayerAnimation_Typing || !ConfigServerControlledSyncedToClient.showPlayerAnimation_Typing) typing = false;
        if (!ConfigClient.showPlayerAnimation_Idle || !ConfigServerControlledSyncedToClient.showPlayerAnimation_Idle) idle = false;

        playerStatus.setNewLerp(becauseMousePress ? armMouseTickRate * 0.5F : armMouseTickRate * 1.0F);

        if (pointing || typing) {
            playerStatus.getLerpTarget().head.xRot = (float) Math.toRadians(15);
            playerStatus.getLerpTarget().head.yRot = 0;
        }

        if (pointing) {
            double xPercent = playerStatus.getScreenPosPercentX() * 0.6;
            double yPercent = playerStatus.getScreenPosPercentY() * 0.6;
            double x = Math.toRadians(90) - Math.toRadians(22.5) - yPercent;
            double y = -Math.toRadians(15) + xPercent;
            double xHead = Math.toRadians(22.5) + yPercent;
            double yHead = xPercent;

            playerStatus.getLerpTarget().rightArm.yRot = (float) y;
            playerStatus.getLerpTarget().rightArm.xRot = (float) -x;
            playerStatus.getLerpTarget().head.yRot = (float) yHead * 0.5F;
            playerStatus.getLerpTarget().head.xRot = (float) xHead * 0.5F;

            if (playerStatus.isPressing()) {
                Vec3 vec = calculateViewVector((float) Math.toDegrees(y), (float) Math.toDegrees(x));
                float press = 1.0F;
                playerStatus.getLerpTarget().rightArm.x = (float) (press * vec.x);
                playerStatus.getLerpTarget().rightArm.y = (float) (press * vec.y);
                playerStatus.getLerpTarget().rightArm.z = (float) (press * vec.z);
            } else {
                playerStatus.getLerpTarget().rightArm.x = 0;
                playerStatus.getLerpTarget().rightArm.y = 0;
                playerStatus.getLerpTarget().rightArm.z = 0;
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

        if (!pointing && !typing && !idle) {
            playerStatus.setLerpTarget(new Lerpables());
            playerStatus.getLerpTarget().head.xRot = playerStatus.xRotHeadBeforeOverriding;
            playerStatus.getLerpTarget().head.yRot = playerStatus.yRotHeadBeforeOverriding;
        }

        if (idle) {
            playerStatus.getLerpTarget().head.xRot = (float) Math.toRadians(70);
            playerStatus.setNewLerp(40);
        }

        if (playerStatusPrev != null && playerStatusPrev.getPlayerGuiState() == PlayerGuiState.NONE && playerStatus.getPlayerGuiState() != PlayerGuiState.NONE) {
            playerStatus.getLerpPrev().head.xRot = playerStatus.xRotHeadBeforeOverriding;
            playerStatus.getLerpPrev().head.yRot = playerStatus.yRotHeadBeforeOverriding;
        }
    }

    public static Vec3 calculateViewVector(float pXRot, float pYRot) {
        float f = pXRot * ((float) Math.PI / 180.0F);
        float f1 = -pYRot * ((float) Math.PI / 180.0F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3((double) (f3 * f4), (double) (-f5), (double) (f2 * f4));
    }
}
