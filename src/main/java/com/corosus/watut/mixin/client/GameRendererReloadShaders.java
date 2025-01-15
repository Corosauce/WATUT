package com.corosus.watut.mixin.client;

import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.WatutMod;
import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GameRenderer.class)
public abstract class GameRendererReloadShaders {

    @Inject(method = "reloadShaders", at = @At("TAIL"))
    private void reloadShaders(ResourceProvider p_250719_, CallbackInfo ci) {
        //try {
            /*System.out.println("register shaders");
            PlayerStatusManagerClient.positionTexBlur = null;
            PlayerStatusManagerClient.positionColorTexBlur = null;

            PlayerStatusManagerClient.positionTexBlur = new ShaderInstance(p_250719_, new ResourceLocation("watut:position_tex_blur"),
                    DefaultVertexFormat.POSITION_TEX);
            event.registerShader(WatutMod.cloudShader, (shaderInstance -> {}));

            ShaderPro*/
        /*} catch (IOException e) {
            e.printStackTrace();
        }*/
    }
}