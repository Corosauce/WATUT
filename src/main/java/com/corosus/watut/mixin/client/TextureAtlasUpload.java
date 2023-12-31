package com.corosus.watut.mixin.client;

import com.corosus.watut.ParticleRegistry;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasUpload {

    @Inject(method = "upload", at = @At("TAIL"))
    private void render(SpriteLoader.Preparations pPreparations, CallbackInfo info) {
        ParticleRegistry.textureAtlasUpload((TextureAtlas)(Object)this);
    }
}