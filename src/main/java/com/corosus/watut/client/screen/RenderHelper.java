package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.PlayerStatus;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.mixin.client.AbstractContainerScreenAccessor;
import com.corosus.watut.mixin.client.NativeImageAccessor;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class RenderHelper {

    public static boolean performingOwnRender = false;
    public static Identifier cursor = Identifier.fromNamespaceAndPath(WatutMod.MODID, "textures/misc/mouse.png");

    public static volatile boolean captureRequested = false;
    public static int captureMouseX = 0;
    public static int captureMouseY = 0;

    //xaero minimap support (doesnt capture extra elements just terrain)
    public static Class guiMap;
    public static Class improvedFramebuffer;
    public static int xaeroWorldMapTextureID = -1;

    public static boolean CAPTURE_DEBUG = false;
    public static Field guiMapPrimaryScaleFBO;
    public static Field colorTextureId;
    public static Method improvedFramebufferGetTas;

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
            try {
                improvedFramebuffer = Class.forName("xaero.map.common.ImprovedFramebuffer");
            } catch (ClassNotFoundException e2) {
                //e.printStackTrace();
            }
        }
        try {
            if (guiMap != null) {
                guiMapPrimaryScaleFBO = guiMap.getDeclaredField("primaryScaleFBO");
                guiMapPrimaryScaleFBO.setAccessible(true);
            }
            if (improvedFramebuffer != null) {
                try {
                    improvedFramebufferGetTas = improvedFramebuffer.getMethod("getTas");
                } catch (NoSuchMethodException e) {
                    try {
                        colorTextureId = improvedFramebuffer.getDeclaredField("colorTextureId");
                    } catch (NoSuchFieldException e2) {
                        CULog.log("watut: xaero minimap not installed or mod structure changed");
                    }
                }
            } else {
                CULog.log("watut: xaero minimap not installed or mod structure changed");
            }
        } catch (NoSuchFieldException e) {
            CULog.log("watut: xaero minimap not installed or mod structure changed");
            //e.printStackTrace();
        }
    }

    private static boolean xaeroReflectionLogged = false;

    public static void logXaeroReflectionOnce() {
        if (!CAPTURE_DEBUG || xaeroReflectionLogged) return;
        xaeroReflectionLogged = true;
        CULog.dbg("[watut-capture] xaero reflection: guiMap=" + (guiMap != null)
                + " framebuffer=" + (improvedFramebuffer == null ? "NONE" : improvedFramebuffer.getName())
                + " primaryScaleFBO=" + (guiMapPrimaryScaleFBO != null)
                + " handle=" + (improvedFramebufferGetTas != null ? "getTas()" : (colorTextureId != null ? "colorTextureId" : "NONE")));
    }

    public static void blitXaeroFboIntoCapture(RenderTarget captureTarget) {
        if (guiMapPrimaryScaleFBO == null) return;
        try {
            Object fbo = guiMapPrimaryScaleFBO.get(null);
            if (!(fbo instanceof RenderTarget fbTarget)) {
                if (CAPTURE_DEBUG) CULog.dbg("[watut-capture] xaero fb copy skipped, fbo=" + fbo);
                return;
            }
            GpuTexture src = fbTarget.getColorTexture();
            GpuTexture dst = captureTarget.getColorTexture();
            if (src == null || dst == null) {
                if (CAPTURE_DEBUG) CULog.dbg("[watut-capture] xaero fb copy skipped, src=" + src + " dst=" + dst);
                return;
            }
            int w = Math.min(fbTarget.width, captureTarget.width);
            int h = Math.min(fbTarget.height, captureTarget.height);
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(src, dst, 0, 0, 0, 0, 0, w, h);
            if (CAPTURE_DEBUG) CULog.dbg("[watut-capture] copied xaero fb " + fbTarget.width + "x" + fbTarget.height
                    + " -> capture target " + captureTarget.width + "x" + captureTarget.height + " at " + w + "x" + h);
        } catch (Exception e) {
            CULog.err("[watut-capture] xaero fb copy failed: " + e);
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

    public static ByteBufferProcessor<CapturedFrame> processor = new ByteBufferProcessor<>(frame -> {
        ByteBuffer processed = compress(frame.data());

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

        return new CapturedFrame(processed, frame.width(), frame.height());
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

    public static void guiRender() {
        long gameTime = 0;
        if (Minecraft.getInstance().level != null) {
            gameTime = Minecraft.getInstance().level.getGameTime();
        }

        for (PlayerStatus playerStatus : WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {

            ScreenData screenData = playerStatus.getScreenData();

            if ((screenData.getIsBufferReady().get() && screenData.needsNewRenderFromPixelData() && screenData.getTexturePixelData() != null && screenData.getGameTicksSinceLastScreenReceiveAndRender() + ConfigClient.tickReceiveAndRenderRateOfGUIUpdates < gameTime)) {
                screenData.markNeedsNewRenderFromPixelData(false);
                screenData.setGameTicksSinceLastScreenReceiveAndRender(gameTime);

                ByteBuffer decoded = screenData.getDecompressionBuffer();
                if (!ScreenData.isValidReceivedSize(screenData.getWidth(), screenData.getHeight())
                        || decoded == null || decoded.limit() != screenData.getWidth() * screenData.getHeight() * ScreenParticleRenderer.bytesPerPixel) {
                    CULog.dbg("WATUT: dropping screen frame, size " + screenData.getWidth() + "x" + screenData.getHeight() + " does not match decoded data " + (decoded != null ? decoded.limit() : -1));
                    screenData.getIsBufferReady().set(false);
                    continue;
                }

                ScreenParticleRenderer.getInstance().checkSetup();

                if (playerStatus.getScreenData().getParticleRenderType() == null) {
                    playerStatus.getScreenData().initClient();
                }

                if (screenData.getImage() == null) {
                    setScreenImage(screenData);
                } else {
                    //detect a resolution change and remake buffer, only used if experimental rendering of entire screen config is on
                    //CULog.dbg("screendata sizes " + screenData.getWidth() + " " + screenData.getHeight());
                    if (screenData.getImage().getPixels().getWidth() != screenData.getWidth() || screenData.getImage().getPixels().getHeight() != screenData.getHeight()) {
                        screenData.closeImage();
                        setScreenImage(screenData);
                        CULog.dbg("screendata image resized to " + screenData.getWidth() + " " + screenData.getHeight());
                    }
                }
                //hack into NativeImage to directly inject pixel data, working around its requirements for a PNG format parse
                long nativeImagePixelMemoryAddress = ((NativeImageAccessor)((Object)screenData.getImage().getPixels())).pixels();
                if (nativeImagePixelMemoryAddress != -1 && nativeImagePixelMemoryAddress != 0) {
                    MemoryUtil.memCopy(MemoryUtil.memAddress(screenData.getDecompressionBuffer()), nativeImagePixelMemoryAddress,
                            screenData.getWidth() * screenData.getHeight() * ScreenParticleRenderer.bytesPerPixel);
                }

                screenData.getImage().upload();

                playerStatus.getScreenData().getIsBufferReady().set(false);

            }
        }
    }

    private static void setScreenImage(ScreenData screenData) {
        DynamicTexture texture = new DynamicTexture(() -> "watut_image", screenData.getWidth(), screenData.getHeight(), true);
        screenData.setImage(texture);
        Minecraft.getInstance().getTextureManager().register(screenData.getTextureId(), texture);
    }

    public static boolean useDynamicGUISystem() {
        if (ConfigServerControlledSyncedToClient.dynamicGuiUseOldSimpleGUIVisual) return false;
        if (ConfigClient.dontSendDetailedGUIInfo) return false;
        return true;
    }

    private static String screenBlacklistSource = null;
    private static List<String> screenBlacklistExact = List.of();
    private static List<String> screenBlacklistPrefixes = List.of();

    public static boolean isScreenBlacklisted(@Nullable Screen screen) {
        if (screen == null) return false;
        String source = ConfigServerControlledSyncedToClient.dynamicGuiScreenBlacklist;
        if (!Objects.equals(source, screenBlacklistSource)) {
            parseScreenBlacklist(source);
        }
        if (screenBlacklistExact.isEmpty() && screenBlacklistPrefixes.isEmpty()) return false;

        String className = screen.getClass().getName();
        if (screenBlacklistExact.contains(className)) return true;
        for (String prefix : screenBlacklistPrefixes) {
            if (className.startsWith(prefix)) return true;
        }
        return false;
    }

    private static void parseScreenBlacklist(@Nullable String source) {
        List<String> exact = new ArrayList<>();
        List<String> prefixes = new ArrayList<>();
        if (source != null) {
            for (String entry : source.split("[,\\n]")) {
                entry = entry.trim();
                if (entry.isEmpty() || entry.startsWith("#")) continue;
                if (entry.endsWith(".")) {
                    prefixes.add(entry);
                } else {
                    exact.add(entry);
                }
            }
        }
        screenBlacklistExact = exact;
        screenBlacklistPrefixes = prefixes;
        screenBlacklistSource = source;
    }

    public static final int STANDARD_GUI_WIDTH = 176;
    public static final int STANDARD_GUI_HEIGHT = 166;
    private static final int CREATIVE_TAB_OVERHANG = 28;

    /**
     * Finding out the size of a screen by checking all its elements, getting the mins and maxs
     * @param screen
     * @return
     */
    public static @Nullable ScreenRectangle getGuiContentBounds(@Nullable Screen screen) {
        if (screen == null) return null;
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) containerScreen;
            int top = accessor.watut_getTopPos();
            int height = accessor.watut_getImageHeight();
            if (screen instanceof CreativeModeInventoryScreen) {
                top -= CREATIVE_TAB_OVERHANG;
                height += CREATIVE_TAB_OVERHANG * 2;
            }
            return new ScreenRectangle(accessor.watut_getLeftPos(), top, accessor.watut_getImageWidth(), height);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && !widget.visible) continue;
            ScreenRectangle rect;
            if (child instanceof AbstractSelectionList<?> list) {
                rect = new ScreenRectangle(list.getRowLeft(), list.getY(), list.getRowWidth(), list.getHeight());
            } else {
                rect = child.getRectangle();
            }
            if (rect.width() <= 0 || rect.height() <= 0) continue;
            minX = Math.min(minX, rect.left());
            minY = Math.min(minY, rect.top());
            maxX = Math.max(maxX, rect.right());
            maxY = Math.max(maxY, rect.bottom());
        }
        if (minX == Integer.MAX_VALUE) return null;
        return new ScreenRectangle(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Variable scaling for guis larger than viewable area
     * @param content
     * @param screenGuiHeight
     * @return
     */
    public static float getGuiContentScale(ScreenRectangle content, int screenGuiHeight) {
        float middleY = screenGuiHeight / 2F;
        float symmetricHeight = 2 * Math.max(middleY - content.top(), content.bottom() - middleY);
        float scaleX = content.width() / (float) STANDARD_GUI_WIDTH;
        float scaleY = symmetricHeight / STANDARD_GUI_HEIGHT;
        return Math.max(1F, Math.max(scaleX, scaleY));
    }

    public static synchronized void renderWithTooltipEnd(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (!useDynamicGUISystem()) return;
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null || Minecraft.getInstance().gui == null) {
            return;
        }
        //fallback to simple gui if blacklisted, so skip all this
        if (isScreenBlacklisted(Minecraft.getInstance().gui.screen())) return;

        PlayerStatus playerStatusLocal = WatutMod.getPlayerStatusManagerClient().getStatusLocal();

        if (processor.hasProcessedBuffers()) {
            try {
                CapturedFrame result = processor.getProcessedBuffer();
                if (result != null) {
                    ScreenData screenDataLocal = playerStatusLocal.getScreenData();
                    screenDataLocal.setTexturePixelData(result.data());
                    WatutMod.getPlayerStatusManagerClient().sendScreenRenderData(playerStatusLocal, result.width(), result.height());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        boolean needsScreenUpdate = playerStatusLocal.getScreenData().isNeedsNewRenderToPixelData();

        if (CAPTURE_DEBUG && needsScreenUpdate && processor.hasWork()) {
            CULog.dbg("[watut-capture] rearm flagged but processor queue busy, queueSize=" + processor.getQueueSize());
        }

        if (needsScreenUpdate && !processor.hasWork()) {
            logXaeroReflectionOnce();
            playerStatusLocal.getScreenData().setNeedsNewRenderToPixelData(false);
            ScreenParticleRenderer.getInstance().checkSetup();

            int detectedXaeroSentinel = -1;
            String xaeroDebugDesc = "";
            if (Minecraft.getInstance().gui.screen() != null && isXaeroGuiMap(Minecraft.getInstance().gui.screen())) {
                try {
                    Object fbo = guiMapPrimaryScaleFBO.get(null);
                    if (fbo == null) {
                        xaeroDebugDesc = "primaryScaleFBO is NULL (map never allocated?)";
                    } else {
                        String size = "?x?";
                        try {
                            size = improvedFramebuffer.getMethod("getWidth").invoke(fbo) + "x" + improvedFramebuffer.getMethod("getHeight").invoke(fbo);
                        } catch (Exception e) {
                        }
                        if (improvedFramebufferGetTas != null) {
                            if (improvedFramebufferGetTas.invoke(fbo) != null) {
                                detectedXaeroSentinel = 1;
                            }
                            xaeroDebugDesc = "fb " + size + " getTas()=" + (detectedXaeroSentinel == 1 ? "present" : "NULL");
                        } else if (colorTextureId != null) {
                            Object colorTextureIdObj = colorTextureId.get(fbo);
                            if (colorTextureIdObj != null) {
                                detectedXaeroSentinel = (int) colorTextureIdObj;
                            }
                            xaeroDebugDesc = "fb " + size + " colorTextureId=" + colorTextureIdObj;
                        }
                    }
                } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            if (CAPTURE_DEBUG && detectedXaeroSentinel != xaeroWorldMapTextureID) {
                CULog.dbg("[watut-capture] xaero sentinel " + xaeroWorldMapTextureID + " -> " + detectedXaeroSentinel
                        + " (" + (xaeroDebugDesc.isEmpty() ? "no xaero screen" : xaeroDebugDesc) + ")");
            }
            xaeroWorldMapTextureID = detectedXaeroSentinel;

            captureRequested = true;
            captureMouseX = pMouseX;
            captureMouseY = pMouseY;
        }
    }

    public static ByteBuffer compress(ByteBuffer inputBuffer) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
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

            outputBuffer.flip(); // Prepare buffer for reading
            inputBuffer.flip();
            return outputBuffer;
        } finally {
            deflater.end();
        }
    }

    public static ByteBuffer decompress(ScreenData screenData, ByteBuffer compressedBuffer, int expectedSize) throws Exception {
        if (expectedSize <= 0) {
            throw new DataFormatException("invalid expected size " + expectedSize);
        }
        screenData.getIsBufferReady().set(false);
        Inflater inflater = new Inflater();
        try {
            // Copy compressed data into a byte array
            byte[] compressedBytes = new byte[compressedBuffer.remaining()];
            compressedBuffer.get(compressedBytes);
            inflater.setInput(compressedBytes);

            ByteBuffer decompressionBuffer = screenData.getDecompressionBuffer();
            if (decompressionBuffer == null || decompressionBuffer.capacity() != expectedSize) {
                if (decompressionBuffer != null) {
                    MemoryUtil.memFree(decompressionBuffer);
                }
                CULog.dbg("Creating new buffer for decompression");
                decompressionBuffer = MemoryUtil.memAlloc(expectedSize);
                screenData.setDecompressionBuffer(decompressionBuffer);
            }
            decompressionBuffer.clear();

            byte[] temp = new byte[1024];
            while (!inflater.finished()) {
                int decompressedBytes = inflater.inflate(temp);
                if (decompressedBytes == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw new DataFormatException("truncated screen data");
                }
                if (decompressionBuffer.remaining() < decompressedBytes) {
                    throw new DataFormatException("screen data inflates past " + expectedSize + " bytes");
                }
                decompressionBuffer.put(temp, 0, decompressedBytes);
            }
            if (decompressionBuffer.position() != expectedSize) {
                throw new DataFormatException("screen data inflated to " + decompressionBuffer.position() + " bytes, expected " + expectedSize);
            }

            decompressionBuffer.flip(); // Prepare buffer for reading
            return decompressionBuffer;
        } finally {
            inflater.end();
        }
    }

    @Deprecated
    public static ByteBuffer getPixelDataFromFrameBuffer() {
        throw new UnsupportedOperationException("glReadPixels not available in 1.21.5 - use readPixelDataAsync");
    }

    public static boolean validatePixelByteBuffer(ByteBuffer byteBuffer, int expectedSize, int expectedAlignment) {

        if (byteBuffer == null) return false;

        if (byteBuffer.limit() != expectedSize) {
            return false;
        }

        //GL30.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, expectedAlignment);

        return true;
    }

    @Deprecated
    public static void readPixelsTest() {
    }

}
