package com.corosus.watut.client.screen;

import com.corosus.watut.WatutMod;
import com.corosus.watut.client.animation.PlayerAnimator;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.status.PlayerGuiState;
import com.corosus.watut.status.PlayerStatus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * Renderer GPU-Safe ad alte prestazioni per la GUI olografica 3D dinamica fluttuante a mezz'aria.
 */
public class DynamicScreenRenderer {

    private static final Identifier MOUSE_CURSOR_TEXTURE = Identifier.fromNamespaceAndPath("watut", "textures/misc/mouse.png");

    public static void render(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        if (ConfigServerControlledSyncedToClient.dynamicGuiUseOldSimpleGUIVisual) return;
        if (ConfigClient.dontSendDetailedGUIInfo) return;

        SubmitNodeCollector submitCollector = context.submitNodeCollector();
        PoseStack poseStack = context.poseStack();
        Camera camera = mc.gameRenderer.mainCamera();
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        Vec3 cameraPos = camera.position();

        for (PlayerStatus status : com.corosus.watut.client.WatutClientMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
            UUID uuid = status.getUuid();
            if (uuid == null) continue;

            Player player = mc.level.getPlayerByUUID(uuid);
            if (player == null || player.isInvisible() || player.isSwimming() || player.isFallFlying()) continue;

            // In prima persona, non renderizziamo la nostra GUI
            if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }

            if (!ConfigClient.showGuisForYourOwnPlayerIn3rdPerson && player == mc.player) {
                continue;
            }

            PlayerGuiState guiState = status.getPlayerGuiState();
            if (guiState == PlayerGuiState.NONE || guiState == PlayerGuiState.CHAT_SCREEN) {
                continue;
            }

            ScreenData screenData = status.getScreenData();
            if (!screenData.isTextureReady() || screenData.getImage() == null) {
                continue;
            }

            Identifier screenTexture = screenData.getTextureIdentifier(uuid);
            if (screenTexture == null) continue;

            double distToCamera = cameraPos.distanceTo(player.position());
            double maxDist = ConfigServerControlledSyncedToClient.distanceRequiredToShowGUIInfo;
            if (distToCamera > maxDist) continue;

            // Calcolo Alpha con dissolvenza di prossimità
            float alpha = 1.0F;
            if (distToCamera > maxDist * 0.7D) {
                alpha = (float) ((maxDist - distToCamera) / (maxDist * 0.3D));
            }
            alpha = Mth.clamp(alpha, 0.05F, 0.95F);
            final float finalAlpha = alpha;

            poseStack.pushPose();

            // Posizionamento ologramma fluttuante a mezz'aria di fronte al giocatore
            double px = Mth.lerp(partialTicks, player.xOld, player.getX());
            double py = Mth.lerp(partialTicks, player.yOld, player.getY());
            double pz = Mth.lerp(partialTicks, player.zOld, player.getZ());

            float distFromPlayer = 0.95F;
            Vec3 lookVec = PlayerAnimator.calculateViewVector(0, player.yBodyRot).scale(distFromPlayer);

            double crouchOffset = player.isCrouching() ? -0.22D : 0.0D;
            double screenWorldX = px + lookVec.x;
            double screenWorldY = py + 1.25D + crouchOffset;
            double screenWorldZ = pz + lookVec.z;

            poseStack.translate(screenWorldX - cameraPos.x, screenWorldY - cameraPos.y, screenWorldZ - cameraPos.z);

            // Rotazione solidale con il corpo del giocatore
            poseStack.mulPose(Axis.YP.rotationDegrees(-player.yBodyRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(20.0F));

            // Calcolo direzione della telecamera rispetto allo schermo per evitare sovrapposizioni (culling unilaterale)
            Vec3 toCamera = cameraPos.subtract(screenWorldX, screenWorldY, screenWorldZ);
            Vec3 frontNormal = PlayerAnimator.calculateViewVector(-20.0F, player.yBodyRot);
            boolean isViewingFromFront = toCamera.dot(frontNormal) >= 0.0D;

            // Scala proporzionale in base all'Aspect Ratio esatto della GUI
            int texW = Math.max(16, screenData.getWidth());
            int texH = Math.max(16, screenData.getHeight());
            float aspectRatio = (float) texW / (float) texH;

            float baseScale = (float) (0.65F * ConfigClient.particleSizeScale);
            float widthHalf;
            float heightHalf;

            if (aspectRatio >= 1.0F) {
                widthHalf = baseScale * 0.5F;
                heightHalf = (baseScale * 0.5F) / aspectRatio;
            } else {
                heightHalf = baseScale * 0.5F;
                widthHalf = (baseScale * 0.5F) * aspectRatio;
            }

            int light = 0x00F000F0; // Piena luminosità olografica

            // Invio geometria personalizzata per la schermata GUI (Mostriamo SOLO la faccia orientata verso l'osservatore)
            RenderType screenRenderType = RenderTypes.entityTranslucent(screenTexture);
            submitCollector.submitCustomGeometry(poseStack, screenRenderType, (pose, vertexConsumer) -> {
                Matrix4f mat = pose.pose();
                if (isViewingFromFront) {
                    // Faccia Frontale dritta per chi guarda frontalmente
                    drawQuad(vertexConsumer, mat, -widthHalf, widthHalf, -heightHalf, heightHalf, 0.0F, 1.0F, 1.0F, 1.0F, finalAlpha, 0, 0, 1, 1, light, false);
                } else {
                    // Faccia Posteriore dritta e leggibile per chi guarda da dietro
                    drawQuad(vertexConsumer, mat, -widthHalf, widthHalf, -heightHalf, heightHalf, 0.0F, 1.0F, 1.0F, 1.0F, finalAlpha * 0.9F, 0, 0, 1, 1, light, true);
                }
            });

            // Invio geometria personalizzata per il cursore del mouse
            if (status.getScreenPosPercentX() != 0.0F || status.getScreenPosPercentY() != 0.0F) {
                float cursorNormX = Mth.clamp(status.getScreenPosPercentX(), -1.0F, 1.0F);
                float cursorNormY = Mth.clamp(status.getScreenPosPercentY(), -1.0F, 1.0F);

                float cursorX = cursorNormX * widthHalf;
                float cursorY = -cursorNormY * heightHalf;
                float cursorSize = 0.04F;

                RenderType cursorRenderType = RenderTypes.entityTranslucent(MOUSE_CURSOR_TEXTURE);
                submitCollector.submitCustomGeometry(poseStack, cursorRenderType, (pose, vertexConsumer) -> {
                    Matrix4f mat = pose.pose();
                    if (isViewingFromFront) {
                        drawQuad(vertexConsumer, mat, cursorX, cursorX + cursorSize, cursorY - cursorSize, cursorY, 0.002F, 1.0F, 1.0F, 1.0F, finalAlpha, 0, 0, 1, 1, light, false);
                    } else {
                        float backCursorX = -cursorX;
                        drawQuad(vertexConsumer, mat, backCursorX - cursorSize, backCursorX, cursorY - cursorSize, cursorY, -0.002F, 1.0F, 1.0F, 1.0F, finalAlpha * 0.9F, 0, 0, 1, 1, light, true);
                    }
                });
            }

            poseStack.popPose();
        }
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f mat, float x0, float x1, float y0, float y1, float z, float r, float g, float b, float a, float u0, float v0, float u1, float v1, int light, boolean backFace) {
        float normalZ = backFace ? -1.0F : 1.0F;
        if (!backFace) {
            consumer.addVertex(mat, x0, y0, z).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, normalZ);
            consumer.addVertex(mat, x1, y0, z).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, normalZ);
            consumer.addVertex(mat, x1, y1, z).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, normalZ);
            consumer.addVertex(mat, x0, y1, z).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, normalZ);
        } else {
            // Faccia posteriore: invertiamo orizzontalmente i vertici in modo che chi guarda da dietro veda la GUI dritta (non specchiata)
            consumer.addVertex(mat, x1, y1, z).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, normalZ);
            consumer.addVertex(mat, x0, y1, z).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, normalZ);
            consumer.addVertex(mat, x0, y0, z).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, normalZ);
            consumer.addVertex(mat, x1, y0, z).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, normalZ);
        }
    }
}
