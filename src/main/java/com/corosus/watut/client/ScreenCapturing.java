package com.corosus.watut.client;


import com.mojang.blaze3d.pipeline.MainTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

public class ScreenCapturing {

    //public static int framebuffer;
    public static int SIZE_FLOAT = 4;
    public static int SIZE_RGB = 3;

    public static int width = 1920;
    public static int height = 1080;

    public static boolean needsInit = true;
    //public static FrameBufferObject frameBufferObject;
    public static MainTarget mainRenderTarget;
    //public static CustomRenderTarget mainRenderTarget;

    public static void setup() {
        //framebuffer = GL30.glGenFramebuffers();
        //frameBufferObject = new FrameBufferObject(width, height);
        Minecraft mc = Minecraft.getInstance();
        width = mc.getWindow().getWidth();
        height = mc.getWindow().getHeight();
        System.out.println("init with resolution: " + width + "x" + height);
        mainRenderTarget = new MainTarget(width, height);
        //mainRenderTarget = new CustomRenderTarget(width, height, true);
        mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mainRenderTarget.clear(Minecraft.ON_OSX);
    }

    public static void gameTick() {
        bind();
        //renderScreen();
        getPixels();
        unbind();
    }

    public static void innerBlit(ResourceLocation pAtlasLocation, int pX1, int pX2, int pY1, int pY2, int pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV) {
        System.out.println("pAtlasLocation: " + pAtlasLocation);
        System.out.println("pX1: " + pX1 + ", pX2: " + pX2 + ", pY1: " + pY1 + ", pY2: " + pY2 + ", pBlitOffset: " + pBlitOffset + ", pMinU: " + pMinU + ", pMaxU: " + pMaxU + ", pMinV: " + pMinV + ", pMaxV: " + pMaxV);
    }

    public static void checkSetup() {
        if (needsInit) {
            needsInit = false;
            setup();
        }
    }

    public static void postScreenRenderHook(ScreenEvent.Render.Post event) {
        System.out.println("RENDER STACK END");
        boolean doit = false;
        if (doit) {
            if (needsInit) {
                needsInit = false;
                setup();
            }
            bind();
            event.getScreen().renderWithTooltip(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            getPixels();
            unbind();
        }
    }

    public static void bind() {
        //GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mainRenderTarget.frameBufferId);
        mainRenderTarget.bindWrite(true);
        //frameBufferObject.Begin(width, height);
    }

    /*public static void renderScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            int i = (int)(mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getScreenWidth());
            int j = (int)(mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getScreenHeight());
            GuiGraphics guigraphics = new GuiGraphics(mc, mc.renderBuffers.bufferSource());
            net.minecraftforge.client.ForgeHooksClient.drawScreen(mc.screen, guigraphics, i, j, mc.getDeltaFrameTime());
        }
    }*/

    public static void unbind() {
        //GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        mainRenderTarget.unbindWrite();
        //frameBufferObject.End();
    }

    public static void renderFrameBuffer() {
        /*GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainRenderTarget.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glBlitFramebuffer(0, 0, mainRenderTarget.width, mainRenderTarget.height, 0, 0, mainRenderTarget.width, mainRenderTarget.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);*/
        //mainRenderTarget.blitToScreen(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight());
    }

    public static void getPixels() {
        int capacity = width*height*SIZE_RGB;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity);
        //GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, byteBuffer);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, byteBuffer);

        int colorIndex = 0;
        String str = "";
        String strAll = "";

        int cutoffTest = 100;

        for (int i = 0; i < capacity; i++) {
            if (i < cutoffTest) {
                byte val = byteBuffer.get();
                str += val + ",";
                if ((i + 1) % 3 == 0) {
                    str = str.substring(0, str.length() - 1);
                    strAll += str + "\n";
                    //System.out.println(str);
                    str = "";
                }
                if (val > 0) {
                    //System.out.println("!!");
                }
                //System.out.println(val);
            }
        }

        System.out.println(strAll);

        int what = 0;

    }

    public static float getZDepth(int x, int y) {
        int capacity = SIZE_FLOAT;
        ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);
        GL11.glReadPixels(x, y, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, byteBuffer);
        return byteBuffer.getFloat(0);
    }

}
