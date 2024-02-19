package com.corosus.watut.client.screen;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;

import java.util.ArrayList;
import java.util.List;

public class ScreenData {

    private boolean isCapturing = false;
    private List<RenderCall> listRenderCalls = new ArrayList<>();
    private boolean needsNewRender = false;
    private MainTarget mainRenderTarget;

    private ParticleRenderType particleRenderType;

    public int width = 1920;
    public int height = 1080;
    public boolean needsInit = true;

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
        //TODO: TEMP
        //height = width;
        mainRenderTarget = new MainTarget(width, height);
        //mainRenderTarget = new CustomRenderTarget(width, height, true);
        mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTarget.clear(Minecraft.ON_OSX);

        //System.out.println("init with resolution: " + width + "x" + height);
        System.out.println("init new framebuffer, texture id: " + mainRenderTarget.getColorTextureId());

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

    public void bind() {
        mainRenderTarget.bindWrite(true);
    }

    public void unbind() {
        mainRenderTarget.unbindWrite();
    }

    public void startCapture() {
        listRenderCalls.clear();
        isCapturing = true;
        System.out.println("capture started");
    }

    public void stopCapture() {
        isCapturing = false;
        System.out.println("capture stopped - captured call count: " + listRenderCalls.size());
    }

    public void addRenderCall(RenderCall renderCall) {
        listRenderCalls.add(renderCall);
    }

    public boolean isCapturing() {
        return isCapturing;
    }

    public void setCapturing(boolean capturing) {
        isCapturing = capturing;
    }

    public List<RenderCall> getListRenderCalls() {
        return listRenderCalls;
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
}
