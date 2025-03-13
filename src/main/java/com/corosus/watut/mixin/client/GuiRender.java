package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.corosus.watut.client.screen.RenderHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiRender {

    @Inject(method = "render", at = @At("TAIL"))
    private void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        WatutMod.getPlayerStatusManagerClient().onGuiRender();
        RenderHelper.guiRender(guiGraphics);
    }
}