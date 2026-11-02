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

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"), require = 1)
    private void renderWithTooltipEnd(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        //avoid recursion
        if (!RenderHelper.performingOwnRender) {
            RenderHelper.renderWithTooltipEnd(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }
    }
}
