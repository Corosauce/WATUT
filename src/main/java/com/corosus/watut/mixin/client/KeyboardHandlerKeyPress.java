package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerKeyPress {

    @Inject(method = "keyPress", at = @At("TAIL"))
    private void keyPress(long windowPointer, int action, KeyEvent event, CallbackInfo info) {
        WatutMod.getPlayerStatusManagerClient().onKey();
    }

    @Inject(method = "charTyped", at = @At("TAIL"))
    private void charTyped(long windowPointer, CharacterEvent event, CallbackInfo info) {
        WatutMod.getPlayerStatusManagerClient().onKey();
    }
}