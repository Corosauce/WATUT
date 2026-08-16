package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.status.PlayerStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

public class RenderHelper {

    public static boolean performingOwnRender = false;
    public static Identifier cursor = Identifier.fromNamespaceAndPath(WatutMod.MODID, "textures/misc/mouse.png");

    //xaero minimap support (doesnt capture extra elements just terrain)
    public static Class<?> guiMap;
    public static Class<?> improvedFramebuffer;
    public static int xaeroWorldMapTextureID = -1;
    public static Field guiMapPrimaryScaleFBO;
    public static Field colorTextureId;

    //shaders enable check support
    public static Class<?> irisConfig;
    public static Class<?> iris;
    public static Method getIrisConfig;
    public static Method areShadersEnabled;

    static {
        try {
            guiMap = Class.forName("xaero.map.gui.GuiMap");
        } catch (ClassNotFoundException ignored) {}
        try {
            improvedFramebuffer = Class.forName("xaero.map.graphics.ImprovedFramebuffer");
        } catch (ClassNotFoundException ignored) {}
        try {
            iris = Class.forName("net.irisshaders.iris.Iris");
            irisConfig = Class.forName("net.irisshaders.iris.config.IrisConfig");
            getIrisConfig = iris.getDeclaredMethod("getIrisConfig");
            areShadersEnabled = irisConfig.getDeclaredMethod("areShadersEnabled");
        } catch (ClassNotFoundException e) {
            try {
                iris = Class.forName("net.coderbot.iris.Iris");
                irisConfig = Class.forName("net.coderbot.iris.config.IrisConfig");
                getIrisConfig = iris.getDeclaredMethod("getIrisConfig");
                areShadersEnabled = irisConfig.getDeclaredMethod("areShadersEnabled");
            } catch (ClassNotFoundException | NoSuchMethodException ex) {
                CULog.log("watut: iris not installed or mod structure changed");
            }
        } catch (NoSuchMethodException e) {
            CULog.log("watut: iris not installed or mod structure changed");
        }
        try {
            if (guiMap != null) {
                guiMapPrimaryScaleFBO = guiMap.getDeclaredField("primaryScaleFBO");
                guiMapPrimaryScaleFBO.setAccessible(true);
            }
            if (improvedFramebuffer != null) {
                colorTextureId = improvedFramebuffer.getDeclaredField("colorTextureId");
            } else {
                CULog.log("watut: xaero minimap not installed or mod structure changed");
            }
        } catch (NoSuchFieldException e) {
            CULog.log("watut: xaero minimap not installed or mod structure changed");
        }
    }

    public static boolean isShadersEnabled() {
        if (areShadersEnabled == null) return false;
        try {
            return (boolean) areShadersEnabled.invoke(getIrisConfig.invoke(null));
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    public static boolean isXaeroGuiMap(Screen screen) {
        if (guiMap == null) return false;
        return guiMap.isInstance(screen);
    }

    public static ByteBufferProcessor processor = new ByteBufferProcessor(buffer -> {
        return compress(buffer);
    });

    public static void guiRender() {
        long gameTime = 0;
        if (Minecraft.getInstance().level != null) {
            gameTime = Minecraft.getInstance().level.getGameTime();
        }

        for (PlayerStatus playerStatus : WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
            ScreenData screenData = playerStatus.getScreenData();

            if (screenData.getIsBufferReady().get() && screenData.needsNewRenderFromPixelData() && screenData.getTexturePixelData() != null && screenData.getGameTicksSinceLastScreenReceiveAndRender() + ConfigClient.tickReceiveAndRenderRateOfGUIUpdates < gameTime) {
                screenData.markNeedsNewRenderFromPixelData(false);
                screenData.setGameTicksSinceLastScreenReceiveAndRender(gameTime);

                ScreenParticleRenderer.getInstance().checkSetup();

                if (playerStatus.getScreenData().getParticleRenderType() == null) {
                    playerStatus.getScreenData().initClient();
                }

                if (screenData.getImage() == null) {
                    screenData.setImage(new DynamicTexture("watut_screen", screenData.getWidth(), screenData.getHeight(), true));
                } else {
                    if (screenData.getImage().getPixels().getWidth() != screenData.getWidth() || screenData.getImage().getPixels().getHeight() != screenData.getHeight()) {
                        screenData.closeImage();
                        screenData.setImage(new DynamicTexture("watut_screen", screenData.getWidth(), screenData.getHeight(), true));
                        CULog.dbg("screendata image resized to " + screenData.getWidth() + " " + screenData.getHeight());
                    }
                }
                long nativeImagePixelMemoryAddress = screenData.getImage().getPixels().getPointer();
                if (nativeImagePixelMemoryAddress != -1 && screenData.getDecompressionBuffer() != null) {
                    MemoryUtil.memCopy(MemoryUtil.memAddress(screenData.getDecompressionBuffer()), nativeImagePixelMemoryAddress,
                            (long) screenData.getWidth() * screenData.getHeight() * ScreenParticleRenderer.bytesPerPixel);
                }

                screenData.getImage().upload();
                playerStatus.getScreenData().getIsBufferReady().set(false);
            }
        }
    }

    public static void bindVanillaRenderTargetAndSetupProjectionMatrix() {
    }

    public static void unbindVanillaRenderTarget() {
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
                throw new RuntimeException(e);
            }
        }
    }

    public static ByteBuffer compress(ByteBuffer inputBuffer) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);

        byte[] inputBytes = new byte[inputBuffer.remaining()];
        inputBuffer.get(inputBytes);
        deflater.setInput(inputBytes);
        deflater.finish();

        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(inputBytes.length + 512);
        byte[] temp = new byte[1024];

        while (!deflater.finished()) {
            int compressedBytes = deflater.deflate(temp);
            if (outputBuffer.remaining() < compressedBytes) {
                ByteBuffer newBuffer = ByteBuffer.allocateDirect(outputBuffer.capacity() * 2);
                outputBuffer.flip();
                newBuffer.put(outputBuffer);
                outputBuffer = newBuffer;
            }
            outputBuffer.put(temp, 0, compressedBytes);
        }
        deflater.end();

        outputBuffer.flip();
        inputBuffer.flip();
        return outputBuffer;
    }

    public static ByteBuffer decompress(ScreenData screenData, ByteBuffer compressedBuffer, int expectedSize) throws Exception {
        Inflater inflater = new Inflater();

        byte[] compressedBytes = new byte[compressedBuffer.remaining()];
        compressedBuffer.get(compressedBytes);
        inflater.setInput(compressedBytes);

        ByteBuffer decompressionBuffer = screenData.getDecompressionBuffer();

        if (decompressionBuffer == null) {
            CULog.dbg("Creating new buffer for decompression");
            decompressionBuffer = MemoryUtil.memAlloc(expectedSize);
            screenData.setDecompressionBuffer(decompressionBuffer);
        } else {
            decompressionBuffer.clear();
        }
        byte[] temp = new byte[1024];

        while (!inflater.finished()) {
            int decompressedBytes = inflater.inflate(temp);
            if (decompressionBuffer.remaining() < decompressedBytes) {
                CULog.dbg("expanding buffer from " + decompressionBuffer.capacity() + " to " + (decompressionBuffer.capacity() * 2));
                ByteBuffer newBuffer = MemoryUtil.memAlloc(decompressionBuffer.capacity() * 2);
                decompressionBuffer.flip();
                newBuffer.put(decompressionBuffer);
                MemoryUtil.memFree(decompressionBuffer);
                decompressionBuffer = newBuffer;
                screenData.setDecompressionBuffer(decompressionBuffer);
            }

            decompressionBuffer.put(temp, 0, decompressedBytes);
        }
        inflater.end();

        decompressionBuffer.flip();

        return decompressionBuffer;
    }

    public static ByteBuffer decompress2(ScreenData screenData, ByteBuffer compressedBuffer, int expectedSize) throws Exception {
        Inflater inflater = new Inflater();

        byte[] compressedBytes = new byte[compressedBuffer.remaining()];
        compressedBuffer.get(compressedBytes);
        inflater.setInput(compressedBytes);

        ByteBuffer decompressionBuffer = screenData.getDecompressionBuffer();

        if (decompressionBuffer == null) {
            decompressionBuffer = ByteBuffer.allocateDirect(expectedSize);
            screenData.setDecompressionBuffer(decompressionBuffer);
        } else {
            decompressionBuffer.clear();
        }
        byte[] temp = new byte[1024];

        while (!inflater.finished()) {
            int decompressedBytes = inflater.inflate(temp);
            if (decompressionBuffer.remaining() < decompressedBytes) {
                throw new IllegalStateException("Decompressed size exceeds expected size!");
            }
            decompressionBuffer.put(temp, 0, decompressedBytes);
        }
        inflater.end();

        decompressionBuffer.flip();
        return decompressionBuffer;
    }

    public static ByteBuffer decompressGZIP(ByteBuffer compressedBuffer) throws IOException {
        byte[] compressedBytes = new byte[compressedBuffer.remaining()];
        compressedBuffer.get(compressedBytes);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedBytes);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        gzipInputStream.close();
        byteArrayInputStream.close();

        byte[] decompressedBytes = byteArrayOutputStream.toByteArray();
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(decompressedBytes.length);
        directBuffer.put(decompressedBytes);
        directBuffer.flip();

        return directBuffer;
    }

    public static ByteBuffer compressGZIP(ByteBuffer inputBuffer) throws IOException {
        byte[] inputBytes = new byte[inputBuffer.remaining()];
        inputBuffer.get(inputBytes);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(inputBytes);
        }

        byte[] compressedBytes = byteArrayOutputStream.toByteArray();
        return ByteBuffer.wrap(compressedBytes);
    }

    public static ByteBuffer getPixelDataFromFrameBuffer() {
        int width = ScreenParticleRenderer.getInstance().widthScaledDown;
        int height = ScreenParticleRenderer.getInstance().heightScaledDown;

        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * ScreenParticleRenderer.bytesPerPixel);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);

        return pixelBuffer;
    }

    public static boolean validatePixelByteBuffer(ByteBuffer byteBuffer, int expectedSize, int expectedAlignment) {
        if (byteBuffer.capacity() != expectedSize) {
            System.err.println("Buffer size mismatch: Expected " + expectedSize + ", but got " + byteBuffer.capacity());
            return false;
        }

        long address = MemoryUtil.memAddress(byteBuffer);
        if (address % expectedAlignment != 0) {
            System.err.println("Buffer alignment mismatch: Address " + address + " is not aligned to " + expectedAlignment + " bytes.");
            return false;
        }

        return true;
    }
}
