package com.corosus.watut.client.screen;

import com.corosus.watut.client.ParticleRenderTypeOld;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenData {

    private volatile ByteBuffer texturePixelData = null;
    private volatile ByteBuffer decompressionBuffer = null;
    private final AtomicBoolean isBufferReady = new AtomicBoolean(false);
    private byte[] texturePixelDataPartial = null;

    //private byte[] texturePixelDataDiff = null;

    private long gameTicksSinceFirstPacket = 0;
    private int lastIndexReceived = 0;
    private long gameTicksSinceLastScreenSend = 0;
    private long gameTicksSinceLastScreenReceiveAndRender = 0;

    private ParticleRenderTypeOld particleRenderType;

    private boolean needsNewRenderFromPixelData = false;

    //used for communicating from outside screen render hook to inside it
    private boolean needsNewRenderToPixelData = false;

    private DynamicTexture image = null;
    private int width = ScreenParticleRenderer.defaultWidthScaledDown;
    private int height = ScreenParticleRenderer.defaultHeightScaledDown;

    //since gui states are kinda old system and require specifically adding support for a screen, we use this instead to track true differences now
    private Screen lastScreen;

    public static boolean testing = false;

    private static int nextTextureIndex = 0;
    private Identifier textureId;

    public static boolean isValidReceivedSize(int width, int height) {
        int max = com.corosus.watut.config.ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen ? 4096 : 512;
        return width > 0 && height > 0 && width <= max && height <= max;
    }

    public void initClient() {
        if (textureId == null) {
            textureId = Identifier.fromNamespaceAndPath(com.corosus.watut.WatutMod.MODID, "dynamic/screen_" + (nextTextureIndex++));
        }
        final Identifier id = textureId;
        this.particleRenderType = new ParticleRenderTypeOld() {
            @Override
            public Identifier getTextureId() {
                return getImage() != null ? id : null;
            }

            public String toString() {
                return "DYNAMIC_TEXTURE";
            }
        };
    }

    public Identifier getTextureId() {
        return textureId;
    }

    public ByteBuffer getTexturePixelData() {
        return texturePixelData;
    }

    public void release() {
        closeImage();
        if (decompressionBuffer != null) {
            MemoryUtil.memFree(decompressionBuffer);
            decompressionBuffer = null;
        }
        texturePixelData = null;
        getIsBufferReady().set(false);
        markNeedsNewRenderFromPixelData(false);
    }

    public void setTexturePixelData(ByteBuffer texturePixelData) {
        this.texturePixelData = texturePixelData;
    }

    public ParticleRenderTypeOld getParticleRenderType() {
        return particleRenderType;
    }

    public void setParticleRenderType(ParticleRenderTypeOld particleRenderType) {
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
            if (textureId != null) {
                net.minecraft.client.Minecraft.getInstance().getTextureManager().release(textureId);
            } else {
                this.image.close();
            }
            this.image = null;
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

    /*public byte[] getTexturePixelDataDiff() {
        return texturePixelDataDiff;
    }

    public void setTexturePixelDataDiff(byte[] texturePixelDataDiff) {
        this.texturePixelDataDiff = texturePixelDataDiff;
    }*/
}
