package com.corosus.watut.mixin.client;

import com.corosus.watut.client.WatutClientMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin Client-Only per il tick dei giocatori in WATUT.
 * Eseguito esclusivamente nell'ambiente client, azzerando qualsiasi rischio di NoClassDefFoundError su server dedicati.
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayer.class)
public abstract class ClientPlayerTick {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientPlayerTick(CallbackInfo ci) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        WatutClientMod.getPlayerStatusManagerClient().tickPlayer(player);
    }
}
