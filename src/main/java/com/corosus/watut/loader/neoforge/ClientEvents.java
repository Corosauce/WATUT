package com.corosus.watut.loader.neoforge;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.ShaderInstanceBlur;
import com.corosus.watut.WatutMod;
import com.corosus.watut.command.CommandWatutReloadJSON;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

import java.io.IOException;

public class ClientEvents {

    public void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstanceBlur(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(WatutMod.MODID, "particle"),
                            DefaultVertexFormat.PARTICLE),
                    s -> PlayerStatusManagerClient.particle = (ShaderInstanceBlur) s);
            event.registerShader(
                    new ShaderInstanceBlur(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(WatutMod.MODID, "position_tex_blur"),
                            DefaultVertexFormat.POSITION_TEX),
                    s -> PlayerStatusManagerClient.positionTexBlur = (ShaderInstanceBlur) s);
            event.registerShader(
                    new ShaderInstanceBlur(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(WatutMod.MODID, "position_tex_blur_horizontal"),
                            DefaultVertexFormat.POSITION_TEX),
                    s -> PlayerStatusManagerClient.positionTexBlurHorizontal = (ShaderInstanceBlur) s);
            event.registerShader(
                    new ShaderInstanceBlur(event.getResourceProvider(),
                            ResourceLocation.fromNamespaceAndPath(WatutMod.MODID, "position_tex_blur_vertical"),
                            DefaultVertexFormat.POSITION_TEX),
                    s -> PlayerStatusManagerClient.positionTexBlurVertical = (ShaderInstanceBlur) s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void getRegisteredParticles(TextureAtlasStitchedEvent event) {
        ParticleRegistry.textureAtlasUpload(event.getAtlas());
    }

    public void onGameTick(ClientTickEvent.Post event) {
        WatutMod.getPlayerStatusManagerClient().tickGame();
    }

    public void onRegisterCommandsClient(RegisterClientCommandsEvent event) {
        CommandWatutReloadJSON.register(event.getDispatcher());
    }

    public void onMouse(InputEvent.MouseButton.Post event) {
        WatutMod.getPlayerStatusManagerClient().onMouse(event.getAction() != 0);
    }

    public void onKey(InputEvent.Key event) {
        WatutMod.getPlayerStatusManagerClient().onKey();
    }

}
