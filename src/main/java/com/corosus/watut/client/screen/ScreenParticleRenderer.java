package com.corosus.watut.client.screen;

import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.JsonObjects.ScreenRule;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
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

    //used on client with gui open side
    private MainTarget mainRenderTarget;
    private MainTarget mainRenderTargetScaledDown;

    //used on receiving client side to render onto the particles texture
    private MainTarget mainRenderTargetScaledDownFromByteBuffer;

    public int width;
    public int height;
    public int widthScaledDown;
    public int heightScaledDown;
    public boolean needsInit = true;

    public long lastResizeTime = 0;
    public long lastRenderTime = 0;

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
        mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTarget.clear(Minecraft.ON_OSX);

        float aspectRatio = width / height;
        widthScaledDown = 512;
        //heightScaledDown = (int) (widthScaledDown / aspectRatio);
        heightScaledDown = 512;//(int) (widthScaledDown / aspectRatio);

        mainRenderTargetScaledDown = new MainTarget(widthScaledDown, heightScaledDown);
        mainRenderTargetScaledDown.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTargetScaledDown.clear(Minecraft.ON_OSX);

        mainRenderTargetScaledDownFromByteBuffer = new MainTarget(widthScaledDown, heightScaledDown);
        mainRenderTargetScaledDownFromByteBuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTargetScaledDownFromByteBuffer.clear(Minecraft.ON_OSX);

        //System.out.println("init with resolution: " + width + "x" + height);
        //System.out.println("init new framebuffer, texture id: " + mainRenderTarget.getColorTextureId());
    }

    public synchronized void resize(int width, int height) {
        System.out.println("resize");
        this.lastResizeTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        this.width = width;
        this.height = height;
        checkSetup();
        mainRenderTarget.resize(width, height, Minecraft.ON_OSX);

        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        widthScaledDown = (int) (this.width / guiScale);
        heightScaledDown = (int) (this.height / guiScale);
        float aspectRatio = (float)width / (float)height;
        widthScaledDown = 512;
        heightScaledDown = (int) (widthScaledDown / aspectRatio);
        heightScaledDown = 512;//(int) (widthScaledDown / aspectRatio);
        System.out.println("width: " + width + " height: " + height);
        System.out.println("widthScaledDown: " + widthScaledDown + " heightScaledDown: " + heightScaledDown + " - gui scale " + guiScale);
        mainRenderTargetScaledDown.resize(widthScaledDown, heightScaledDown, Minecraft.ON_OSX);
        mainRenderTargetScaledDownFromByteBuffer.resize(widthScaledDown, heightScaledDown, Minecraft.ON_OSX);

        //markNeedsNewRender(true);
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
        //RenderSystem.setShaderTexture(0, p_283461_);
        RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
        //RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShader(() -> PlayerStatusManagerClient.positionTexBlur);

        if (PlayerStatusManagerClient.positionTexBlur == null) {
            return;
        }
        //RenderSystem.setShaderTexture(0, p_283254_);
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
        /*bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();*/
        //flip vertically, required for some reason when moving from 1 framebuffer to another
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();
        /*bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283430_, (float)p_281729_).uv(p_283247_, p_282883_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_281399_, (float)p_283615_, (float)p_281729_).uv(p_283247_, p_283017_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283615_, (float)p_281729_).uv(p_282598_, p_283017_).endVertex();
        bufferbuilder.vertex(matrix4f, (float)p_283222_, (float)p_283430_, (float)p_281729_).uv(p_282598_, p_282883_).endVertex();*/
        BufferUploader.drawWithShader(bufferbuilder.end());
    }
}
