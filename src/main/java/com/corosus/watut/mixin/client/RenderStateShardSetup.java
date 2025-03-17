package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderStateShard.class)
public abstract class RenderStateShardSetup {

    /**
     * Unused and also a terrible idea, for testing only
     * @param ci
     */
    @Inject(method = "setupRenderState",
            at = @At(value = "HEAD"), cancellable = true)
    public void setupRenderState(CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI) {
            //((RenderStateShard)(Object)this).setupRenderState();
            //instance.run();
            ci.cancel();
        }
    }
}