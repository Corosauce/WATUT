package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.corosus.watut.client.screen.RenderCall;
import com.corosus.watut.client.screen.RenderCallType;
import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PoseStack.class)
public abstract class PoseStacks {

    @Inject(method = "pushPose", at = @At("TAIL"))
    private void pushPose(CallbackInfo ci) {
        if (ScreenParticleRenderer.isCapturing()) {
            WatutMod.getPlayerStatusManagerClient().getStatusLocal().getScreenData().addRenderCall(new RenderCall(RenderCallType.POSE_PUSH));
        }
    }

    @Inject(method = "popPose", at = @At("TAIL"))
    private void popPose(CallbackInfo ci) {
        if (ScreenParticleRenderer.isCapturing()) {
            WatutMod.getPlayerStatusManagerClient().getStatusLocal().getScreenData().addRenderCall(new RenderCall(RenderCallType.POSE_POP));
        }
    }

    @Inject(method = "translate(FFF)V", at = @At("TAIL"))
    private void translate(float x, float y, float z, CallbackInfo ci) {
        if (ScreenParticleRenderer.isCapturing()) {
            WatutMod.getPlayerStatusManagerClient().getStatusLocal().getScreenData().addRenderCall(new RenderCall(RenderCallType.POSE_TRANSLATE_F, x, y, z));
        }
    }

    @Inject(method = "translate(DDD)V", at = @At("TAIL"))
    private void translate(double x, double y, double z, CallbackInfo ci) {
        if (ScreenParticleRenderer.isCapturing()) {
            WatutMod.getPlayerStatusManagerClient().getStatusLocal().getScreenData().addRenderCall(new RenderCall(RenderCallType.POSE_TRANSLATE_D, x, y, z));
        }
    }
}