package com.corosus.watut.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenData {

    private boolean needsNewRenderToPixelData = false;
    private boolean needsNewRenderFromPixelData = false;
    private long gameTicksSinceLastScreenSend = 0;
    private long gameTicksSinceLastScreenReceiveAndRender = 0;
    private long gameTicksSinceFirstPacket = 0;
    private int lastIndexReceived = 0;
    private byte[] texturePixelDataPartial = new byte[0];
    private ByteBuffer texturePixelData;
    private ByteBuffer decompressionBuffer;
    private final AtomicBoolean isBufferReady = new AtomicBoolean(false);
    private int width = 256;
    private int height = 256;
    private Screen lastScreen;
    private Object particleRenderType;
    private Identifier textureIdentifier;
    private long lastFrameHash = 0;
    private volatile boolean textureReady = false;
    private DynamicTexture image;

    public Identifier getTextureIdentifier(UUID uuid) {
        if (this.textureIdentifier == null && uuid != null) {
            this.textureIdentifier = Identifier.fromNamespaceAndPath("watut", "screen_" + uuid.toString().replace("-", ""));
        }
        return this.textureIdentifier;
    }

    public long getLastFrameHash() {
        return lastFrameHash;
    }

    public void setLastFrameHash(long lastFrameHash) {
        this.lastFrameHash = lastFrameHash;
    }

    public ByteBuffer getTexturePixelData() {
        return texturePixelData;
    }

    public void freeTexturePixelData() {
        this.texturePixelData = null;
        this.decompressionBuffer = null;
        this.texturePixelDataPartial = new byte[0];
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

    public boolean isTextureReady() {
        return textureReady && image != null;
    }

    public void setTextureReady(boolean ready) {
        this.textureReady = ready;
    }

    public DynamicTexture getImage() {
        return image;
    }

    public void setImage(DynamicTexture image) {
        this.image = image;
    }

    public void closeImage() {
        this.textureReady = false;
        if (this.image != null) {
            this.image.close();
            this.image = null;
        }
    }

    public void cleanup() {
        this.textureReady = false;
        freeTexturePixelData();
        this.isBufferReady.set(false);
        this.needsNewRenderFromPixelData = false;
        this.lastFrameHash = 0;
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
