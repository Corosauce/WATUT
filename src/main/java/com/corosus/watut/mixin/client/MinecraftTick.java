package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTick {

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo info) {
        com.corosus.watut.client.WatutClientMod.getPlayerStatusManagerClient().tickGame();
    }
}