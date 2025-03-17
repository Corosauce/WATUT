package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererResize {

    @Inject(method = "resize", at = @At("TAIL"))
    private void resize(int p_110026_, int p_110027_, CallbackInfo ci) {
        ScreenParticleRenderer.getInstance().resize(p_110026_, p_110027_);
    }
}