package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScreenParticleRenderer {

    public static boolean isRenderingParticleGUI = false;
    public static boolean isRenderingParticleGUI2 = false;

    //used on client with gui open side
    //used to capture raw copy of minecraft screen
    private MainTarget mainRenderTarget;

    public int width;
    public int height;
    public static int defaultWidthScaledDown = 256;
    public static int defaultHeightScaledDown = 256;
    public static int bytesPerPixel = 4;
    public int widthScaledDown = defaultWidthScaledDown;
    public int heightScaledDown = defaultHeightScaledDown;
    public boolean needsInit = true;

    private static final float[] WEIGHTS_LEVEL_1 = {0.45F, 0.1F, 0.1F, 0.05F, 0.02F};
    private static final float[] WEIGHTS_GAUSSIAN = {0.227027F, 0.1945946F, 0.1216216F, 0.054054F, 0.016216F};
    private static final int BLUR_RANGE = 4;
    private static final int FADE_WIDTH = 16;

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
        resizeScaledDown(width, height);
    }

    public synchronized void resize(int width, int height) {
        this.width = width;
        this.height = height;
        checkSetup();
        mainRenderTarget.resize(width, height);
        resizeScaledDown(width, height);
    }

    public void resizeScaledDown(int width, int height) {
        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            widthScaledDown = width;
            heightScaledDown = height;
        } else {
            widthScaledDown = defaultWidthScaledDown;
            heightScaledDown = defaultHeightScaledDown;
        }
    }

    public MainTarget getMainRenderTarget() {
        return mainRenderTarget;
    }

    public void clearMainRenderTarget() {
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                mainRenderTarget.getColorTexture(), new Vector4f(0, 0, 0, 0), mainRenderTarget.getDepthTexture(), 0.0);
    }

    public void performCaptureBlurAndReadback(int mouseX, int mouseY) {
        checkSetup();
        boolean dbg = RenderHelper.CAPTURE_DEBUG;

        GpuTexture colorTexture = mainRenderTarget.getColorTexture();
        if (colorTexture == null) {
            CULog.dbg("[watut-capture] readback aborted: capture target color texture is null");
            return;
        }

        int sourceWidth = mainRenderTarget.width;
        int sourceHeight = mainRenderTarget.height;
        int cropW;
        int cropH;
        int cropX;
        int cropY;
        int outW;
        int outH;
        //area the output image represents, can extend past the screen edges, the part outside the screen comes out transparent
        int virtualW;
        int virtualH;
        int virtualOffsetX = 0;
        int virtualOffsetY = 0;
        float contentScale = 1F;
        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            cropW = sourceWidth;
            cropH = sourceHeight;
            outW = widthScaledDown;
            outH = heightScaledDown;
            cropX = (sourceWidth - cropW) / 2;
            cropY = (sourceHeight - cropH) / 2;
            virtualW = cropW;
            virtualH = cropH;
        } else if (RenderHelper.xaeroWorldMapTextureID != -1) {
            int side = Math.min(sourceWidth, sourceHeight);
            cropW = side;
            cropH = side;
            outW = 256;
            outH = 256;
            cropX = (sourceWidth - cropW) / 2;
            cropY = (sourceHeight - cropH) / 2;
            virtualW = cropW;
            virtualH = cropH;
        } else {
            Minecraft mc = Minecraft.getInstance();
            double guiScale = mc.getWindow().getGuiScale();
            outW = widthScaledDown;
            outH = heightScaledDown;
            double centerX = sourceWidth / 2D;

            Screen screen = mc.gui != null ? mc.gui.screen() : null;
            ScreenRectangle content = RenderHelper.getGuiContentBounds(screen);
            if (content != null && screen != null) {
                contentScale = RenderHelper.getGuiContentScale(content, screen.height);
                //keep main gui in center if things like recipe book pushes it to the side
                centerX = (content.left() + content.width() / 2D) * guiScale;
            }

            virtualW = (int) Math.round(outW * guiScale * contentScale);
            virtualH = (int) Math.round(outH * guiScale * contentScale);
            int virtualX = (int) Math.round(centerX - virtualW / 2D);
            int virtualY = (sourceHeight - virtualH) / 2;

            cropX = Math.max(0, virtualX);
            cropY = Math.max(0, virtualY);
            cropW = Math.min(sourceWidth, virtualX + virtualW) - cropX;
            cropH = Math.min(sourceHeight, virtualY + virtualH) - cropY;
            virtualOffsetX = cropX - virtualX;
            virtualOffsetY = cropY - virtualY;
            if (cropW <= 0 || cropH <= 0) {
                return;
            }
        }

        float radius = (float) ConfigServerControlledSyncedToClient.dynamicGuiSizeRadiusInPixelsToShow;
        int blurLevel = RenderHelper.xaeroWorldMapTextureID != -1 ? 0 : ConfigServerControlledSyncedToClient.dynamicGuiBlurLevel;

        int blockSize = colorTexture.getFormat().blockSize();
        if (dbg) CULog.dbg("[watut-capture] readback src=" + sourceWidth + "x" + sourceHeight
                + " crop=" + cropW + "x" + cropH + "@" + cropX + "," + cropY
                + " virtual=" + virtualW + "x" + virtualH + " offset=" + virtualOffsetX + "," + virtualOffsetY + " contentScale=" + contentScale
                + " out=" + outW + "x" + outH + " blurLevel=" + blurLevel + " xaeroSentinel=" + RenderHelper.xaeroWorldMapTextureID);
        final int fCropW = cropW, fCropH = cropH, fOutW = outW, fOutH = outH;
        final int fVirtualW = virtualW, fVirtualH = virtualH, fVirtualOffsetX = virtualOffsetX, fVirtualOffsetY = virtualOffsetY;
        GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "WATUT gui capture readback",
                GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, (long) cropW * cropH * blockSize);

        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(colorTexture, gpuBuffer, 0, () -> {
            try (GpuBufferSlice.MappedView view = gpuBuffer.map(true, false)) {
                ByteBuffer src = view.data().order(ByteOrder.LITTLE_ENDIAN);
                if (dbg) logCapturePixelStats(src, fCropW, fCropH, blockSize);
                ByteBuffer out = process(src, fCropW, fCropH, blockSize, fOutW, fOutH, radius, blurLevel,
                        fVirtualW, fVirtualH, fVirtualOffsetX, fVirtualOffsetY);
                RenderHelper.processor.submitForProcessing(new CapturedFrame(out, fOutW, fOutH));
            } catch (Exception e) {
                CULog.err("WATUT: gui capture readback failed: " + e);
            } finally {
                gpuBuffer.close();
            }
        }, 0, cropX, cropY, cropW, cropH);
    }


    private static ByteBuffer process(ByteBuffer src, int srcW, int srcH, int blockSize, int outW, int outH, float radius, int blurLevel,
                                      int virtualW, int virtualH, int virtualOffsetX, int virtualOffsetY) {
        float[] weights = blurLevel == 1 ? WEIGHTS_LEVEL_1 : WEIGHTS_GAUSSIAN;
        float[] r = new float[outW * outH];
        float[] g = new float[outW * outH];
        float[] b = new float[outW * outH];
        float[] a = new float[outW * outH];

        for (int oy = 0; oy < outH; oy++) {
            int sy = (int) ((oy + 0.5F) * virtualH / outH) - virtualOffsetY;
            if (sy < 0 || sy >= srcH) continue;
            for (int ox = 0; ox < outW; ox++) {
                int sx = (int) ((ox + 0.5F) * virtualW / outW) - virtualOffsetX;
                if (sx < 0 || sx >= srcW) continue;
                int i = ox + oy * outW;
                int center = pixel(src, sx, sy, srcW, blockSize);
                a[i] = ((center >>> 24) & 0xFF) / 255F;
                if (a[i] <= 0) continue;
                if (blurLevel == 0) {
                    r[i] = (center & 0xFF) / 255F;
                    g[i] = ((center >>> 8) & 0xFF) / 255F;
                    b[i] = ((center >>> 16) & 0xFF) / 255F;
                } else {
                    for (int k = -BLUR_RANGE; k <= BLUR_RANGE; k++) {
                        int p = pixel(src, Math.clamp(sx + k, 0, srcW - 1), sy, srcW, blockSize);
                        float w = weights[Math.abs(k)];
                        r[i] += (p & 0xFF) / 255F * w;
                        g[i] += ((p >>> 8) & 0xFF) / 255F * w;
                        b[i] += ((p >>> 16) & 0xFF) / 255F * w;
                    }
                }
            }
        }

        ByteBuffer out = ByteBuffer.allocateDirect(outW * outH * bytesPerPixel).order(ByteOrder.LITTLE_ENDIAN);
        float midX = outW / 2F;
        float midY = outH / 2F;
        for (int oy = 0; oy < outH; oy++) {
            for (int ox = 0; ox < outW; ox++) {
                int i = ox + oy * outW;
                float alpha = a[i];
                float rr = r[i], gg = g[i], bb = b[i];
                if (blurLevel != 0) {
                    rr = gg = bb = 0;
                    for (int k = -BLUR_RANGE; k <= BLUR_RANGE; k++) {
                        int j = ox + Math.clamp(oy + k, 0, outH - 1) * outW;
                        float w = weights[Math.abs(k)];
                        rr += r[j] * w;
                        gg += g[j] * w;
                        bb += b[j] * w;
                    }
                }
                if (radius != -1) {
                    float dist = (float) Math.hypot(ox + 0.5F - midX, oy + 0.5F - midY);
                    if (dist > radius + FADE_WIDTH) {
                        alpha = 0;
                    } else if (dist > radius) {
                        alpha = Math.min(alpha, 1 - ((dist - radius) / FADE_WIDTH));
                    }
                }
                out.putInt(i * bytesPerPixel, (toByte(alpha) << 24) | (toByte(bb) << 16) | (toByte(gg) << 8) | toByte(rr));
            }
        }
        out.position(0);
        out.limit(outW * outH * bytesPerPixel);
        return out;
    }

    private static void logCapturePixelStats(ByteBuffer src, int srcW, int srcH, int blockSize) {
        int r = 0, g = 0, b = 0, a = 0, lit = 0, n = 0;
        for (int gy = 0; gy < 16; gy++) {
            for (int gx = 0; gx < 16; gx++) {
                int p = src.getInt((Math.min(srcW - 1, srcW * gx / 16) + Math.min(srcH - 1, srcH * gy / 16) * srcW) * blockSize);
                r += p & 0xFF;
                g += (p >>> 8) & 0xFF;
                b += (p >>> 16) & 0xFF;
                a += (p >>> 24) & 0xFF;
                if (((p >>> 24) & 0xFF) > 0 && (p & 0xFFFFFF) != 0) lit++;
                n++;
            }
        }
        CULog.dbg("[watut-capture] pixels: avgRGBA=" + (r / n) + "," + (g / n) + "," + (b / n) + "," + (a / n)
                + " litSamples=" + lit + "/" + n + (lit == 0 ? " <- CAPTURE EMPTY, nothing drawn to target" : ""));
    }

    private static int pixel(ByteBuffer src, int x, int y, int srcW, int blockSize) {
        return src.getInt((x + y * srcW) * blockSize);
    }

    private static int toByte(float v) {
        return Math.clamp(Math.round(v * 255F), 0, 255);
    }
}
