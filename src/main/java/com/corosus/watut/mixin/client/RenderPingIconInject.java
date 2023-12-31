package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerTabOverlay.class)
public abstract class RenderPingIconInject {

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;renderPingIcon(Lcom/mojang/blaze3d/vertex/PoseStack;IIILnet/minecraft/client/multiplayer/PlayerInfo;)V"))
    public void renderPingIcon(PlayerTabOverlay playerTabOverlay, PoseStack poseStack, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo) {
        if (!WatutMod.getPlayerStatusManagerClient().renderPingIconHook((PlayerTabOverlay)(Object)this, poseStack, p_281809_, p_282801_, pY, pPlayerInfo)) {
            playerTabOverlay.renderPingIcon(poseStack, p_281809_, p_282801_, pY, pPlayerInfo);
        }
    }

}