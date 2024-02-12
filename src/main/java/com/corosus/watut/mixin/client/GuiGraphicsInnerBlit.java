package com.corosus.watut.mixin.client;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.client.ScreenCapturing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsInnerBlit {

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("TAIL"))
    private void innerBlit(ResourceLocation p_283461_, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_, CallbackInfo ci) {
        ScreenCapturing.innerBlit(p_283461_, p_281399_, p_283222_, p_283615_, p_283430_, p_281729_, p_283247_, p_282598_, p_282883_, p_283017_);
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("TAIL"))
    private void innerBlit(ResourceLocation p_283461_, int p_281399_, int p_283222_, int p_283615_, int p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_, float p_282800_, float p_282850_, float p_282375_, float p_282754_, CallbackInfo ci) {
        ScreenCapturing.innerBlit(p_283461_, p_281399_, p_283222_, p_283615_, p_283430_, p_281729_, p_283247_, p_282598_, p_282883_, p_283017_);
    }
}