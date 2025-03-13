package com.corosus.watut.mixin.client;

import com.corosus.watut.PlayerStatusManagerClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.particle.ParticleEngine.class)
public abstract class ParticleEngineMixinFabric {

    @Inject(method = "render", at = @At("TAIL"))
    private void render(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        PlayerStatusManagerClient.getParticleEngine().render(lightTexture, camera, partialTick);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        PlayerStatusManagerClient.getParticleEngine().tick();
    }
}