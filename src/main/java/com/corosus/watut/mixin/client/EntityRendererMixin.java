package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.corosus.watut.loader.neoforge.ClientEvents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    /*@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At(value = "TAIL"))
    public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch, CallbackInfo ci) {
        WatutMod.getPlayerStatusManagerClient().setupRotationsHook((EntityModel)(Object)this, pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
    }*/

    @Inject(method = "extractRenderState", at = @At(value = "TAIL"))
    public void extractRenderState(T p_entity, S reusedState, float partialTick, CallbackInfo ci) {
        WatutMod.getPlayerStatusManagerClient().extractRenderState(p_entity, reusedState, partialTick);
    }

}