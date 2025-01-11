package com.corosus.watut.loader.forge;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.ShaderInstanceBlur;
import com.corosus.watut.WatutMod;
import com.corosus.watut.client.screen.RenderHelper;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = WatutMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EventHandlerForge {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void guiRender(RenderGuiEvent.Post event) {
        WatutMod.getPlayerStatusManagerClient().onGuiRender();
        RenderHelper.guiRender();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onMouse(InputEvent.MouseButton.Post event) {
        WatutMod.getPlayerStatusManagerClient().onMouse(event.getAction() != 0);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onKey(InputEvent.Key event) {
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
            if (event.player.level().isClientSide()) {
                WatutMod.getPlayerStatusManagerClient().tickPlayer(event.player);
            } else {
                WatutMod.getPlayerStatusManagerServer().tickPlayer(event.player);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        WatutMod.getPlayerStatusManagerServer().playerLoggedIn(event.getEntity());
    }


    public static void getRegisteredParticles(TextureStitchEvent.Post event) {
        ParticleRegistry.textureAtlasUpload(event.getAtlas());
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            //System.out.println("watut register shaders");
            PlayerStatusManagerClient.particle = null;
            PlayerStatusManagerClient.positionTexBlur = null;
            PlayerStatusManagerClient.positionColorTexBlur = null;

            PlayerStatusManagerClient.particle = new ShaderInstanceBlur(event.getResourceProvider(), new ResourceLocation("watut:particle"),
                    DefaultVertexFormat.PARTICLE);
            PlayerStatusManagerClient.positionTexBlur = new ShaderInstanceBlur(event.getResourceProvider(), new ResourceLocation("watut:position_tex_blur"),
                    DefaultVertexFormat.POSITION_TEX);
            PlayerStatusManagerClient.positionColorTexBlur = new ShaderInstanceBlur(event.getResourceProvider(), new ResourceLocation("watut:position_color_tex_blur"),
                    DefaultVertexFormat.POSITION_COLOR_TEX);

            event.registerShader(PlayerStatusManagerClient.particle, (shaderInstance -> {}));
            event.registerShader(PlayerStatusManagerClient.positionTexBlur, (shaderInstance -> {}));
            event.registerShader(PlayerStatusManagerClient.positionColorTexBlur, (shaderInstance -> {}));
        } catch (IOException e) {
            e.printStackTrace();
            //WatutMod.cloudShader = GameRenderer.getPositionTexColorNormalShader();
            //throw new RuntimeException(e);
        }

    }
}
