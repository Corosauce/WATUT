package com.corosus.watut;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Watut.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EventHandlerForge {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void worldRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            //ClientTickHandler.getClientWeather();
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void guiRender(RenderGuiEvent.Post event) {
        //System.out.println("hook");
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChatScreen && mc.getConnection() != null) {
            ChatScreen chat = (ChatScreen) mc.screen;
            GuiGraphics guigraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            int height = chat.height + 26;
            guigraphics.drawString(mc.font, Watut.getPlayerStatusManagerClient().getTypingPlayers(), 2, height - 50, 16777215);
            guigraphics.flush();
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        //SceneEnhancer.renderTick(event);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onPlayerRender(RenderPlayerEvent.Pre event) {
        event.getRenderer().getModel().rightArm.xRot += 45;
        event.getRenderer().getModel().rightArm.yRot += 45;
        event.getRenderer().getModel().rightArm.zRot += 45;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.player.level().isClientSide()) {
            Watut.getPlayerStatusManagerClient().tickPlayer(event.player);
		} else {
            Watut.getPlayerStatusManagerServer().tickPlayer(event.player);
        }
    }
}
