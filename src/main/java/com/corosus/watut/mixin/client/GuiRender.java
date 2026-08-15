package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiRender {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void extractRenderState(DeltaTracker deltaTracker, boolean b1, boolean b2, CallbackInfo ci) {
        WatutMod.getPlayerStatusManagerClient().onGuiRender();
    }
}