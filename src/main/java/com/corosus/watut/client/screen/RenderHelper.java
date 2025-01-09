package com.corosus.watut.client.screen;

import com.corosus.watut.PlayerStatus;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.JSONLoader;
import com.corosus.watut.config.JsonObjects.ScreenRule;
import com.corosus.watut.mixin.client.ScreenRenderBackground;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RenderHelper {

    public static HashMap<RenderCallType, Method> lookupRenderCallsToMethod = new HashMap<>();

    static {
        try {
            lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT, GuiGraphics.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class));
            lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT2, GuiGraphics.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class));
            lookupRenderCallsToMethod.put(RenderCallType.POSE_PUSH, PoseStack.class.getDeclaredMethod("pushPose"));
            lookupRenderCallsToMethod.put(RenderCallType.POSE_POP, PoseStack.class.getDeclaredMethod("popPose"));
            lookupRenderCallsToMethod.put(RenderCallType.POSE_TRANSLATE_F, PoseStack.class.getDeclaredMethod("translate", float.class, float.class, float.class));
            lookupRenderCallsToMethod.put(RenderCallType.POSE_TRANSLATE_D, PoseStack.class.getDeclaredMethod("translate", double.class, double.class, double.class));

            lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT_BLUR, ScreenParticleRenderer.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, PoseStack.class, ScreenRule.class));
            lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT_BLUR2, ScreenParticleRenderer.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, PoseStack.class, ScreenRule.class));
        } catch (NoSuchMethodException e) {
            //e.printStackTrace();
            try {
                lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT, GuiGraphics.class.getDeclaredMethod("m_280444_", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class));
                lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT2, GuiGraphics.class.getDeclaredMethod("m_280479_", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class));

                lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT_BLUR, ScreenParticleRenderer.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, PoseStack.class, ScreenRule.class));
                lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT_BLUR2, ScreenParticleRenderer.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, PoseStack.class, ScreenRule.class));
            } catch (NoSuchMethodException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    public static void renderWithTooltipEnd(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        for (PlayerStatus playerStatus : WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
            ScreenData screenData = playerStatus.getScreenData();

            int gameTime = 0;
            if (Minecraft.getInstance().level != null) {
                gameTime = (int) Minecraft.getInstance().level.getGameTime();
            }

            //needsNewRender can be true during initial minecraft load due to ScreenParticleRenderer.resize getting triggered marking a new rerender need, so we just check if theres data too
            if ((ScreenParticleRenderer.getInstance().needsNewRender()/* || gameTime % 20 == 0*/) && screenData.getListRenderCalls().size() > 0 && Minecraft.getInstance().screen != null/* || true*/) {
                ScreenParticleRenderer.getInstance().markNeedsNewRender(false);

                ScreenParticleRenderer.getInstance().checkSetup();
                Minecraft.getInstance().getMainRenderTarget().unbindWrite();
                ScreenParticleRenderer.getInstance().bind();

                RenderSystem.clear(16640, Minecraft.ON_OSX);

                String screenName = screenData.getScreenClass();
                ScreenRule screenRule = null;
                if (screenName != null && !screenName.isEmpty()) {
                    screenRule = JSONLoader.getInstance().getAllGuiOverrideConfigs().getScreenRuleByClass(screenName);
                }

                String renderType = ScreenRule.RenderTypes.GENERIC_INVENTORY;
                if (ConfigClient.moddedGUIDefaultVisual == "DYNAMIC") {
                    renderType = ScreenRule.RenderTypes.BIGGEST_TEXTURE;
                }

                if (screenRule != null) {
                    renderType = screenRule.getRenderType();
                }

                //System.out.println("render type " + renderType);

                boolean test = true;

                if (test) {
                    if (Minecraft.getInstance().screen != null) {
                        //ForgeHooksClient.drawScreen(Minecraft.getInstance().screen, pGuiGraphics, pMouseX, pMouseY, pPartialTick);

                        ScreenParticleRenderer.isRenderingParticleGUI = true;
                        ScreenParticleRenderer.isRenderingParticleGUI2 = true;
                        Window window = Minecraft.getInstance().getWindow();
                        //pGuiGraphics.fillGradient(0, 0, window.getWidth(), window.getHeight(), -1072689136, -0);
                        Minecraft.getInstance().screen.renderWithTooltip(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
                        //ScreenParticleRenderer.isRenderingParticleGUI = false;
                        //ScreenParticleRenderer.isRenderingParticleGUI2 = false;

                        //readPixelsTest();
                    }
                } else {
                    if (renderType.equals(ScreenRule.RenderTypes.ALL_TEXTURES)) {
                        for (RenderCall renderCall : screenData.getListRenderCalls()) {
                            List<Object> listParams = renderCall.getListParamsCopy();
                            if (renderCall.getRenderCallType() == RenderCallType.INNER_BLIT_BLUR || renderCall.getRenderCallType() == RenderCallType.INNER_BLIT_BLUR2) {
                                listParams.add(pGuiGraphics.pose());
                                listParams.add(screenRule);
                            }

                            //System.out.println("size x: " + ((float)listParams.get(7) * 256) + " - " + "size y: " + ((float)listParams.get(9) * 256));
                            Object instance = ScreenParticleRenderer.getInstance();
                            try {
                                //lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(pGuiGraphics, listParams.toArray());
                                //TODO: this spits out java.lang.IllegalArgumentException while escape menu is open AND resizing the screen

                                if (renderCall.getRenderCallType() == RenderCallType.POSE_PUSH
                                        || renderCall.getRenderCallType() == RenderCallType.POSE_POP
                                        || renderCall.getRenderCallType() == RenderCallType.POSE_TRANSLATE_F
                                        || renderCall.getRenderCallType() == RenderCallType.POSE_TRANSLATE_D) {
                                    instance = pGuiGraphics.pose();
                                }

                                lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(instance, listParams.toArray());
                            } catch (Exception e) {
                                System.out.println("instance " + instance);
                                System.out.println("RenderCallType " + renderCall.getRenderCallType());
                                System.out.println("params before render: " + listParams);
                                e.printStackTrace();
                            }
                        }
                    } else if (renderType.equals(ScreenRule.RenderTypes.BIGGEST_TEXTURE)) {

                        int biggestUVRenderCallIndex = -1;
                        float biggest = 0;
                        for (int i = 0; i < screenData.getListRenderCalls().size(); i++) {
                            RenderCall renderCall = screenData.getListRenderCalls().get(i);
                            if (renderCall.getRenderCallType() != RenderCallType.INNER_BLIT_BLUR || renderCall.getRenderCallType() != RenderCallType.INNER_BLIT_BLUR2) {
                                continue;
                            }
                            float x1 = (int) renderCall.getListParams().get(1);
                            float x2 = (int) renderCall.getListParams().get(2);
                            float y1 = (int) renderCall.getListParams().get(3);
                            float y2 = (int) renderCall.getListParams().get(4);
                            float minU = (float) renderCall.getListParams().get(6);
                            float maxU = (float) renderCall.getListParams().get(7);
                            float minV = (float) renderCall.getListParams().get(8);
                            float maxV = (float) renderCall.getListParams().get(9);
                            float effectiveSizeX = (x2 - x1) * (maxU - minU);
                            float effectiveSizeY = (y2 - y1) * (maxV - minV);
                            float effectiveSize = effectiveSizeX + effectiveSizeY;
                            if (effectiveSize > biggest) {
                                biggestUVRenderCallIndex = i;
                                biggest = effectiveSize;
                            }
                        }

                        //biggestUVRenderCallIndex = -1;

                        //System.out.println("rendering biggest: " + biggest);
                        if (biggestUVRenderCallIndex != -1) {
                            //for (RenderCall renderCall : screenData.getListRenderCalls()) {
                            RenderCall renderCall = screenData.getListRenderCalls().get(biggestUVRenderCallIndex);
                            //prevent modifying original list, theres edge cases on resize where itll run this code twice before a new render call refreshes the list
                            List<Object> listParams = renderCall.getListParamsCopy();
                            listParams.add(pGuiGraphics.pose());
                            listParams.add(screenRule);
                            convertParamsToStaticlySized(listParams);
                            try {
                                //System.out.println("?" + renderCall.getRenderCallType());
                                //System.out.println("params before render: " + listParams);
                                //lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(pGuiGraphics, listParams.toArray());
                                lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(ScreenParticleRenderer.getInstance(), listParams.toArray());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //}
                        }

                    }/* else if (renderType.equals(ScreenRule.RenderTypes.SINGLE_TEXTURE)) {
                    RenderCall renderCall = new RenderCall(RenderCallType.INNER_BLIT);
                    renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), 339, 515, 140, 306, 0, 0.0F, 0.6875F, 0.0F, 0.6484375F);
                    try {
                        lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(pGuiGraphics, renderCall.getListParams().toArray());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }*/ else if (renderType.equals(ScreenRule.RenderTypes.GENERIC_INVENTORY) || renderType.equals(ScreenRule.RenderTypes.SINGLE_TEXTURE)) {
                        RenderCall renderCall = new RenderCall(RenderCallType.INNER_BLIT_BLUR);
                        //renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), 872, 1048, 405, 571, 0, 0.0F, 0.6875F, 0.0F, 0.8671875F);
                        renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), 872, 1048, 968, 1190, 0, 0.0F, 0.6875F, 0.0F, 0.8671875F);
                        //[minecraft:textures/gui/container/generic_54.png, 872, 1048, 475, 571, 0, 0.0, 0.6875, 0.4921875, 0.8671875]
                        //int resX = Minecraft.getInstance().
                    /*int sizeX = (int) renderCall.getListParams().get(2) - (int) renderCall.getListParams().get(1);
                    int sizeY = (int) renderCall.getListParams().get(4) - (int) renderCall.getListParams().get(3);
                    int x1 = 0;
                    int x2 = (int)(1920 / 1.26F);
                    int y2 = 1080;
                    int y1 = 0;
                    int intScale = Math.min(ScreenParticleRenderer.getInstance().width / sizeX, ScreenParticleRenderer.getInstance().height / sizeY);
                    y1 = (ScreenParticleRenderer.getInstance().height / 2) - (sizeY / 2 * intScale);
                    y2 = (ScreenParticleRenderer.getInstance().height / 2) + (sizeY / 2 * intScale);
                    x1 = (ScreenParticleRenderer.getInstance().width / 2) - (sizeX / 2 * intScale);
                    x2 = (ScreenParticleRenderer.getInstance().width / 2) + (sizeX / 2 * intScale);
                    renderCall.getListParams().set(1, x1);
                    renderCall.getListParams().set(2, x2);
                    renderCall.getListParams().set(3, y1);
                    renderCall.getListParams().set(4, y2);
                    System.out.println("sizeX " + sizeX + " sizeY " + sizeY + " intScale " + intScale);
                    System.out.println("params before render: " + renderCall.getListParams());*/

                        List<Object> listParams = renderCall.getListParamsCopy();
                        listParams.add(pGuiGraphics.pose());
                        listParams.add(screenRule);
                        convertParamsToStaticlySized(listParams);

                        //renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), 339, 515, 140, 306, 0, 0.0F, 0.6875F, 0.0F, 0.8671875F);
                        //renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), 872, 1048, 405, 571, 0, 0.0F, 0.6875F, 0.0F, 0.6484375F);

                        //renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), x1, x2, y1, y2, 0, 0.0F, 0.6875F, 0.0F, 0.8671875F);
                        try {
                            lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(ScreenParticleRenderer.getInstance(), listParams.toArray());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                ScreenParticleRenderer.getInstance().unbind();


                Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)ScreenParticleRenderer.getInstance().widthScaledDown, (float)ScreenParticleRenderer.getInstance().heightScaledDown, 0.0F, 1000.0F, net.minecraftforge.client.ForgeHooksClient.getGuiFarPlane());
                RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

                ScreenParticleRenderer.getInstance().bindScaledDown();
                //ScreenParticleRenderer.getInstance().bindScaledDownFromByteBuffer();
                RenderSystem.clear(16640, Minecraft.ON_OSX);
                double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
                int croppedWidth = (int) (512 * guiScale);
                int croppedHeight = (int) (512 * guiScale);
                /*float aspectRatio = (float)ScreenParticleRenderer.getInstance().width / (float)ScreenParticleRenderer.getInstance().height;
                croppedWidth = 512;
                croppedHeight = (int) (croppedWidth / aspectRatio);*/
                //croppedWidth = ScreenParticleRenderer.getInstance().width;
                //croppedHeight = ScreenParticleRenderer.getInstance().height;

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
                /*minU = 0;
                maxU = 1;
                minV = 0;
                maxV = 1;*/
                x1 = 0;
                x2 = ScreenParticleRenderer.getInstance().widthScaledDown * 4;
                y1 = 0;
                y2 = ScreenParticleRenderer.getInstance().heightScaledDown * 4;

                x1 = 0;
                x2 = ScreenParticleRenderer.getInstance().width;
                y1 = 0;
                y2 = ScreenParticleRenderer.getInstance().height;

                x1 = 0;
                x2 = ScreenParticleRenderer.getInstance().widthScaledDown;
                y1 = 0;
                y2 = ScreenParticleRenderer.getInstance().heightScaledDown;

                ScreenParticleRenderer t = ScreenParticleRenderer.getInstance();
                /*ScreenParticleRenderer.getInstance().innerBlitCustom(pGuiGraphics.pose()
                        , 0, ScreenParticleRenderer.getInstance().widthScaledDown
                        , 0, ScreenParticleRenderer.getInstance().heightScaledDown
                        , 0, 0, 1, 0, 1);*/

                ScreenParticleRenderer.getInstance().innerBlitCustom(pGuiGraphics.pose()
                        , x1, x2
                        , y1, y2
                        , 0
                        , minU, maxU, minV, maxV);

                //getting data from scaled down framebuffer
                ByteBuffer pixelBuffer = readPixelsTest();

                ScreenParticleRenderer.getInstance().unbindScaledDown();

                ScreenParticleRenderer.getInstance().bindScaledDownFromByteBuffer();

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, ScreenParticleRenderer.getInstance().getMainRenderTargetScaledDownFromByteBuffer().getColorTextureId());

                GL11.glTexImage2D(
                        GL11.GL_TEXTURE_2D,
                        0, // Mipmap level
                        GL11.GL_RGBA, // Internal format
                        ScreenParticleRenderer.getInstance().widthScaledDown,
                        ScreenParticleRenderer.getInstance().heightScaledDown,
                        0, // Border
                        GL11.GL_RGBA, // Data format
                        GL11.GL_UNSIGNED_BYTE, // Data type
                        pixelBuffer
                );

                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

                GL30.glFramebufferTexture2D(
                        GL30.GL_FRAMEBUFFER,
                        GL30.GL_COLOR_ATTACHMENT0,
                        GL11.GL_TEXTURE_2D,
                        ScreenParticleRenderer.getInstance().getMainRenderTargetScaledDownFromByteBuffer().getColorTextureId(),
                        0 // Mipmap level
                );

                // Check if the framebuffer is complete
                if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    throw new RuntimeException("Framebuffer is not complete");
                }

                ScreenParticleRenderer.getInstance().unbindScaledDownFromByteBuffer();

                Window window = Minecraft.getInstance().getWindow();
                matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)((double)window.getWidth() / window.getGuiScale()), (float)((double)window.getHeight() / window.getGuiScale()), 0.0F, 1000.0F, net.minecraftforge.client.ForgeHooksClient.getGuiFarPlane());
                RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);

                Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
                //System.out.println("completed new render for " + playerStatus);
            }
        }
    }

    public static void convertParamsToStaticlySized(List<Object> params) {
        ScreenParticleRenderer screenParticleRenderer = ScreenParticleRenderer.getInstance();
        int sizeX = (int) params.get(2) - (int) params.get(1);
        int sizeY = (int) params.get(4) - (int) params.get(3);
        int x1 = 0;
        int x2 = (int)(1920 / 1.26F);
        int y2 = 1080;
        int y1 = 0;
        int intScale = Math.min(screenParticleRenderer.width / sizeX, screenParticleRenderer.height / sizeY);
        //TODO: guiScale setting lies, smaller window sizes dynamically reduce gui scale
        //int guiScale = Minecraft.getInstance().options.guiScale().get();
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        //intScale = 1;
        x1 = (screenParticleRenderer.width / 2) - (sizeX / 2 * intScale);
        x2 = (screenParticleRenderer.width / 2) + (sizeX / 2 * intScale);
        y1 = (screenParticleRenderer.height / 2) - (sizeY / 2 * intScale);
        y2 = (screenParticleRenderer.height / 2) + (sizeY / 2 * intScale);
        x1 /= guiScale;
        x2 /= guiScale;
        y1 /= guiScale;
        y2 /= guiScale;
        //System.out.println("sizeX " + sizeX + " sizeY " + sizeY + " intScale " + intScale + " screen width " + screenParticleRenderer.width + " screen height " + screenParticleRenderer.height);
        //System.out.println("original params: " + params);
        params.set(1, x1);
        params.set(2, x2);
        params.set(3, y1);
        params.set(4, y2);
        //System.out.println("adjusted params: " + params);
    }

    public static ByteBuffer readPixelsTest() {
        int width = ScreenParticleRenderer.getInstance().widthScaledDown;
        int height = ScreenParticleRenderer.getInstance().heightScaledDown;

        ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(width * height * 4); // RGBA = 4 bytes per pixel
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);

        // Process the pixel data (example: print the color of the first pixel)
        int red = Byte.toUnsignedInt(pixelBuffer.get(0));
        int green = Byte.toUnsignedInt(pixelBuffer.get(1));
        int blue = Byte.toUnsignedInt(pixelBuffer.get(2));
        int alpha = Byte.toUnsignedInt(pixelBuffer.get(3));

        /**
         * The pixel data is read from the lower-left corner of the framebuffer by default.
         * Ensure the buffer size matches the width, height, and bytes per pixel.
         * Performance: glReadPixels can be slow, so avoid using it in performance-critical loops.
         */

        /**
         * reading specific pixel:
         *
         * int index = (y * width + x) * componentsPerPixel;
         */

        int x = width / 2; // X coordinate of the pixel
        int y = height / 2; // Y coordinate of the pixel

        // Calculate the index for pixel (x, y)
        int componentsPerPixel = 4; // RGBA
        int index = (y * width + x) * componentsPerPixel;

        red = Byte.toUnsignedInt(pixelBuffer.get(index));
        green = Byte.toUnsignedInt(pixelBuffer.get(index + 1));
        blue = Byte.toUnsignedInt(pixelBuffer.get(index + 2));
        alpha = Byte.toUnsignedInt(pixelBuffer.get(index + 3));

        //System.out.printf("Pixel color at (0,0): R=%d, G=%d, B=%d, A=%d%n", red, green, blue, alpha);

        //confirmed works
        return pixelBuffer;
    }

}
