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

    @Inject(
            method = "extractBackground",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onExtractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI || ScreenParticleRenderer.isRenderingParticleGUI2) {
            ci.cancel();
        }
    }

    @Inject(
            method = "extractTransparentBackground",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onExtractTransparentBackground(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI || ScreenParticleRenderer.isRenderingParticleGUI2) {
            ci.cancel();
        }
    }
}
