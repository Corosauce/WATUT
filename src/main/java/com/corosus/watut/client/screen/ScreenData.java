package com.corosus.watut.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenData {

    private volatile ByteBuffer texturePixelData = null;
    private volatile ByteBuffer decompressionBuffer = null;
    private final AtomicBoolean isBufferReady = new AtomicBoolean(false);
    private byte[] texturePixelDataPartial = null;

    private long gameTicksSinceFirstPacket = 0;
    private int lastIndexReceived = 0;
    private long gameTicksSinceLastScreenSend = 0;
    private long gameTicksSinceLastScreenReceiveAndRender = 0;

    private Object particleRenderType;

    private boolean needsNewRenderFromPixelData = false;

    //used for communicating from outside screen render hook to inside it
    private boolean needsNewRenderToPixelData = false;

    private DynamicTexture image = null;
    private int width = ScreenParticleRenderer.defaultWidthScaledDown;
    private int height = ScreenParticleRenderer.defaultHeightScaledDown;

    private Screen lastScreen;

    public void initClient() {
    }

    public ByteBuffer getTexturePixelData() {
        return texturePixelData;
    }

    public void freeTexturePixelData() {
        if (texturePixelData != null) {
            MemoryUtil.memFree(texturePixelData);
        }
    }

    public void setTexturePixelData(ByteBuffer texturePixelData) {
        this.texturePixelData = texturePixelData;
    }

    public Object getParticleRenderType() {
        return particleRenderType;
    }

    public void setParticleRenderType(Object particleRenderType) {
        this.particleRenderType = particleRenderType;
    }

    public synchronized boolean needsNewRenderFromPixelData() {
        return needsNewRenderFromPixelData;
    }

    public synchronized void markNeedsNewRenderFromPixelData(boolean needsNewRender) {
        this.needsNewRenderFromPixelData = needsNewRender;
    }

    public boolean isNeedsNewRenderToPixelData() {
        return needsNewRenderToPixelData;
    }

    public void setNeedsNewRenderToPixelData(boolean needsNewRenderToPixelData) {
        this.needsNewRenderToPixelData = needsNewRenderToPixelData;
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

    public int getLastIndexReceived() {
        return lastIndexReceived;
    }

    public void setLastIndexReceived(int lastIndexReceived) {
        this.lastIndexReceived = lastIndexReceived;
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

    public AtomicBoolean getIsBufferReady() {
        return isBufferReady;
    }

    public ByteBuffer getDecompressionBuffer() {
        return decompressionBuffer;
    }

    public void setDecompressionBuffer(ByteBuffer decompressionBuffer) {
        this.decompressionBuffer = decompressionBuffer;
    }

    public DynamicTexture getImage() {
        return image;
    }

    public void setImage(DynamicTexture image) {
        this.image = image;
    }

    public void closeImage() {
        if (this.image != null) {
            this.image.close();
        }
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Screen getLastScreen() {
        return lastScreen;
    }

    public void setLastScreen(Screen lastScreen) {
        this.lastScreen = lastScreen;
    }
}
