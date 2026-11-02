package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenRenderBackground {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractBackground(CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI2) {
            ci.cancel();
        }
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractBlurredBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI2) {
            ci.cancel();
        }
    }

    @Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractMenuBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height, CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI2) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true, require = 1)
    private void onExtractTransparentBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI2) {
            ci.cancel();
        }
    }
}
