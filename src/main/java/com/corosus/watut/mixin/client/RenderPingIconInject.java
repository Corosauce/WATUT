package com.corosus.watut.mixin.client;

import com.corosus.watut.Watut;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class RenderPingIconInject {

    /*@Inject(method = "renderPingIcon", at = @At(value = "TAIL"))
    public void renderPingIcon(GuiGraphics pGuiGraphics, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo, CallbackInfo ci) {
        Watut.getPlayerStatusManagerClient().renderPingIconHook((PlayerTabOverlay)(Object)this, pGuiGraphics, p_281809_, p_282801_, pY, pPlayerInfo);
    }*/

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;renderPingIcon(Lnet/minecraft/client/gui/GuiGraphics;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V"))
    public void renderPingIcon(PlayerTabOverlay playerTabOverlay, GuiGraphics pGuiGraphics, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo) {
        if (!Watut.getPlayerStatusManagerClient().renderPingIconHook((PlayerTabOverlay)(Object)this, pGuiGraphics, p_281809_, p_282801_, pY, pPlayerInfo)) {
            playerTabOverlay.renderPingIcon(pGuiGraphics, p_281809_, p_282801_, pY, pPlayerInfo);
        }
    }

}