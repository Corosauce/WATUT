package com.corosus.watut.client.screen;

import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerSyncedToClient;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

public class ScreenParticleRenderer {

    public static boolean isRenderingParticleGUI = false;
    public static boolean isRenderingParticleGUI2 = false;

    //used on client with gui open side
    private MainTarget mainRenderTarget;
    private MainTarget mainRenderTargetScaledDown;

    //used on receiving client side to render each other clients screen data onto the particles texture
    private MainTarget mainRenderTargetScaledDownFromByteBuffer;

    public int width;
    public int height;
    public int widthScaledDown = 512;
    public int heightScaledDown = 512;
    public boolean needsInit = true;

    private static ScreenParticleRenderer instance;

    public static ScreenParticleRenderer getInstance() {
        if (instance == null) {
            instance = new ScreenParticleRenderer();
        }
        return instance;
    }

    public void init() {

    }

    public void checkSetup() {
        if (needsInit) {
            needsInit = false;
            setup();
        }
    }

    public void setup() {
        Minecraft mc = Minecraft.getInstance();
        width = mc.getWindow().getWidth();
        height = mc.getWindow().getHeight();
        mainRenderTarget = new MainTarget(width, height);
        mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTarget.clear(Minecraft.ON_OSX);

        mainRenderTargetScaledDown = new MainTarget(widthScaledDown, heightScaledDown);
        mainRenderTargetScaledDown.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTargetScaledDown.clear(Minecraft.ON_OSX);

        mainRenderTargetScaledDownFromByteBuffer = new MainTarget(widthScaledDown, heightScaledDown);
        mainRenderTargetScaledDownFromByteBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTargetScaledDownFromByteBuffer.clear(Minecraft.ON_OSX);
    }

    public synchronized void resize(int width, int height) {
        this.width = width;
        this.height = height;
        checkSetup();
        mainRenderTarget.resize(width, height, Minecraft.ON_OSX);
    }

    public void bind() {
        mainRenderTarget.bindWrite(true);
    }

    public void unbind() {
        mainRenderTarget.unbindWrite();
    }

    public void bindScaledDown() {
        mainRenderTargetScaledDown.bindWrite(true);
    }

    public void unbindScaledDown() {
        mainRenderTargetScaledDown.unbindWrite();
    }

    public void bindScaledDownFromByteBuffer() {
        mainRenderTargetScaledDownFromByteBuffer.bindWrite(true);
    }

    public void unbindScaledDownFromByteBuffer() {
        mainRenderTargetScaledDownFromByteBuffer.unbindWrite();
    }

    public MainTarget getMainRenderTarget() {
        return mainRenderTarget;
    }

    public MainTarget getMainRenderTargetScaledDown() {
        return mainRenderTargetScaledDown;
    }

    public MainTarget getMainRenderTargetScaledDownFromByteBuffer() {
        return mainRenderTargetScaledDownFromByteBuffer;
    }

    public void setMainRenderTarget(MainTarget mainRenderTarget) {
        this.mainRenderTarget = mainRenderTarget;
    }

    public void innerBlitCustomShader(PoseStack pose, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_) {
        RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShader(() -> PlayerStatusManagerClient.positionTexBlur);

        if (PlayerStatusManagerClient.positionTexBlur == null) {
            return;
        }
        if (PlayerStatusManagerClient.positionTexBlur.RESOLUTION != null) {
            int sizeX = ScreenParticleRenderer.getInstance().widthScaledDown;
            int sizeY = ScreenParticleRenderer.getInstance().heightScaledDown;
            PlayerStatusManagerClient.positionTexBlur.RESOLUTION.set((float)sizeX, (float)sizeY);
        }
        if (PlayerStatusManagerClient.positionTexBlur.RADIUS != null) {
            PlayerStatusManagerClient.positionTexBlur.RADIUS.set((float)0);
        }

        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        //UV coordinates adjusted to fix upside down render from data from earlier, cant figure out why its backwards to begin with but we fixed it via UV here
        // Bottom-left vertex
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
        // Top-left vertex
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
        // Top-right vertex
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();
        // Bottom-right vertex
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();

        BufferUploader.drawWithShader(bufferbuilder.end());
    }

    public void innerBlitCustomShader2(int textureID, PoseStack pose, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_) {
        //RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem._setShaderTexture(0, textureID);
        RenderSystem.setShader(() -> PlayerStatusManagerClient.positionTexBlur);

        if (PlayerStatusManagerClient.positionTexBlur == null) {
            return;
        }
        if (PlayerStatusManagerClient.positionTexBlur.RESOLUTION != null) {
            int sizeX = ScreenParticleRenderer.getInstance().widthScaledDown;
            int sizeY = ScreenParticleRenderer.getInstance().heightScaledDown;
            PlayerStatusManagerClient.positionTexBlur.RESOLUTION.set((float)sizeX, (float)sizeY);
        }
        if (PlayerStatusManagerClient.positionTexBlur.RADIUS != null) {
            PlayerStatusManagerClient.positionTexBlur.RADIUS.set((float)0);
        }

        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        //UV coordinates adjusted to fix upside down render from data from earlier, cant figure out why its backwards to begin with but we fixed it via UV here
        // Bottom-left vertex
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
        // Top-left vertex
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
        // Top-right vertex
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();
        // Bottom-right vertex
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();

        BufferUploader.drawWithShader(bufferbuilder.end());
    }

    public void innerBlitCustomShaderHorizontal(PoseStack pose, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_) {
        if (RenderHelper.xaeroWorldMapTextureID != -1) {
            //RenderSystem._setShaderTexture(0, RenderHelper.xaeroWorldMapTextureID);
            GlStateManager._bindTexture(RenderHelper.xaeroWorldMapTextureID);
            RenderSystem.setShaderTexture(0, RenderHelper.xaeroWorldMapTextureID);
            /*int test = 181;
            GlStateManager._bindTexture(test);
            RenderSystem.setShaderTexture(0, test);*/
        } else {
            RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        }
        //RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShader(() -> PlayerStatusManagerClient.positionTexBlurHorizontal);

        if (PlayerStatusManagerClient.positionTexBlurHorizontal == null) {
            return;
        }

        if (PlayerStatusManagerClient.positionTexBlurHorizontal.BLUR_LEVEL != null) {
            PlayerStatusManagerClient.positionTexBlurHorizontal.BLUR_LEVEL.set((float)(RenderHelper.xaeroWorldMapTextureID != -1 ? 0 : ConfigServerSyncedToClient.blurLevel));
        }

        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        if (RenderHelper.xaeroWorldMapTextureID != -1) {
            bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
            bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
            bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();
            bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();
        } else {
            //UV coordinates adjusted to fix upside down render from data from earlier, cant figure out why its backwards to begin with but we fixed it via UV here
            // Bottom-left vertex
            bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
            // Top-left vertex
            bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
            // Top-right vertex
            bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();
            // Bottom-right vertex
            bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();
        }



        BufferUploader.drawWithShader(bufferbuilder.end());
    }

    public void innerBlitCustomShaderVertical(PoseStack pose, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_) {
        RenderSystem._setShaderTexture(0, mainRenderTargetScaledDown.getColorTextureId());
        //RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShader(() -> PlayerStatusManagerClient.positionTexBlurVertical);

        if (PlayerStatusManagerClient.positionTexBlurVertical == null) {
            return;
        }
        if (PlayerStatusManagerClient.positionTexBlurVertical.RESOLUTION != null) {
            int sizeX = ScreenParticleRenderer.getInstance().widthScaledDown;
            int sizeY = ScreenParticleRenderer.getInstance().heightScaledDown;
            PlayerStatusManagerClient.positionTexBlurVertical.RESOLUTION.set((float)sizeX, (float)sizeY);
        }

        //visual cutoff radius
        if (PlayerStatusManagerClient.positionTexBlurVertical.RADIUS != null) {
            PlayerStatusManagerClient.positionTexBlurVertical.RADIUS.set((float) ConfigServerSyncedToClient.sizeRadiusInPixelsToShow);
        }

        if (PlayerStatusManagerClient.positionTexBlurVertical.BLUR_LEVEL != null) {
            PlayerStatusManagerClient.positionTexBlurVertical.BLUR_LEVEL.set((float)ConfigServerSyncedToClient.blurLevel);
        }

        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        //UV coordinates adjusted to fix upside down render from data from earlier, cant figure out why its backwards to begin with but we fixed it via UV here
        // Bottom-left vertex
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
        // Top-left vertex
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
        // Top-right vertex
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();
        // Bottom-right vertex
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();

        BufferUploader.drawWithShader(bufferbuilder.end());
    }
}
