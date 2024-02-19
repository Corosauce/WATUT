package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsInnerBlit {

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("TAIL"))
    private void innerBlit(ResourceLocation pAtlasLocation, int pX1, int pX2, int pY1, int pY2, int pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV, CallbackInfo ci) {
        //ScreenCapturing.innerBlit(pAtlasLocation, pX1, pX2, pY1, pY2, pBlitOffset, pMinU, pMaxU, pMinV, pMaxV);
        WatutMod.getPlayerStatusManagerClient().hookInnerBlit(pAtlasLocation, pX1, pX2, pY1, pY2, pBlitOffset, pMinU, pMaxU, pMinV, pMaxV);
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("TAIL"))
    private void innerBlit(ResourceLocation pAtlasLocation, int pX1, int pX2, int pY1, int pY2, int pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV, float pRed, float pGreen, float pBlue, float pAlpha, CallbackInfo ci) {
        //ScreenCapturing.innerBlit(pAtlasLocation, pX1, pX2, pY1, pY2, pBlitOffset, pMinU, pMaxU, pMinV, pMaxV, pRed, pGreen, pBlue, pAlpha);
        WatutMod.getPlayerStatusManagerClient().hookInnerBlit(pAtlasLocation, pX1, pX2, pY1, pY2, pBlitOffset, pMinU, pMaxU, pMinV, pMaxV, pRed, pGreen, pBlue, pAlpha);
    }
}