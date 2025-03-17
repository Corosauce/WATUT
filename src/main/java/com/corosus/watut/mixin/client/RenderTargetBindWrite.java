package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.RenderHelper;
import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public abstract class RenderTargetBindWrite {

    /*@Redirect(method = "bindWrite",
            at = @At(value = "HEAD"))

    public void renderMenuBackground(boolean setViewport) {
        if (!ScreenParticleRenderer.isRenderingParticleGUI2) {
            ((RenderTarget)(Object)this).bindWrite(setViewport);
        }
    }*/

    @Inject(method = "bindWrite",
            at = @At(value = "HEAD"), cancellable = true)
    public void bindWrite(CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI2) {
            //((RenderStateShard)(Object)this).setupRenderState();
            //instance.run();
            ci.cancel();
        }
    }
}