package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.PlayerStatusManagerClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;

import java.nio.ByteBuffer;

public class ScreenData {

    private String screenClass = "";
    private int textureID = -1;
    private ByteBuffer texturePixelData = null;
    private byte[] texturePixelDataPartial = null;

    private long gameTicksSinceFirstPacket = 0;
    private long gameTicksSinceLastScreenSend = 0;
    private long gameTicksSinceLastScreenReceiveAndRender = 0;

    private ParticleRenderType particleRenderType;

    private boolean needsNewRender = false;

    public void init() {

    }

    public void initClient() {

        this.particleRenderType = new ParticleRenderType() {
            public void begin(BufferBuilder p_107455_, TextureManager p_107456_) {
                RenderSystem.setShader(() -> PlayerStatusManagerClient.particle);
                RenderSystem.depthMask(false);
                RenderSystem._setShaderTexture(0, textureID);
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

    /*public synchronized void startCapture() {
        ScreenParticleRenderer.isCapturing = true;
        System.out.println("capture started");

        if (Minecraft.getInstance().screen != null) {
            screenClass = Minecraft.getInstance().screen.getClass().getCanonicalName();
        } else {
            CULog.dbg("watut screen capture started but screen is null?");
        }
    }*/

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

    public long getGameTicksSinceFirstPacket() {
        return gameTicksSinceFirstPacket;
    }

    public void setGameTicksSinceFirstPacket(long gameTicksSinceFirstPacket) {
        this.gameTicksSinceFirstPacket = gameTicksSinceFirstPacket;
    }

    public long getGameTicksSinceLastScreenSend() {
        return gameTicksSinceLastScreenSend;
    }

    public void setGameTicksSinceLastScreenSend(long gameTicksSinceLastScreenSend) {
        this.gameTicksSinceLastScreenSend = gameTicksSinceLastScreenSend;
    }

    public long getGameTicksSinceLastScreenReceiveAndRender() {
        return gameTicksSinceLastScreenReceiveAndRender;
    }

    public void setGameTicksSinceLastScreenReceiveAndRender(long gameTicksSinceLastScreenReceiveAndRender) {
        this.gameTicksSinceLastScreenReceiveAndRender = gameTicksSinceLastScreenReceiveAndRender;
    }
}
