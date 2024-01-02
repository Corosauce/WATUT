package com.corosus.watut.mixin.client;

import com.corosus.watut.ParticleRegistry;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Set;
import java.util.stream.Stream;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasPrepareToStitch {

    @Inject(method = "prepareToStitch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void prepareToStitch(ResourceManager p_118308_, Stream<ResourceLocation> p_118309_, ProfilerFiller p_118310_, int p_118311_, CallbackInfoReturnable<TextureAtlas.Preparations> cir, Set<ResourceLocation> set) {
        //System.out.println("HOOOOOOOOOOOK");
        //System.out.println(set.size());
        ParticleRegistry.textureAtlasPrepareToSitch((TextureAtlas)(Object)this, set);
    }
}