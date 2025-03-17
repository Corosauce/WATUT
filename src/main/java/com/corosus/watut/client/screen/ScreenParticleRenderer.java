package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class ScreenParticleRenderer {

    public static boolean isRenderingParticleGUI = false;
    public static boolean isRenderingParticleGUI2 = false;

    //used on client with gui open side
    //used to capture raw copy of minecraft screen
    private MainTarget mainRenderTarget;

    //used to render a sized down and cropped version of the above raw copy
    private MainTarget mainRenderTargetScaledDown;

    public int width;
    public int height;
    public static int defaultWidthScaledDown = 256;
    public static int defaultHeightScaledDown = 256;
    public static int bytesPerPixel = 4;
    public int widthScaledDown = defaultWidthScaledDown;
    public int heightScaledDown = defaultHeightScaledDown;
    public boolean needsInit = true;

    private static ScreenParticleRenderer instance;

    public static ScreenParticleRenderer getInstance() {
        if (instance == null) {
            instance = new ScreenParticleRenderer();
        }
        return instance;
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
        mainRenderTarget.clear();

        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            widthScaledDown = width;
            heightScaledDown = height;
        } else {
            widthScaledDown = defaultWidthScaledDown;
            heightScaledDown = defaultHeightScaledDown;
        }

        mainRenderTargetScaledDown = new MainTarget(widthScaledDown, heightScaledDown);
        mainRenderTargetScaledDown.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTargetScaledDown.clear();
    }

    public synchronized void resize(int width, int height) {
        this.width = width;
        this.height = height;
        checkSetup();
        mainRenderTarget.resize(width, height);
        resizeScaledDown(width, height);
    }

    public void resizeScaledDown(int width, int height) {
        checkSetup();
        int widthToUse = defaultWidthScaledDown;
        int heightToUse = defaultHeightScaledDown;
        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            widthToUse = width;
            heightToUse = height;
        }

        widthScaledDown = widthToUse;
        heightScaledDown = heightToUse;

        CULog.dbg("resizeScaledDown to " + widthToUse + " " + heightToUse);

        if (mainRenderTargetScaledDown.width != widthToUse || mainRenderTargetScaledDown.height != heightToUse) {
            mainRenderTargetScaledDown.resize(widthToUse, heightToUse);
        }
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

    public MainTarget getMainRenderTarget() {
        return mainRenderTarget;
    }

    public MainTarget getMainRenderTargetScaledDown() {
        return mainRenderTargetScaledDown;
    }

    public void setMainRenderTarget(MainTarget mainRenderTarget) {
        this.mainRenderTarget = mainRenderTarget;
    }

    public void innerBlitCustomShader(PoseStack pose, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_) {
        RenderSystem.setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShader(PlayerStatusManagerClient.positionTexBlur.getProgram());

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
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        //bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        //UV coordinates adjusted to fix upside down render from data from earlier, cant figure out why its backwards to begin with but we fixed it via UV here
        // Bottom-left vertex
        bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).setUv(p_283247_, p_283017_);
        // Top-left vertex
        bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).setUv(p_283247_, p_282883_);
        // Top-right vertex
        bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).setUv(p_282598_, p_282883_);
        // Bottom-right vertex
        bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).setUv(p_282598_, p_283017_);

        BufferUploader.drawWithShader(bufferbuilder.build());
    }

    public void innerBlitCustomShader2(int textureID, PoseStack pose, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_) {
        //RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShader(PlayerStatusManagerClient.positionTexBlur.getProgram());

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
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        //bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        //UV coordinates adjusted to fix upside down render from data from earlier, cant figure out why its backwards to begin with but we fixed it via UV here
        // Bottom-left vertex
        bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).setUv(p_283247_, p_283017_);
        // Top-left vertex
        bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).setUv(p_283247_, p_282883_);
        // Top-right vertex
        bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).setUv(p_282598_, p_282883_);
        // Bottom-right vertex
        bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).setUv(p_282598_, p_283017_);

        BufferUploader.drawWithShader(bufferbuilder.build());
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
            RenderSystem.setShaderTexture(0, mainRenderTarget.getColorTextureId());
        }
        //RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShader(PlayerStatusManagerClient.positionTexBlurHorizontal.getProgram());

        if (PlayerStatusManagerClient.positionTexBlurHorizontal == null) {
            return;
        }

        if (PlayerStatusManagerClient.positionTexBlurHorizontal.BLUR_LEVEL != null) {
            PlayerStatusManagerClient.positionTexBlurHorizontal.BLUR_LEVEL.set((float)(RenderHelper.xaeroWorldMapTextureID != -1 ? 0 : ConfigServerControlledSyncedToClient.dynamicGuiBlurLevel));
        }

        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        //bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        if (RenderHelper.xaeroWorldMapTextureID != -1) {
            bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).setUv(p_283247_, p_282883_);
            bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).setUv(p_283247_, p_283017_);
            bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).setUv(p_282598_, p_283017_);
            bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).setUv(p_282598_, p_282883_);
        } else {
            //UV coordinates adjusted to fix upside down render from data from earlier, cant figure out why its backwards to begin with but we fixed it via UV here
            // Bottom-left vertex
            bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).setUv(p_283247_, p_283017_);
            // Top-left vertex
            bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).setUv(p_283247_, p_282883_);
            // Top-right vertex
            bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).setUv(p_282598_, p_282883_);
            // Bottom-right vertex
            bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).setUv(p_282598_, p_283017_);
        }



        BufferUploader.drawWithShader(bufferbuilder.build());
    }

    public void innerBlitCustomShaderVertical(PoseStack pose, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_) {
        RenderSystem.setShaderTexture(0, mainRenderTargetScaledDown.getColorTextureId());
        //RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShader(PlayerStatusManagerClient.positionTexBlurVertical.getProgram());

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
            PlayerStatusManagerClient.positionTexBlurVertical.RADIUS.set((float) ConfigServerControlledSyncedToClient.dynamicGuiSizeRadiusInPixelsToShow);
        }

        if (PlayerStatusManagerClient.positionTexBlurVertical.BLUR_LEVEL != null) {
            PlayerStatusManagerClient.positionTexBlurVertical.BLUR_LEVEL.set((float)(RenderHelper.xaeroWorldMapTextureID != -1 ? 0 : ConfigServerControlledSyncedToClient.dynamicGuiBlurLevel));
        }

        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        //bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        //UV coordinates adjusted to fix upside down render from data from earlier, cant figure out why its backwards to begin with but we fixed it via UV here
        // Bottom-left vertex
        bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).setUv(p_283247_, p_283017_);
        // Top-left vertex
        bufferbuilder.addVertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).setUv(p_283247_, p_282883_);
        // Top-right vertex
        bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).setUv(p_282598_, p_282883_);
        // Bottom-right vertex
        bufferbuilder.addVertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).setUv(p_282598_, p_283017_);

        BufferUploader.drawWithShader(bufferbuilder.build());
    }

    //copy of GuiGraphics.innerBlit with PoseStack added
    public void innerBlit(PoseStack pose, ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        //RenderSystem.setShaderTexture(0, mainRenderTarget.getColorTextureId());
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        //bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y1, (float)blitOffset).setUv(minU, minV);
        bufferBuilder.addVertex(matrix4f, (float)x1, (float)y2, (float)blitOffset).setUv(minU, maxV);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y2, (float)blitOffset).setUv(maxU, maxV);
        bufferBuilder.addVertex(matrix4f, (float)x2, (float)y1, (float)blitOffset).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferBuilder.build());
    }
}
