package com.corosus.watut.loader.forge;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.WatutMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WatutMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EventHandlerForge {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void guiRender(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            WatutMod.getPlayerStatusManagerClient().onGuiRender();
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onMouse(InputEvent.RawMouseEvent event) {
        WatutMod.getPlayerStatusManagerClient().onMouse(event.getAction() != 0);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onKey(InputEvent.KeyInputEvent event) {
        WatutMod.getPlayerStatusManagerClient().onKey();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onGameTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            WatutMod.getPlayerStatusManagerClient().tickGame();
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (event.player.level.isClientSide()) {
                WatutMod.getPlayerStatusManagerClient().tickPlayer(event.player);
            } else {
                WatutMod.getPlayerStatusManagerServer().tickPlayer(event.player);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        WatutMod.getPlayerStatusManagerServer().playerLoggedIn(event.getPlayer());
    }

    public static void getRegisteredParticles(TextureStitchEvent.Post event) {
        ParticleRegistry.textureAtlasUpload(event.getMap());
    }
}
