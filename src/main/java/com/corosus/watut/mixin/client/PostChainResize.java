package com.corosus.watut.mixin.client;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.client.screen.ScreenParticleRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostChain.class)
public abstract class PostChainResize {

    @Inject(method = "resize", at = @At("TAIL"))
    private void resize(int p_110026_, int p_110027_, CallbackInfo ci) {
        ScreenParticleRenderer.getInstance().resize(p_110026_, p_110027_);
    }
}