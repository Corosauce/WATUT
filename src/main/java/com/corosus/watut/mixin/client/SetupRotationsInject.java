package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class SetupRotationsInject<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At(value = "TAIL"))
    public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch, CallbackInfo ci) {
        WatutMod.getPlayerStatusManagerClient().setupRotationsHook((EntityModel)(Object)this, pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
    }

}