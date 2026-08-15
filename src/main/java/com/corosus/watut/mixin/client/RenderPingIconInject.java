package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerTabOverlay.class, priority = 999)
public abstract class RenderPingIconInject {

    @Inject(method = "extractPingIcon", at = @At(value = "HEAD"), cancellable = true)
    public void extractPingIcon(GuiGraphicsExtractor extractor, int width, int x, int y, PlayerInfo playerInfo, CallbackInfo ci) {
        if (WatutMod.getPlayerStatusManagerClient().extractPingIconHook(extractor, width, x, y, playerInfo)) {
            ci.cancel();
        }
    }
}