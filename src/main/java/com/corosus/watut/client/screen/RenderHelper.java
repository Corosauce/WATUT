package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.status.PlayerStatus;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class RenderHelper {

    public static boolean performingOwnRender = false;
    public static Identifier cursor = Identifier.fromNamespaceAndPath(WatutMod.MODID, "textures/misc/mouse.png");

    public static ByteBufferProcessor processor = new ByteBufferProcessor(buffer -> {
        return compress(buffer);
    });

    public static void guiRender() {
        long gameTime = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            gameTime = mc.level.getGameTime();
        }

        for (PlayerStatus playerStatus : WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
            ScreenData screenData = playerStatus.getScreenData();

            if (screenData.getIsBufferReady().get() && screenData.needsNewRenderFromPixelData() && screenData.getTexturePixelData() != null
                    && screenData.getGameTicksSinceLastScreenReceiveAndRender() + ConfigClient.tickReceiveAndRenderRateOfGUIUpdates <= gameTime) {

                screenData.markNeedsNewRenderFromPixelData(false);
                screenData.setGameTicksSinceLastScreenReceiveAndRender(gameTime);

                int targetRes = 256;

                try {
                    ByteBuffer srcBuffer = screenData.getTexturePixelData();
                    srcBuffer.rewind();

                    DynamicTexture texture = screenData.getImage();
                    if (texture == null || texture.getPixels() == null || texture.getPixels().getWidth() != targetRes || texture.getPixels().getHeight() != targetRes) {
                        screenData.closeImage();
                        NativeImage newImage = new NativeImage(NativeImage.Format.RGBA, targetRes, targetRes, true);
                        texture = new DynamicTexture("watut_screen", targetRes, targetRes, true);
                        texture.setPixels(newImage);
                        texture.upload(); // Inizializza immediatamente la textureView su GPU!
                        screenData.setImage(texture);

                        if (playerStatus.getUuid() != null) {
                            mc.getTextureManager().register(screenData.getTextureIdentifier(playerStatus.getUuid()), texture);
                        }
                    }

                    NativeImage nativeImage = texture.getPixels();
                    if (nativeImage != null && !nativeImage.isClosed()) {
                        ByteBuffer dstBuffer = nativeImage.getPixelBytes();
                        dstBuffer.rewind();
                        int copyLen = Math.min(srcBuffer.remaining(), dstBuffer.remaining());
                        byte[] temp = new byte[copyLen];
                        srcBuffer.get(temp);
                        dstBuffer.put(temp);
                        dstBuffer.rewind();
                        srcBuffer.rewind();

                        texture.upload();
                        screenData.setTextureReady(true);
                    }
                } catch (Exception ex) {
                    CULog.dbg("Watut: error uploading dynamic screen texture: " + ex.getMessage());
                } finally {
                    screenData.getIsBufferReady().set(false);
                }
            }
        }
    }

    public static boolean useDynamicGUISystem() {
        if (ConfigServerControlledSyncedToClient.dynamicGuiUseOldSimpleGUIVisual) return false;
        if (ConfigClient.dontSendDetailedGUIInfo) return false;
        return true;
    }

    public static synchronized void renderWithTooltipEnd(GuiGraphicsExtractor extractor, int pMouseX, int pMouseY, float pPartialTick) {
        if (!useDynamicGUISystem()) return;
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        PlayerStatus playerStatusLocal = WatutMod.getPlayerStatusManagerClient().getStatusLocal();

        if (processor.hasProcessedBuffers()) {
            try {
                ByteBuffer result = processor.getProcessedBuffer();
                if (result != null) {
                    ScreenData screenDataLocal = playerStatusLocal.getScreenData();
                    screenDataLocal.setTexturePixelData(result);
                    WatutMod.getPlayerStatusManagerClient().sendScreenRenderData(playerStatusLocal);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static ByteBuffer compress(ByteBuffer inputBuffer) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);

        byte[] inputBytes = new byte[inputBuffer.remaining()];
        inputBuffer.get(inputBytes);
        deflater.setInput(inputBytes);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(inputBytes.length);
        byte[] temp = new byte[1024];

        while (!deflater.finished()) {
            int count = deflater.deflate(temp);
            baos.write(temp, 0, count);
        }
        deflater.end();

        byte[] compressed = baos.toByteArray();
        inputBuffer.rewind();
        return ByteBuffer.wrap(compressed);
    }

    public static ByteBuffer decompress(ScreenData screenData, ByteBuffer compressedBuffer, int expectedSize) throws Exception {
        Inflater inflater = new Inflater();

        byte[] compressedBytes = new byte[compressedBuffer.remaining()];
        compressedBuffer.get(compressedBytes);
        inflater.setInput(compressedBytes);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(expectedSize > 0 ? expectedSize : compressedBytes.length * 2);
        byte[] temp = new byte[1024];

        while (!inflater.finished()) {
            int count = inflater.inflate(temp);
            if (count == 0 && inflater.needsInput()) break;
            baos.write(temp, 0, count);
        }
        inflater.end();

        byte[] decompressed = baos.toByteArray();
        compressedBuffer.rewind();
        return ByteBuffer.wrap(decompressed);
    }
}
