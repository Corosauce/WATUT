package com.corosus.watut.mixin;

import com.corosus.watut.WatutMod;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerTick {

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo info) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        if (player.level.isClientSide()) {
            WatutMod.getPlayerStatusManagerClient().tickPlayer(player);
        } else {
            WatutMod.getPlayerStatusManagerServer().tickPlayer(player);
        }
    }
}