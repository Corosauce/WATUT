package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.PlayerStatus;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigCommon;
import com.corosus.watut.config.ConfigServerSyncedToClient;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import xaero.common.graphics.ImprovedFramebuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

public class RenderHelper {

    public static boolean performingOwnRender = false;
    public static ResourceLocation cursor = new ResourceLocation(WatutMod.MODID, "textures/misc/mouse.png");
    //public static ImprovedFramebuffer GuiMapPrimaryScaleFBO;
    public static Field guiMapPrimaryScaleFBO;
    public static Field colorTextureId;
    public static Class guiMap;
    public static Class improvedFramebuffer;
    public static int xaeroWorldMapTextureID = -1;
    public static ByteBuffer testBuffer = null;

    public static long pixels = 0L;
    public static long size = 0;

    public static GuiGraphics temp = null;

    public static Field pixelsField;

    static {
        try {
            pixelsField = NativeImage.class.getDeclaredField("pixels");
            pixelsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                pixelsField = NativeImage.class.getDeclaredField("f_84964_");
                pixelsField.setAccessible(true);
            } catch (NoSuchFieldException ex) {
                e.printStackTrace();
            }

        }
    }

    public static void testCopy(ByteBuffer buffer) {
        /*if (testBuffer != null) {
            MemoryUtil.memFree(testBuffer);
        }*/
        //buffer.flip();

        // Allocate a direct ByteBuffer with the same capacity as the original
        //ByteBuffer copiedBuffer = ByteBuffer.allocateDirect(buffer.capacity());
        //ByteBuffer copiedBuffer = MemoryUtil.memAlloc(buffer.capacity());
        if (pixels == 0L) {
            pixels = MemoryUtil.nmemAlloc(buffer.capacity());
        }

        ByteBuffer copiedBuffer = MemoryUtil.memByteBuffer(pixels, buffer.capacity());


                // Copy content from the original ByteBuffer to the new direct ByteBuffer
        copiedBuffer.put(buffer);

        // Reset position for reading in the copied buffer
        copiedBuffer.flip();

        copiedBuffer.limit(buffer.limit());

        testBuffer = copiedBuffer;
    }

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
            if (guiMap != null) {
                guiMapPrimaryScaleFBO = guiMap.getDeclaredField("primaryScaleFBO");
                guiMapPrimaryScaleFBO.setAccessible(true);
            }
            if (improvedFramebuffer != null) {
                colorTextureId = improvedFramebuffer.getDeclaredField("colorTextureId");
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static boolean isXaeroGuiMap(Screen screen) {
        if (guiMap == null) return false;
        return guiMap.isInstance(screen);
    }

    public static ByteBufferProcessor processor = new ByteBufferProcessor(buffer -> {
        ByteBuffer processed = compress(buffer);
        return processed;
    });

    public static void guiRender(GuiGraphics guiGraphics) {
        long gameTime = 0;
        if (Minecraft.getInstance().level != null) {
            gameTime = Minecraft.getInstance().level.getGameTime();
        }

        boolean hasUnboundVanillaMainTarget = false;

        for (PlayerStatus playerStatus : WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
            ScreenData screenData = playerStatus.getScreenData();

            if ((screenData.needsNewRender() && screenData.getTexturePixelData() != null/* && screenData.getGameTicksSinceLastScreenReceiveAndRender() + ConfigClient.tickReceiveAndRenderRateOfGUIUpdates < gameTime*/)) {
                /*if (!screenData.getIsBufferReady().get()) {
                    System.out.println("buffer wasnt ready, prevented issue");
                    continue;
                }*/
                //screenData.markNeedsNewRender(false);
                screenData.setGameTicksSinceLastScreenReceiveAndRender(gameTime);

                ScreenParticleRenderer.getInstance().checkSetup();
                if (!hasUnboundVanillaMainTarget) {
                    hasUnboundVanillaMainTarget = true;
                    unbindVanillaRenderTarget();
                }

                Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)ScreenParticleRenderer.getInstance().widthScaledDown, (float)ScreenParticleRenderer.getInstance().heightScaledDown, 0.0F, 1000.0F, net.minecraftforge.client.ForgeHooksClient.getGuiFarPlane());
                RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

                ScreenParticleRenderer.getInstance().bindScaledDownFromByteBuffer();
                RenderSystem.clear(16640, Minecraft.ON_OSX);

                //setup a new texture, borrowing relevant bits from RenderTarget class
                //TODO: im binding each new texture to the same framebuffer....... how does this play out in multiplayer? were already having issues
                if (playerStatus.getScreenData().getTextureID() == -1) {
                    int texture = GL11.glGenTextures();
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

                    //setup the texture
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                            ScreenParticleRenderer.getInstance().widthScaledDown,
                            ScreenParticleRenderer.getInstance().heightScaledDown, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

                    //from code example / RenderTarget via setFilterMode
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

                    //from RenderTarget via createBuffers when prepping a new texture
                    GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                    GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                    //this binds the texture id to the active framebuffer (scaled down framebuffer), result is anything rendered to it is stored in this texture id
                    /*GL30.glFramebufferTexture2D(
                            GL30.GL_FRAMEBUFFER,
                            GL30.GL_COLOR_ATTACHMENT0,
                            GL11.GL_TEXTURE_2D,
                            ScreenParticleRenderer.getInstance().getMainRenderTargetScaledDownFromByteBuffer().getColorTextureId(),
                            0 // Mipmap level
                    );*/

                    if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE) {
                        System.out.println("Framebuffer is complete and ready to use.");
                    } else {
                        System.err.println("Framebuffer is not complete!");
                    }

                    System.out.println(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));

                    playerStatus.getScreenData().setTextureID(texture);
                }

                GL30.glFramebufferTexture2D(
                        GL30.GL_FRAMEBUFFER,
                        GL30.GL_COLOR_ATTACHMENT0,
                        GL11.GL_TEXTURE_2D,
                        playerStatus.getScreenData().getTextureID(),
                        0 // Mipmap level
                );

                if (playerStatus.getScreenData().getParticleRenderType() == null) {
                    playerStatus.getScreenData().initClient();
                }

                //GL11.glBindTexture(GL11.GL_TEXTURE_2D, playerStatus.getScreenData().getTextureID());

                RenderSystem.clear(16640, Minecraft.ON_OSX);

                /*if (screenData.getImage() != null) {
                    screenData.getImage().upload();
                }*/

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, screenData.getImage().getId());

                /*pGuiGraphics.innerBlit(testing.res
                        , 0, 512
                        , 0, 512
                        , 0
                        , 0, 1, 0, 1);*/

                /*PoseStack pose = new PoseStack();

                if (temp != null) {
                    pose = temp.pose();
                }*/

                /*ScreenParticleRenderer.getInstance().innerBlitCustomShader2(screenData.getTextureID(), guiGraphics.pose()
                        , 0, ScreenParticleRenderer.getInstance().widthScaledDown
                        , 0, ScreenParticleRenderer.getInstance().heightScaledDown
                        , 0
                        , 0, 1, 0, 1);*/

                long nativeImagePixelMemoryAddress = -1;
                try {
                    nativeImagePixelMemoryAddress = (Long)pixelsField.get(screenData.getImage().getPixels());
                    if (nativeImagePixelMemoryAddress != -1) {
                        MemoryUtil.memCopy(MemoryUtil.memAddress(screenData.getDecompressionBuffer()), nativeImagePixelMemoryAddress, 512*512*4/*intermediatePixelBuffer.capacity()*/);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                screenData.getImage().upload();

                guiGraphics.innerBlit(screenData.res
                        , 0, 512
                        , 0, 512
                        , 0
                        , 0, 1, 0, 1);

                //test
                /*if (temp != null && Minecraft.getInstance().screen != null) {
                    Minecraft.getInstance().screen.renderWithTooltip(temp, 0, 0, 0);
                }*/




                //System.out.println("screenData.getTexturePixelData().limit() " + screenData.getTexturePixelData().limit());
                /*if (testBuffer == null) {
                    System.out.println("make copy");
                    testCopy(screenData.getTexturePixelData());
                    System.out.println("screenData.getTexturePixelData().limit() " + screenData.getTexturePixelData().limit());
                    //System.out.println("testBuffer.limit() " + testBuffer.limit());
                }*/
                if (true || validatePixelByteBuffer(screenData.getTexturePixelData(),
                        ScreenParticleRenderer.getInstance().widthScaledDown * ScreenParticleRenderer.getInstance().heightScaledDown * 4,
                        4)) {
                    //System.out.println("glTexImage2D");
                    /*for (int i = 0; i < 100; i++) {
                        GL11.glTexImage2D(
                                GL11.GL_TEXTURE_2D,
                                0, // Mipmap level
                                GL11.GL_RGBA, // Internal format
                                ScreenParticleRenderer.getInstance().widthScaledDown,
                                ScreenParticleRenderer.getInstance().heightScaledDown,
                                0, // Border
                                GL11.GL_RGBA, // Data format
                                GL11.GL_UNSIGNED_BYTE, // Data type
                                screenData.getTexturePixelData()
                        );
                    }*/
                    /*ByteBuffer test = ByteBuffer.allocateDirect(ScreenParticleRenderer.getInstance().widthScaledDown * ScreenParticleRenderer.getInstance().heightScaledDown * 4);
                    test.flip();*/

                    //System.out.println("screenData.getTexturePixelData().limit() " + screenData.getTexturePixelData().limit());
                    //System.out.println("w " + ScreenParticleRenderer.getInstance().widthScaledDown + ", h " + ScreenParticleRenderer.getInstance().heightScaledDown);

                    /*ByteBuffer copiedBuffer = ByteBuffer.allocateDirect(ScreenParticleRenderer.getInstance().widthScaledDown * ScreenParticleRenderer.getInstance().heightScaledDown * 4);
                    copiedBuffer.flip();*/

                    /*GL11.glTexImage2D(
                            GL11.GL_TEXTURE_2D,
                            0, // Mipmap level
                            GL11.GL_RGBA, // Internal format
                            ScreenParticleRenderer.getInstance().widthScaledDown,
                            ScreenParticleRenderer.getInstance().heightScaledDown,
                            0, // Border
                            GL11.GL_RGBA, // Data format
                            GL11.GL_UNSIGNED_BYTE, // Data type
                            screenData.getTexturePixelData()
                    );*/

                    /*GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                            ScreenParticleRenderer.getInstance().widthScaledDown,
                            ScreenParticleRenderer.getInstance().heightScaledDown, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);*/

                    /*GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                            ScreenParticleRenderer.getInstance().widthScaledDown,
                            ScreenParticleRenderer.getInstance().heightScaledDown, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);*/

                    /*GL11.glTexImage2D(
                            GL11.GL_TEXTURE_2D,
                            0, // Mipmap level
                            GL11.GL_RGBA, // Internal format
                            ScreenParticleRenderer.getInstance().widthScaledDown,
                            ScreenParticleRenderer.getInstance().heightScaledDown,
                            0, // Border
                            GL11.GL_RGBA, // Data format
                            GL11.GL_UNSIGNED_BYTE, // Data type
                            testBuffer
                    );*/
                    /*GL11.glTexImage2D(
                            GL11.GL_TEXTURE_2D,
                            0, // Mipmap level
                            GL11.GL_RGBA, // Internal format
                            ScreenParticleRenderer.getInstance().widthScaledDown,
                            ScreenParticleRenderer.getInstance().heightScaledDown,
                            0, // Border
                            GL11.GL_RGBA, // Data format
                            GL11.GL_UNSIGNED_BYTE, // Data type
                            pixels
                    );*/
                    //mark it so network thread can update it again
                    playerStatus.getScreenData().getIsBufferReady().set(false);
                } else {
                    CULog.dbg("ERROR: invalid bytebuffer, avoiding glTexImage2D");
                }

                ScreenParticleRenderer.getInstance().unbindScaledDownFromByteBuffer();
            }
        }

        if (hasUnboundVanillaMainTarget) {
            bindVanillaRenderTargetAndSetupProjectionMatrix();
        }
    }

    public static void bindVanillaRenderTargetAndSetupProjectionMatrix() {
        Window window = Minecraft.getInstance().getWindow();
        Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)((double)window.getWidth() / window.getGuiScale()), (float)((double)window.getHeight() / window.getGuiScale()), 0.0F, 1000.0F, net.minecraftforge.client.ForgeHooksClient.getGuiFarPlane());
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    public static void unbindVanillaRenderTarget() {
        Minecraft.getInstance().getMainRenderTarget().unbindWrite();
    }

    public static synchronized void renderWithTooltipEnd(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (ConfigServerSyncedToClient.useOldSimpleGUIVisual) return;
        if (ConfigClient.dontSendDetailedGUIInfo) return;

        temp = pGuiGraphics;

        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        long gameTime = 0;
        if (Minecraft.getInstance().level != null) {
            gameTime = Minecraft.getInstance().level.getGameTime();
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

        boolean needsScreenUpdate = false;

        if (!playerStatusLocal.isIdle()) {
            if (true || playerStatusLocal.getScreenData().getGameTicksSinceLastScreenSend() + ConfigServerSyncedToClient.tickSendRateOfGUIUpdates < gameTime) {
                playerStatusLocal.setLastScreenCaptured(playerStatusLocal.getPlayerGuiState());
                if (Minecraft.getInstance().screen != null && playerStatusLocal.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE && playerStatusLocal.getPlayerGuiState() != PlayerStatus.PlayerGuiState.CHAT_SCREEN) {
                    playerStatusLocal.getScreenData().setGameTicksSinceLastScreenSend(gameTime);
                    needsScreenUpdate = true;
                }
            }
        }

        if (needsScreenUpdate && !processor.hasWork()) {

            ScreenParticleRenderer.getInstance().checkSetup();
            unbindVanillaRenderTarget();
            ScreenParticleRenderer.getInstance().bind();

            RenderSystem.clear(16640, Minecraft.ON_OSX);

            if (Minecraft.getInstance().screen != null) {
                ScreenParticleRenderer.isRenderingParticleGUI = true;
                ScreenParticleRenderer.isRenderingParticleGUI2 = true;
                performingOwnRender = true;
                //System.out.println(Minecraft.getInstance().screen);
                //if (Minecraft.getInstance().screen instanceof GuiMap) {
                    //(Minecraft.getInstance().screen).render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
                //} else {
                    //Minecraft.getInstance().screen.renderWithTooltip(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
                //}

                xaeroWorldMapTextureID = -1;

                //if (Minecraft.getInstance().screen instanceof GuiMap) {
                if (false && isXaeroGuiMap(Minecraft.getInstance().screen)) {
                    //ScreenParticleRenderer.getInstance().bind();
                    try {
                        Object fbo = guiMapPrimaryScaleFBO.get(null);
                        if (fbo != null) {
                            Object colorTextureIdObj = colorTextureId.get(fbo);
                            if (colorTextureIdObj != null) {
                                xaeroWorldMapTextureID = (int) colorTextureIdObj;
                            }
                            //((ImprovedFramebuffer)fbo).bindAsMainTarget(false);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    Minecraft.getInstance().screen.renderWithTooltip(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
                }
                pGuiGraphics.innerBlit(cursor, pMouseX, pMouseX + 6, pMouseY, pMouseY + 10, 100, 0, 1, 0, 1);
                performingOwnRender = false;
                //ScreenParticleRenderer.isRenderingParticleGUI = false;
                //ScreenParticleRenderer.isRenderingParticleGUI2 = false;
            }

            ScreenParticleRenderer.getInstance().unbind();

            Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)ScreenParticleRenderer.getInstance().widthScaledDown, (float)ScreenParticleRenderer.getInstance().heightScaledDown, 0.0F, 1000.0F, net.minecraftforge.client.ForgeHooksClient.getGuiFarPlane());
            RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

            ScreenParticleRenderer.getInstance().bindScaledDown();

            RenderSystem.clear(16640, Minecraft.ON_OSX);

            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            int croppedWidth = (int) (512 * guiScale);
            int croppedHeight = (int) (512 * guiScale);

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
        //System.out.println("decompress");
        Inflater inflater = new Inflater();

        // Copy compressed data into a byte array
        byte[] compressedBytes = new byte[compressedBuffer.remaining()];
        compressedBuffer.get(compressedBytes);
        inflater.setInput(compressedBytes);

        ByteBuffer decompressionBuffer = screenData.getDecompressionBuffer();

        // Use a direct buffer for decompressed data
        if (decompressionBuffer == null) {
            System.out.println("Creating new buffer");
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
                System.out.println("new ByteBuffer.allocateDirect");
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

        screenData.test();

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

        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4); // RGBA = 4 bytes per pixel
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
