package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.config.JSONLoader;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ScreenData {

    private String screenClass = "";
    private int textureID = -1;
    private ByteBuffer texturePixelData = null;
    private byte[] texturePixelDataPartial = null;

    private long gameticksSinceFirstPacket = 0;

    private ParticleRenderType particleRenderType;

    private boolean needsNewRender = false;

    public void init() {



    }

    public void initClient() {

        this.particleRenderType = new ParticleRenderType() {
            public void begin(BufferBuilder p_107455_, TextureManager p_107456_) {
                RenderSystem.setShader(() -> PlayerStatusManagerClient.particle);
                RenderSystem.depthMask(false);
                //RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
                //RenderSystem.bindTexture(ScreenCapturing.mainRenderTarget.getColorTextureId());
                //RenderSystem._setShaderTexture(0, mainRenderTargetScaledDownFromByteBuffer.getColorTextureId());
                RenderSystem._setShaderTexture(0, textureID);
                //RenderSystem._setShaderTexture(0, mainRenderTarget.getColorTextureId());
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

    public synchronized void startCapture() {
        ScreenParticleRenderer.isCapturing = true;
        //System.out.println("capture started");

        if (Minecraft.getInstance().screen != null) {
            screenClass = Minecraft.getInstance().screen.getClass().getCanonicalName();
        } else {
            CULog.dbg("watut screen capture started but screen is null?");
        }
    }

    public synchronized void stopCapture() {
        ScreenParticleRenderer.isCapturing = false;
        //System.out.println("capture stopped - captured call count: " + listRenderCalls.size());
    }

    public synchronized boolean isCapturing() {
        return ScreenParticleRenderer.isCapturing;
    }

    public synchronized void setCapturing(boolean capturing) {
        ScreenParticleRenderer.isCapturing = capturing;
    }

    public String getScreenClass() {
        return screenClass;
    }

    public void setScreenClass(String screenClass) {
        this.screenClass = screenClass;
    }

    public int getTextureID() {
        return textureID;
    }

    public void setTextureID(int textureID) {
        this.textureID = textureID;
    }

    public ByteBuffer getTexturePixelData() {
        return texturePixelData;
    }

    public void setTexturePixelData(ByteBuffer texturePixelData) {
        this.texturePixelData = texturePixelData;
    }

    public ParticleRenderType getParticleRenderType() {
        return particleRenderType;
    }

    public void setParticleRenderType(ParticleRenderType particleRenderType) {
        this.particleRenderType = particleRenderType;
    }

    public synchronized boolean needsNewRender() {
        return needsNewRender;
    }

    public synchronized void markNeedsNewRender(boolean needsNewRender) {
        this.needsNewRender = needsNewRender;
    }

    public byte[] getTexturePixelDataPartial() {
        return texturePixelDataPartial;
    }

    public void setTexturePixelDataPartial(byte[] texturePixelDataPartial) {
        this.texturePixelDataPartial = texturePixelDataPartial;
    }

    public long getGameticksSinceFirstPacket() {
        return gameticksSinceFirstPacket;
    }

    public void setGameticksSinceFirstPacket(long gameticksSinceFirstPacket) {
        this.gameticksSinceFirstPacket = gameticksSinceFirstPacket;
    }
}
