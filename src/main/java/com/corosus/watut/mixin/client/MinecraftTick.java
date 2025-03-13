package com.corosus.watut.mixin.client;

import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.WatutMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftTick {

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo info) {
        WatutMod.getPlayerStatusManagerClient().tickGame();
    }

    @Inject(method = "updateLevelInEngines", at = @At("TAIL"))
    private void updateLevelInEngines(ClientLevel p_91325_, CallbackInfo ci) {
        PlayerStatusManagerClient.getParticleEngine().setLevel(p_91325_);
    }
}