package com.corosus.watut.mixin.client;

import com.corosus.watut.ParticleRegistry;
import net.minecraft.client.renderer.texture.AtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

@Mixin(AtlasTexture.class)
public abstract class TextureAtlasUpload {

    @Inject(method = "reload", at = @At("TAIL"))
    private void upload(AtlasTexture.SheetData pPreparations, CallbackInfo info) {
        ParticleRegistry.textureAtlasUpload((AtlasTexture)(Object)this);
    }
}