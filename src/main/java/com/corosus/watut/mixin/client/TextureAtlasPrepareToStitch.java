package com.corosus.watut.mixin.client;

import com.corosus.watut.ParticleRegistry;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Set;
import java.util.stream.Stream;

@Mixin(AtlasTexture.class)
public abstract class TextureAtlasPrepareToStitch {

    @Inject(method = "prepareToStitch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;popPush(Ljava/lang/String;)V", ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void prepareToStitch(IResourceManager p_229220_1_, Stream<ResourceLocation> p_229220_2_, IProfiler p_229220_3_, int p_229220_4_, CallbackInfoReturnable<AtlasTexture.SheetData> cir, Set<ResourceLocation> set) {
        //System.out.println("HOOOOOOOOOOOK");
        //System.out.println(set.size());
        ParticleRegistry.textureAtlasPrepareToSitch((AtlasTexture)(Object)this, set);
    }
}