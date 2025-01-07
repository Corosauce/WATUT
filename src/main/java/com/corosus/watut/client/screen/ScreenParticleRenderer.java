package com.corosus.watut.client.screen;

import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.JsonObjects.ScreenRule;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ScreenParticleRenderer {

    public static boolean isCapturing = false;
    public static boolean isRenderingParticleGUI = false;
    public static boolean isRenderingParticleGUI2 = false;

    private boolean needsNewRender = false;
    private MainTarget mainRenderTarget;

    private ParticleRenderType particleRenderType;

    public int width;
    public int height;
    public boolean needsInit = true;

    private static ScreenParticleRenderer instance;

    public static ScreenParticleRenderer getInstance() {
        if (instance == null) {
            instance = new ScreenParticleRenderer();
        }
        return instance;
    }

    public static synchronized boolean isCapturing() {
        return isCapturing;
    }

    public static synchronized boolean isRenderingParticleGUI() {
        return isRenderingParticleGUI;
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
        //mainRenderTarget = new CustomRenderTarget(width, height, true);
        mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTarget.clear(Minecraft.ON_OSX);

        //System.out.println("init with resolution: " + width + "x" + height);
        //System.out.println("init new framebuffer, texture id: " + mainRenderTarget.getColorTextureId());

        this.particleRenderType = new ParticleRenderType() {
            public void begin(BufferBuilder p_107455_, TextureManager p_107456_) {
                RenderSystem.depthMask(true);
                //RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
                //RenderSystem.bindTexture(ScreenCapturing.mainRenderTarget.getColorTextureId());
                RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();
                p_107455_.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            }

            public void end(Tesselator p_107458_) {
                p_107458_.end();
                RenderSystem.enableCull();
            }

            public String toString() {
                return "DYNAMIC_TEXTURE";
            }
        };
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        checkSetup();
        mainRenderTarget.resize(width, height, Minecraft.ON_OSX);
        markNeedsNewRender(true);
    }

    public void bind() {
        mainRenderTarget.bindWrite(true);
    }

    public void unbind() {
        mainRenderTarget.unbindWrite();
    }

    public synchronized boolean needsNewRender() {
        return needsNewRender;
    }

    public synchronized void markNeedsNewRender(boolean needsNewRender) {
        this.needsNewRender = needsNewRender;
    }

    public MainTarget getMainRenderTarget() {
        return mainRenderTarget;
    }

    public void setMainRenderTarget(MainTarget mainRenderTarget) {
        this.mainRenderTarget = mainRenderTarget;
    }

    public ParticleRenderType getParticleRenderType() {
        return particleRenderType;
    }

    public void setParticleRenderType(ParticleRenderType particleRenderType) {
        this.particleRenderType = particleRenderType;
    }

    public void innerBlit(ResourceLocation p_283461_, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_, PoseStack pose, ScreenRule screenRule) {
        if (PlayerStatusManagerClient.positionTexBlur == null) {
            return;
        }
        RenderSystem.setShaderTexture(0, p_283461_);
        RenderSystem.setShader(() -> PlayerStatusManagerClient.positionTexBlur);
        if (PlayerStatusManagerClient.positionTexBlur.RESOLUTION != null) {
            int sizeX = 256;
            int sizeY = 256;
            if (screenRule != null) {
                sizeX = screenRule.getTextureSize()[0];
                sizeY = screenRule.getTextureSize()[1];
            }
            PlayerStatusManagerClient.positionTexBlur.RESOLUTION.set((float)sizeX, (float)sizeY);
        }
        if (PlayerStatusManagerClient.positionTexBlur.RADIUS != null) {
            /*int blur = 2;
            if (Minecraft.getInstance().level != null) {

                blur = (int) ((Minecraft.getInstance().level.getGameTime() * 0.5) % 10);
                float blurFloat = (float) (Math.sin(((int)(Minecraft.getInstance().level.getGameTime() * 0.3)) * 0.1F * 360) * 10);
                blur = Mth.clamp((int)blurFloat + 10, 0, 10);
                int sdfsdfs = 0;
            }*/
            PlayerStatusManagerClient.positionTexBlur.RADIUS.set((float)1);
        }
        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
    }

    public void innerBlit(ResourceLocation p_283254_, int p_283092_, int p_281930_, int p_282113_, int p_281388_, int p_283583_, float p_281327_, float p_281676_, float p_283166_, float p_282630_, float p_282800_, float p_282850_, float p_282375_, float p_282754_, PoseStack pose, ScreenRule screenRule) {
        if (PlayerStatusManagerClient.positionColorTexBlur == null) {
            return;
        }
        RenderSystem.setShaderTexture(0, p_283254_);
        RenderSystem.setShader(() -> PlayerStatusManagerClient.positionColorTexBlur);
        if (PlayerStatusManagerClient.positionTexBlur.RESOLUTION != null) {
            int sizeX = 256;
            int sizeY = 256;
            if (screenRule != null) {
                sizeX = screenRule.getTextureSize()[0];
                sizeY = screenRule.getTextureSize()[1];
            }
            PlayerStatusManagerClient.positionTexBlur.RESOLUTION.set((float)sizeX, (float)sizeY);
        }
        if (PlayerStatusManagerClient.positionTexBlur.RADIUS != null) {
            PlayerStatusManagerClient.positionTexBlur.RADIUS.set((float)2);
        }
        RenderSystem.enableBlend();
        Matrix4f matrix4f = pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        bufferbuilder.vertex(matrix4f, (float)p_283092_, (float)p_282113_, (float)p_283583_).color(p_282800_, p_282850_, p_282375_, p_282754_).uv(p_281327_, p_283166_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_283092_, (float)p_281388_, (float)p_283583_).color(p_282800_, p_282850_, p_282375_, p_282754_).uv(p_281327_, p_282630_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_281930_, (float)p_281388_, (float)p_283583_).color(p_282800_, p_282850_, p_282375_, p_282754_).uv(p_281676_, p_282630_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_281930_, (float)p_282113_, (float)p_283583_).color(p_282800_, p_282850_, p_282375_, p_282754_).uv(p_281676_, p_283166_).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
        RenderSystem.disableBlend();
    }
}
