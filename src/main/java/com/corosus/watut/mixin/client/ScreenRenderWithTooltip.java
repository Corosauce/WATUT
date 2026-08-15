package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.RenderHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenRenderWithTooltip {

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
    private void renderWithTooltipEnd(GuiGraphicsExtractor extractor, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        if (!RenderHelper.performingOwnRender) {
            RenderHelper.renderWithTooltipEnd(extractor, pMouseX, pMouseY, pPartialTick);
        }
    }
}