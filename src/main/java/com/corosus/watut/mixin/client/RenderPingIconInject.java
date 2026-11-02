package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PlayerTabOverlay.class, priority = 999)
public abstract class RenderPingIconInject {

    /*@Inject(method = "renderPingIcon", at = @At(value = "TAIL"))
    public void renderPingIcon(GuiGraphicsExtractor pGuiGraphics, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo, CallbackInfo ci) {
        Watut.getPlayerStatusManagerClient().renderPingIconHook((PlayerTabOverlay)(Object)this, pGuiGraphics, p_281809_, p_282801_, pY, pPlayerInfo);
    }*/

    @WrapOperation(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;extractPingIcon(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V"))
    public void renderPingIcon(PlayerTabOverlay playerTabOverlay, GuiGraphicsExtractor pGuiGraphics, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo, Operation<Void> original) {
        if (!WatutMod.getPlayerStatusManagerClient().renderPingIconHook(playerTabOverlay, pGuiGraphics, p_281809_, p_282801_, pY, pPlayerInfo)) {
            original.call(playerTabOverlay, pGuiGraphics, p_281809_, p_282801_, pY, pPlayerInfo);
        }
    }

}
