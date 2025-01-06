package com.corosus.watut.client.screen;

import com.corosus.watut.PlayerStatus;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.WatutMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.JSONLoader;
import com.corosus.watut.config.JsonObjects.ScreenRule;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

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

            lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT_BLUR, ScreenParticleRenderer.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, PoseStack.class));
            lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT_BLUR2, ScreenParticleRenderer.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, PoseStack.class));
        } catch (NoSuchMethodException e) {
            try {
                lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT, GuiGraphics.class.getDeclaredMethod("m_280444_", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class));
                lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT2, GuiGraphics.class.getDeclaredMethod("m_280479_", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class));

                lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT_BLUR, ScreenParticleRenderer.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, PoseStack.class));
                lookupRenderCallsToMethod.put(RenderCallType.INNER_BLIT_BLUR2, ScreenParticleRenderer.class.getDeclaredMethod("innerBlit", ResourceLocation.class, int.class, int.class, int.class, int.class, int.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, PoseStack.class));
            } catch (NoSuchMethodException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    public static void renderWithTooltipEnd(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        for (PlayerStatus playerStatus : WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
            ScreenData screenData = playerStatus.getScreenData();

            if (ScreenParticleRenderer.getInstance().needsNewRender() || true) {
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

                System.out.println("render type " + renderType);

                if (renderType.equals(ScreenRule.RenderTypes.ALL_TEXTURES)) {
                    for (RenderCall renderCall : screenData.getListRenderCalls()) {
                        List<Object> listParams = renderCall.getListParams();

                        //System.out.println("size x: " + ((float)listParams.get(7) * 256) + " - " + "size y: " + ((float)listParams.get(9) * 256));
                        try {

                            lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(pGuiGraphics, listParams.toArray());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (renderType.equals(ScreenRule.RenderTypes.BIGGEST_TEXTURE)) {

                    int biggestUVRenderCallIndex = -1;
                    float biggest = 0;
                    for (int i = 0; i < screenData.getListRenderCalls().size(); i++) {
                        RenderCall renderCall = screenData.getListRenderCalls().get(i);
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
                        List<Object> listParams = renderCall.getListParams();
                        //override the supposed sizes and positions that arent UV
                            /*listParams.set(1, 0);
                            listParams.set(2, 256);
                            listParams.set(3, 0);
                            listParams.set(4, 512);*/
                        //correct the data to render top left, this might be fragile, but both innerblit methods are consistent with these params
                        int sizeX = (int) listParams.get(2) - (int) listParams.get(1);
                        int sizeY = (int) listParams.get(4) - (int) listParams.get(3);
                            /*listParams.set(1, -(sizeX/2));
                            listParams.set(2, sizeX -(sizeX/2));
                            listParams.set(3, -(sizeY/2));
                            listParams.set(4, sizeY -(sizeY/2));*/
                        /*listParams.set(1, 0);
                        listParams.set(2, sizeX);
                        listParams.set(3, 0);
                        listParams.set(4, sizeY);*/

                        convertParamsToStaticlySized(listParams);

                        //System.out.println("size x: " + ((float)listParams.get(7) * 256) + " - " + "size y: " + ((float)listParams.get(9) * 256));
                        try {
                            lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(pGuiGraphics, listParams.toArray());
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
                    renderCall.getListParams().add(pGuiGraphics.pose());
                    //[minecraft:textures/gui/container/generic_54.png, 872, 1048, 475, 571, 0, 0.0, 0.6875, 0.4921875, 0.8671875]
                    //int resX = Minecraft.getInstance().
                    int sizeX = (int) renderCall.getListParams().get(2) - (int) renderCall.getListParams().get(1);
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
                    System.out.println("params before render: " + renderCall.getListParams());
                    //renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), 339, 515, 140, 306, 0, 0.0F, 0.6875F, 0.0F, 0.8671875F);
                    //renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), 872, 1048, 405, 571, 0, 0.0F, 0.6875F, 0.0F, 0.6484375F);

                    //renderCall.innerBlit(new ResourceLocation("minecraft", "textures/gui/container/generic_54.png"), x1, x2, y1, y2, 0, 0.0F, 0.6875F, 0.0F, 0.8671875F);
                    try {
                        lookupRenderCallsToMethod.get(renderCall.getRenderCallType()).invoke(ScreenParticleRenderer.getInstance(), renderCall.getListParams().toArray());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ScreenParticleRenderer.getInstance().unbind();
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
        y1 = (screenParticleRenderer.height / 2) - (sizeY / 2 * intScale);
        y2 = (screenParticleRenderer.height / 2) + (sizeY / 2 * intScale);
        x1 = (screenParticleRenderer.width / 2) - (sizeX / 2 * intScale);
        x2 = (screenParticleRenderer.width / 2) + (sizeX / 2 * intScale);
        params.set(1, x1);
        params.set(2, x2);
        params.set(3, y1);
        params.set(4, y2);
        System.out.println("sizeX " + sizeX + " sizeY " + sizeY + " intScale " + intScale);
        System.out.println("params size before render: " + params);
    }

}
