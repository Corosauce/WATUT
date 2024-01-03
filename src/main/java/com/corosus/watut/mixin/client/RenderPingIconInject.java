package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.realmsclient.dto.PlayerInfo;
import net.minecraft.client.gui.overlay.PlayerTabOverlayGui;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerTabOverlayGui.class)
public abstract class RenderPingIconInject {

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/overlay/PlayerTabOverlayGui;renderPingIcon(Lcom/mojang/blaze3d/matrix/MatrixStack;IIILnet/minecraft/client/network/play/NetworkPlayerInfo;)V"))
    public void renderPingIcon(PlayerTabOverlayGui playerTabOverlay, MatrixStack poseStack, int p_281809_, int p_282801_, int pY, NetworkPlayerInfo pPlayerInfo) {
        if (!WatutMod.getPlayerStatusManagerClient().renderPingIconHook((PlayerTabOverlayGui)(Object)this, poseStack, p_281809_, p_282801_, pY, pPlayerInfo)) {
            playerTabOverlay.renderPingIcon(poseStack, p_281809_, p_282801_, pY, pPlayerInfo);
        }
    }

}