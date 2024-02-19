package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.corosus.watut.client.ScreenCapturing;
import com.corosus.watut.client.screen.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenRenderWithTooltip {

    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void renderWithTooltipStart(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        WatutMod.getPlayerStatusManagerClient().hookStartScreenRender();
    }

    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void renderWithTooltipEnd(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        WatutMod.getPlayerStatusManagerClient().hookStopScreenRender();
        //WatutMod.getPlayerStatusManagerClient().renderWithTooltipEnd(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        RenderHelper.renderWithTooltipEnd(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }
}