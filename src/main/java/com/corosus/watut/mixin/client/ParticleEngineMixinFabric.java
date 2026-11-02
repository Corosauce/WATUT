package com.corosus.watut.mixin.client;

import com.corosus.watut.PlayerStatusManagerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.particle.ParticleEngine.class)
public abstract class ParticleEngineMixinFabric {

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        PlayerStatusManagerClient.getParticleEngine().tick();
    }
}