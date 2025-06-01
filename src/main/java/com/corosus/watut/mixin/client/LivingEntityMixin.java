package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)

//extracted the relevant generics from LivingEntityRenderer class definition
public abstract class LivingEntityMixin {


    @Redirect(method = "handleDamageEvent",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/WalkAnimationState;setSpeed(F)V"))
    public void setSpeed(WalkAnimationState instance, float speed) {



    }

}