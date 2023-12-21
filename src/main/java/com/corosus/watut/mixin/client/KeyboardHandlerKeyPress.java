package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerKeyPress {

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void keyPress(long pWindowPointer, int pKey, int pScanCode, int pAction, int pModifiers, CallbackInfo info) {
        WatutMod.getPlayerStatusManagerClient().onKey();
    }
}