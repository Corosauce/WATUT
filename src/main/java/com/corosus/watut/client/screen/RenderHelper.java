package com.corosus.watut.client.screen;

import com.corosus.watut.PlayerStatus;
import com.corosus.watut.WatutMod;
import com.corosus.watut.client.ScreenCapturing;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public class RenderHelper {

    public static HashMap<RenderCallType, Method> lookupRenderCallsToMethod = new HashMap<>();

    static {
        try {
            lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT, GuiGraphics.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class));
            lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT2, GuiGraphics.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void renderWithTooltipEnd(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        //PlayerStatus playerStatusLocal = WatutMod.getPlayerStatusManagerClient().getStatusLocal();

        for (PlayerStatus playerStatus : WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
            ScreenData screenData = playerStatus.getScreenData();
            if (screenData.needsNewRender()) {
                screenData.markNeedsNewRender(false);

                //ScreenCapturing.checkSetup();
                //ScreenCapturing.bind();
                screenData.checkSetup();
                screenData.bind();

                RenderSystem.clear(16640, Minecraft.ON_OSX);
                //screenData.getMainRenderTarget().clear(Minecraft.ON_OSX);
                /*screenData.getMainRenderTarget().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                screenData.getMainRenderTarget().clear(Minecraft.ON_OSX);*/
                //GL30.glClearBufferfi();

                System.out.println("rendering");
                int i = 0;
                for (RenderCall renderCall : screenData.getListRenderCalls()) {
                    List<Object> listParams = renderCall.getListParams();
                    System.out.println("params size before render: " + listParams);
                    try {
                        //if (i > 90) {
                        lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(pGuiGraphics, listParams.toArray());
                        //}

                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    }
                    /*if (renderCall.getRenderCallType() == RenderCallType.INNER_BLIT) {
                        pGuiGraphics.innerBlit((ResourceLocation)listParams.get(0), (int)listParams.get(1), (int)listParams.get(2), (int)listParams.get(3), (int)listParams.get(4), (int)listParams.get(5), (float)listParams.get(6), (float)listParams.get(7), (float)listParams.get(8), (float)listParams.get(9));
                    } else if (renderCall.getRenderCallType() == RenderCallType.INNER_BLIT2) {
                        pGuiGraphics.innerBlit((ResourceLocation)listParams.get(0), (int)listParams.get(1), (int)listParams.get(2), (int)listParams.get(3), (int)listParams.get(4), (int)listParams.get(5), (float)listParams.get(6), (float)listParams.get(7), (float)listParams.get(8), (float)listParams.get(9), (float)listParams.get(10), (float)listParams.get(11), (float)listParams.get(12), (float)listParams.get(13));
                    }*/
                    i++;
                }

                //ScreenCapturing.getPixels();

                //ScreenCapturing.unbind();
                screenData.unbind();
                System.out.println("completed new render for " + playerStatus);

                //ScreenCapturing.renderFrameBuffer();
            }
        }
    }

}
