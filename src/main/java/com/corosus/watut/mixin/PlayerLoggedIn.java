package com.corosus.watut.mixin;

import com.corosus.watut.WatutMod;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerLoggedIn {

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void placeNewPlayer(Connection pNetManager, ServerPlayer pPlayer, CallbackInfo info) {
        WatutMod.getPlayerStatusManagerServer().playerLoggedIn(pPlayer);
    }
}