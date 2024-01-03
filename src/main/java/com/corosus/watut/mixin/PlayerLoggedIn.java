package com.corosus.watut.mixin;

import com.corosus.watut.WatutMod;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerLoggedIn {

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void placeNewPlayer(NetworkManager pNetManager, ServerPlayerEntity pPlayer, CallbackInfo info) {
        WatutMod.getPlayerStatusManagerServer().playerLoggedIn(pPlayer);
    }
}