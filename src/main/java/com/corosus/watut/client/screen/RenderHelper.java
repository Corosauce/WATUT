package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.PlayerStatus;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.mixin.client.NativeImageAccessor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
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
    public static ResourceLocation cursor = ResourceLocation.fromNamespaceAndPath(WatutMod.MODID, "textures/misc/mouse.png");

    //xaero minimap support (doesnt capture extra elements just terrain)
    public static Class guiMap;
    public static Class improvedFramebuffer;
    public static int xaeroWorldMapTextureID = -1;
    public static Field guiMapPrimaryScaleFBO;
    public static Field colorTextureId;

    //shaders enable check support
    public static Class irisConfig;
    public static Class iris;
    public static Method getIrisConfig;
    public static Method areShadersEnabled;

    //i dont want to depend build against an api, so i do this instead like the silly bitch that i am
    static {
        try {
            guiMap = Class.forName("xaero.map.gui.GuiMap");
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
        }
        try {
            improvedFramebuffer = Class.forName("xaero.map.graphics.ImprovedFramebuffer");
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
        }
        try {
            iris = Class.forName("net.irisshaders.iris.Iris");
            irisConfig = Class.forName("net.irisshaders.iris.config.IrisConfig");
            getIrisConfig = iris.getDeclaredMethod("getIrisConfig");
            areShadersEnabled = irisConfig.getDeclaredMethod("areShadersEnabled");
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
            try {
                iris = Class.forName("net.coderbot.iris.Iris");
                irisConfig = Class.forName("net.coderbot.iris.config.IrisConfig");
                getIrisConfig = iris.getDeclaredMethod("getIrisConfig");
                areShadersEnabled = irisConfig.getDeclaredMethod("areShadersEnabled");
            } catch (ClassNotFoundException ex) {
                CULog.log("watut: oculus not installed or mod structure changed");
                //throw new RuntimeException(ex);
            } catch (NoSuchMethodException ex) {
                //throw new RuntimeException(ex);
                CULog.log("watut: oculus not installed or mod structure changed");
            }
        } catch (NoSuchMethodException e) {
            CULog.log("watut: oculus not installed or mod structure changed");
        }
        try {
            improvedFramebuffer = Class.forName("xaero.map.graphics.ImprovedFramebuffer");
        } catch (ClassNotFoundException e) {
            //e.printStackTrace();
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
            //e.printStackTrace();
        }
    }

    public static boolean isShadersEnabled() {
        if (areShadersEnabled == null) return false;
        try {
            return (boolean) areShadersEnabled.invoke(getIrisConfig.invoke(null));
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    public static boolean isXaeroGuiMap(Screen screen) {
        if (guiMap == null) return false;
        return guiMap.isInstance(screen);
    }

    public static ByteBufferProcessor processor = new ByteBufferProcessor(buffer -> {
        ByteBuffer processed = compress(buffer);

        //TODO: testing
        /*byte[] newData = new byte[buffer.remaining()];
        buffer.get(newData);
        if (WatutMod.getPlayerStatusManagerClient().getStatusLocal().getScreenData().getTexturePixelDataDiff() != null) {
            byte[] prevData = WatutMod.getPlayerStatusManagerClient().getStatusLocal().getScreenData().getTexturePixelDataDiff();

            List<PixelDifference> diff = calculateDifferences(prevData, newData);
            ByteBuffer diffBuffer = ByteBuffer.allocate(diff.size() * (4 + 4)); //size = rgba + int
            for (PixelDifference pixelDifference : diff) {
                //List<Byte> list = new ArrayList<>();
                byte[] bytes = new byte[10];
                //TODO: its not actually storing the bytes
                bytes = new byte[encodeIndex(pixelDifference.index, bytes)];
                //diffBuffer.putInt(pixelDifference.index);
                diffBuffer.put(bytes);
                diffBuffer.put(pixelDifference.rgba);
            }
            diffBuffer.flip();
            //CULog.dbg("diff size " + diff.size() * (4 + 4));
            CULog.dbg("diff size " + diffBuffer.limit());
            ByteBuffer processed2 = compress(diffBuffer);
            CULog.dbg("diff size compressed " + processed2.limit());
        }

        WatutMod.getPlayerStatusManagerClient().getStatusLocal().getScreenData().setTexturePixelDataDiff(newData);*/

        return processed;
    });

    /*public static int encodeIndex(int index, byte[] encodedBytes) {
        int position = 0; // Keep track of the number of bytes written

        while ((index & ~0x7F) != 0) { // If more than 7 bits are needed
            encodedBytes[position++] = (byte) ((index & 0x7F) | 0x80); // Store 7 bits, set MSB
            index >>>= 7; // Shift to process the next 7 bits
        }
        encodedBytes[position++] = (byte) (index & 0x7F); // Store the last 7 bits

        return position; // Return the number of bytes written
    }

    // Method to calculate differences between two images
    public static List<PixelDifference> calculateDifferences(byte[] image1, byte[] image2) {
        if (image1.length != image2.length) {
            throw new IllegalArgumentException("Images must have the same size!");
        }

        List<PixelDifference> differences = new ArrayList<>();

        for (int i = 0; i < image1.length; i += 4) { // RGBA = 4 bytes per pixel
            if (image1[i] != image2[i] || image1[i + 1] != image2[i + 1] ||
                    image1[i + 2] != image2[i + 2] || image1[i + 3] != image2[i + 3]) {
                // Store the pixel difference
                differences.add(new PixelDifference(i, new byte[] {
                        image2[i], image2[i + 1], image2[i + 2], image2[i + 3]
                }));
            }
        }

        return differences;
    }

    static class PixelDifference {
        int index; // The index of the pixel in the byte array
        byte[] rgba; // The new RGBA value

        PixelDifference(int index, byte[] rgba) {
            this.index = index;
            this.rgba = rgba;
        }
    }*/

    public static void guiRender(GuiGraphics guiGraphics) {
        long gameTime = 0;
        if (Minecraft.getInstance().level != null) {
            gameTime = Minecraft.getInstance().level.getGameTime();
        }

        for (PlayerStatus playerStatus : WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
            ScreenData screenData = playerStatus.getScreenData();

            if ((screenData.getIsBufferReady().get() && screenData.needsNewRenderFromPixelData() && screenData.getTexturePixelData() != null && screenData.getGameTicksSinceLastScreenReceiveAndRender() + ConfigClient.tickReceiveAndRenderRateOfGUIUpdates < gameTime)) {
                screenData.markNeedsNewRenderFromPixelData(false);
                screenData.setGameTicksSinceLastScreenReceiveAndRender(gameTime);

                ScreenParticleRenderer.getInstance().checkSetup();

                if (playerStatus.getScreenData().getParticleRenderType() == null) {
                    playerStatus.getScreenData().initClient();
                }

                if (screenData.getImage() == null) {
                    screenData.setImage(new DynamicTexture(screenData.getWidth(), screenData.getHeight(), true));
                } else {
                    //detect a resolution change and remake buffer, only used if experimental rendering of entire screen config is on
                    //CULog.dbg("screendata sizes " + screenData.getWidth() + " " + screenData.getHeight());
                    if (screenData.getImage().getPixels().getWidth() != screenData.getWidth() || screenData.getImage().getPixels().getHeight() != screenData.getHeight()) {
                        screenData.closeImage();
                        screenData.setImage(new DynamicTexture(screenData.getWidth(), screenData.getHeight(), true));
                        CULog.dbg("screendata image resized to " + screenData.getWidth() + " " + screenData.getHeight());
                    }
                }
                //hack into NativeImage to directly inject pixel data, working around its requirements for a PNG format parse
                long nativeImagePixelMemoryAddress = ((NativeImageAccessor)((Object)screenData.getImage().getPixels())).pixels();
                if (nativeImagePixelMemoryAddress != -1) {
                    MemoryUtil.memCopy(MemoryUtil.memAddress(screenData.getDecompressionBuffer()), nativeImagePixelMemoryAddress,
                            screenData.getWidth() * screenData.getHeight() * ScreenParticleRenderer.bytesPerPixel);
                }

                screenData.getImage().upload();

                playerStatus.getScreenData().getIsBufferReady().set(false);

            }
        }
    }

    public static void bindVanillaRenderTargetAndSetupProjectionMatrix() {
        Window window = Minecraft.getInstance().getWindow();
        Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)((double)window.getWidth() / window.getGuiScale()), (float)((double)window.getHeight() / window.getGuiScale()), 0.0F, 1000.0F, WatutMod.instance().getFarPlane());
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    public static void unbindVanillaRenderTarget() {
        Minecraft.getInstance().getMainRenderTarget().unbindWrite();
    }

    public static boolean useDynamicGUISystem() {
        if (ConfigServerControlledSyncedToClient.dynamicGuiUseOldSimpleGUIVisual) return false;
        if (ConfigClient.dontSendDetailedGUIInfo) return false;
        return true;
    }

    public static synchronized void renderWithTooltipEnd(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
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

        boolean needsScreenUpdate = playerStatusLocal.getScreenData().isNeedsNewRenderToPixelData();



        if (needsScreenUpdate && !processor.hasWork()) {

            playerStatusLocal.getScreenData().setNeedsNewRenderToPixelData(false);
            ScreenParticleRenderer.getInstance().checkSetup();
            unbindVanillaRenderTarget();
            ScreenParticleRenderer.getInstance().bind();

            RenderSystem.clear(16640, Minecraft.ON_OSX);

            if (Minecraft.getInstance().screen != null) {
                if (ConfigServerControlledSyncedToClient.dynamicGuiDisableBackgroundRendering) {
                    ScreenParticleRenderer.isRenderingParticleGUI = true;
                    ScreenParticleRenderer.isRenderingParticleGUI2 = true;
                }
                performingOwnRender = true;

                xaeroWorldMapTextureID = -1;

                if (isXaeroGuiMap(Minecraft.getInstance().screen)) {
                    try {
                        Object fbo = guiMapPrimaryScaleFBO.get(null);
                        if (fbo != null) {
                            Object colorTextureIdObj = colorTextureId.get(fbo);
                            if (colorTextureIdObj != null) {
                                xaeroWorldMapTextureID = (int) colorTextureIdObj;
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    Minecraft.getInstance().screen.renderWithTooltip(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
                }

                //render our cursor
                ScreenParticleRenderer.getInstance().innerBlit(pGuiGraphics.pose(), cursor, pMouseX, pMouseX + 6, pMouseY, pMouseY + 10, 100, 0, 1, 0, 1);

                performingOwnRender = false;
                ScreenParticleRenderer.isRenderingParticleGUI = false;
                ScreenParticleRenderer.isRenderingParticleGUI2 = false;
            }

            ScreenParticleRenderer.getInstance().unbind();

            Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)ScreenParticleRenderer.getInstance().widthScaledDown, (float)ScreenParticleRenderer.getInstance().heightScaledDown, 0.0F, 1000.0F, 21000.0F/*net.minecraftforge.client.ForgeHooksClient.getGuiFarPlane()*/);
            RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

            ScreenParticleRenderer.getInstance().bindScaledDown();

            RenderSystem.clear(16640, Minecraft.ON_OSX);

            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
                guiScale = 1;
            }
            int croppedWidth = (int) (ScreenParticleRenderer.getInstance().widthScaledDown * guiScale);
            int croppedHeight = (int) (ScreenParticleRenderer.getInstance().heightScaledDown * guiScale);

            int centerX = ScreenParticleRenderer.getInstance().width / 2;
            int centerY = ScreenParticleRenderer.getInstance().height / 2;
            int x1 = centerX - (croppedWidth / 2);
            int x2 = centerX + (croppedWidth / 2);
            int y1 = centerY - (croppedHeight / 2);
            int y2 = centerY + (croppedHeight / 2);
            float minU = (float)x1 / (float)ScreenParticleRenderer.getInstance().width;
            float maxU = (float)x2 / (float)ScreenParticleRenderer.getInstance().width;
            float minV = (float)y1 / (float)ScreenParticleRenderer.getInstance().height;
            float maxV = (float)y2 / (float)ScreenParticleRenderer.getInstance().height;

            x1 = 0;
            x2 = ScreenParticleRenderer.getInstance().widthScaledDown;
            y1 = 0;
            y2 = ScreenParticleRenderer.getInstance().heightScaledDown;

            boolean useBlur = true;
            if (useBlur) {
                ScreenParticleRenderer.getInstance().innerBlitCustomShaderHorizontal(pGuiGraphics.pose()
                    , x1, x2
                    , y1, y2
                    , 0
                    , minU, maxU, minV, maxV);

                /*ScreenParticleRenderer.getInstance().innerBlitCustomShaderVertical(pGuiGraphics.pose()
                        , x1, x2
                        , y1, y2
                        , 0
                        , minU, maxU, minV, maxV);*/

                ScreenParticleRenderer.getInstance().innerBlitCustomShaderVertical(pGuiGraphics.pose()
                        , 0, ScreenParticleRenderer.getInstance().widthScaledDown
                        , 0, ScreenParticleRenderer.getInstance().heightScaledDown
                        , 0
                        , 0, 1, 0, 1);
            } else {
                ScreenParticleRenderer.getInstance().innerBlitCustomShader(pGuiGraphics.pose()
                        , x1, x2
                        , y1, y2
                        , 0
                        , minU, maxU, minV, maxV);
            }

            //getting data from scaled down framebuffer
            ByteBuffer pixelBuffer = getPixelDataFromFrameBuffer();

            boolean useThread = true;
            if (useThread) {
                processor.submitForProcessing(pixelBuffer);
            } else {
                ByteBuffer byteBuffer = compress(pixelBuffer);
                playerStatusLocal.getScreenData().setTexturePixelData(byteBuffer);
                WatutMod.getPlayerStatusManagerClient().sendScreenRenderData(playerStatusLocal);
            }

            ScreenParticleRenderer.getInstance().unbindScaledDown();

            bindVanillaRenderTargetAndSetupProjectionMatrix();
        }
    }

    public static ByteBuffer compress(ByteBuffer inputBuffer) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);

        // Copy ByteBuffer data into a byte array
        byte[] inputBytes = new byte[inputBuffer.remaining()];
        inputBuffer.get(inputBytes);
        deflater.setInput(inputBytes);
        deflater.finish();

        // Use a direct buffer for compressed data
        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(inputBytes.length + 512); // Allow extra space
        byte[] temp = new byte[1024];

        while (!deflater.finished()) {
            int compressedBytes = deflater.deflate(temp);
            if (outputBuffer.remaining() < compressedBytes) {
                // Expand the direct buffer dynamically
                ByteBuffer newBuffer = ByteBuffer.allocateDirect(outputBuffer.capacity() * 2);
                outputBuffer.flip();
                newBuffer.put(outputBuffer);
                outputBuffer = newBuffer;
            }
            outputBuffer.put(temp, 0, compressedBytes);
        }
        deflater.end();

        outputBuffer.flip(); // Prepare buffer for reading
        inputBuffer.flip();
        return outputBuffer;
    }

    public static ByteBuffer decompress(ScreenData screenData, ByteBuffer compressedBuffer, int expectedSize) throws Exception {
        Inflater inflater = new Inflater();

        // Copy compressed data into a byte array
        byte[] compressedBytes = new byte[compressedBuffer.remaining()];
        compressedBuffer.get(compressedBytes);
        inflater.setInput(compressedBytes);

        ByteBuffer decompressionBuffer = screenData.getDecompressionBuffer();

        // Use a direct buffer for decompressed data
        if (decompressionBuffer == null) {
            CULog.dbg("Creating new buffer for decompression");
            decompressionBuffer = MemoryUtil.memAlloc(expectedSize); // Allocate initial space
            screenData.setDecompressionBuffer(decompressionBuffer);
        } else {
            decompressionBuffer.clear(); // Reset the buffer for writing
        }

        byte[] temp = new byte[1024];

        while (!inflater.finished()) {
            int decompressedBytes = inflater.inflate(temp);

            // Ensure there is enough space in the buffer
            if (decompressionBuffer.remaining() < decompressedBytes) {
                // Resize the buffer by creating a new one with double the capacity
                int newCapacity = Math.max(decompressionBuffer.capacity() * 2, decompressionBuffer.capacity() + decompressedBytes);
                CULog.dbg("adjusting size of buffer for decompression");
                ByteBuffer newBuffer = MemoryUtil.memAlloc(newCapacity);
                decompressionBuffer.flip(); // Prepare for reading
                newBuffer.put(decompressionBuffer); // Copy old data to new buffer
                MemoryUtil.memFree(decompressionBuffer);
                decompressionBuffer = newBuffer;
                screenData.setDecompressionBuffer(decompressionBuffer);
            }

            decompressionBuffer.put(temp, 0, decompressedBytes);
        }
        inflater.end();

        decompressionBuffer.flip(); // Prepare buffer for reading

        return decompressionBuffer;
    }

    public static ByteBuffer decompress2(ScreenData screenData, ByteBuffer compressedBuffer, int expectedSize) throws Exception {
        Inflater inflater = new Inflater();

        // Copy compressed data into a byte array
        byte[] compressedBytes = new byte[compressedBuffer.remaining()];
        compressedBuffer.get(compressedBytes);
        inflater.setInput(compressedBytes);

        ByteBuffer decompressionBuffer = screenData.getDecompressionBuffer();

        // Use a direct buffer for decompressed data
        if (decompressionBuffer == null) {
            System.out.println("make new buffer");
            decompressionBuffer = ByteBuffer.allocateDirect(expectedSize); // Allocate space for expected size
            screenData.setDecompressionBuffer(decompressionBuffer);
        } else {
            decompressionBuffer.clear();
        }
        //ByteBuffer outputBuffer = ByteBuffer.allocateDirect(expectedSize); // Allocate space for expected size
        byte[] temp = new byte[1024];

        while (!inflater.finished()) {
            int decompressedBytes = inflater.inflate(temp);
            if (decompressionBuffer.remaining() < decompressedBytes) {
                throw new IllegalStateException("Decompressed size exceeds expected size!");
            }
            decompressionBuffer.put(temp, 0, decompressedBytes);
        }
        inflater.end();

        decompressionBuffer.flip(); // Prepare buffer for reading
        return decompressionBuffer;
    }

    public static ByteBuffer decompressGZIP(ByteBuffer compressedBuffer) throws IOException {
        // Extract the byte array from the input ByteBuffer
        byte[] compressedBytes = new byte[compressedBuffer.remaining()];
        compressedBuffer.get(compressedBytes);

        // Use a ByteArrayInputStream to wrap the compressed data
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedBytes);

        // Create a GZIPInputStream for decompression
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);

        // Read decompressed data into a ByteArrayOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        // Close streams
        gzipInputStream.close();
        byteArrayInputStream.close();

        // Get the decompressed data as a byte array
        byte[] decompressedBytes = byteArrayOutputStream.toByteArray();

        // Create a direct ByteBuffer and put the decompressed data into it
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(decompressedBytes.length);
        directBuffer.put(decompressedBytes);
        directBuffer.flip(); // Flip the buffer to prepare it for reading

        return directBuffer;
    }

    public static ByteBuffer compressGZIP(ByteBuffer inputBuffer) throws IOException {
        // Extract bytes from the input ByteBuffer
        byte[] inputBytes = new byte[inputBuffer.remaining()];
        inputBuffer.get(inputBytes);

        // Create a ByteArrayOutputStream to hold the compressed data
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Use GZIPOutputStream to compress the data
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(inputBytes);
        }

        // Get the compressed data as a byte array
        byte[] compressedBytes = byteArrayOutputStream.toByteArray();

        // Wrap the compressed data in a ByteBuffer and return it
        return ByteBuffer.wrap(compressedBytes);
    }

    public static ByteBuffer getPixelDataFromFrameBuffer() {
        int width = ScreenParticleRenderer.getInstance().widthScaledDown;
        int height = ScreenParticleRenderer.getInstance().heightScaledDown;

        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * ScreenParticleRenderer.bytesPerPixel); // RGBA = 4 bytes per pixel
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);

        return pixelBuffer;
    }

    public static boolean validatePixelByteBuffer(ByteBuffer byteBuffer, int expectedSize, int expectedAlignment) {

        if (byteBuffer == null) return false;

        if (byteBuffer.limit() != expectedSize) {
            return false;
        }

        //GL30.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, expectedAlignment);

        return true;
    }

}
