package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.PlayerStatusManagerClient;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenData {

    private int textureID = -1;
    private volatile ByteBuffer texturePixelData = null;
    private volatile ByteBuffer decompressionBuffer = null;
    private final AtomicBoolean isBufferReady = new AtomicBoolean(false);
    private byte[] texturePixelDataPartial = null;

    private long gameTicksSinceFirstPacket = 0;
    private long gameTicksSinceLastScreenSend = 0;
    private long gameTicksSinceLastScreenReceiveAndRender = 0;

    private ParticleRenderType particleRenderType;

    private boolean needsNewRender = false;

    private DynamicTexture image = null;
    public RenderType renderType;
    public ResourceLocation res;

    public void init() {

    }

    public void initClient() {

        this.particleRenderType = new ParticleRenderType() {
            public void begin(BufferBuilder p_107455_, TextureManager p_107456_) {
                RenderSystem.setShader(() -> PlayerStatusManagerClient.particle);
                //RenderSystem._setShaderTexture(0, textureID);
                //RenderSystem._setShaderTexture(0, ScreenParticleRenderer.getInstance().getMainRenderTargetScaledDownFromByteBuffer().getColorTextureId());
                //TODO: this works multiplayer, textureID does not, huh?
                RenderSystem._setShaderTexture(0, getImage().getId());
                //test
                //GlStateManager._bindTexture(textureID);
                //GlStateManager._bindTexture(getImage().getId());

                RenderSystem.depthMask(true);
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

    public int getTextureID() {
        return textureID;
    }

    public void setTextureID(int textureID) {
        this.textureID = textureID;
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

    public AtomicBoolean getIsBufferReady() {
        return isBufferReady;
    }

    public ByteBuffer getDecompressionBuffer() {
        return decompressionBuffer;
    }

    public void setDecompressionBuffer(ByteBuffer decompressionBuffer) {
        this.decompressionBuffer = decompressionBuffer;
    }

    public void test() {
        if (image == null) {
            //NativeImage nativeImage = new NativeImage(ScreenParticleRenderer.getInstance().widthScaledDown, ScreenParticleRenderer.getInstance().heightScaledDown, false);
            //DynamicTexture dynamicTexture = new DynamicTexture(ScreenParticleRenderer.getInstance().widthScaledDown, ScreenParticleRenderer.getInstance().heightScaledDown, false);
            /*DynamicTexture dynamicTexture = null;
            try {
                dynamicTexture = new DynamicTexture(NativeImage.read(this.decompressionBuffer));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/
            /*try {
                image = new DynamicTexture(NativeImage.read(this.decompressionBuffer));
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            /*try {
                image = new DynamicTexture(NativeImage.read(this.decompressionBuffer));
            } catch (IOException e) {*/
                //e.printStackTrace();
                image = new DynamicTexture(ScreenParticleRenderer.getInstance().widthScaledDown, ScreenParticleRenderer.getInstance().heightScaledDown, true);
                //TODO: shouldnt be needed, see other workspace that removed it
                Random rand = new Random();
                ResourceLocation resourcelocation = Minecraft.getInstance().textureManager.register("testingg/" + rand.nextInt(9999999), image);
                res = resourcelocation;
                this.renderType = RenderType.text(resourcelocation);
            //}
        } else {
            /*try {
                image = new DynamicTexture(NativeImage.read(this.decompressionBuffer));
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            //image = new DynamicTexture(ScreenParticleRenderer.getInstance().widthScaledDown, ScreenParticleRenderer.getInstance().heightScaledDown, false);
            /*Random random = new Random();
            for (int i = 0; i < 1000; i++) {
                int x = random.nextInt(ScreenParticleRenderer.getInstance().widthScaledDown);
                int y = random.nextInt(ScreenParticleRenderer.getInstance().heightScaledDown);
                image.getPixels().setPixelRGBA(x, y, MapColor.getColorFromPackedId(48));
            }

            image.upload();*/
        }
    }

    public DynamicTexture getImage() {
        return image;
    }

    public void setImage(DynamicTexture image) {
        this.image = image;
    }
}
