package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenRenderBackground {
    @Inject(
            method = {
                    "renderBackground"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIII)V"),
            cancellable = true
    )
    private void renderBackground(CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI()) {
            ci.cancel();
        }
    }

    @Inject(
        method = {
            "renderBackground*"
        },
        at = @At(value = "HEAD"),
        cancellable = true
    )

    private void onRenderBackground(CallbackInfo ci) {
        if (ScreenParticleRenderer.isRenderingParticleGUI2) {
            ci.cancel();
        }
    }
}
