package com.corosus.watut.mixin.client;

import com.corosus.watut.Watut;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class SetupRotationsOverride {

    /*@Redirect(method = "setupRotations",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"))
    protected void setupRotations(LivingEntityRenderer renderer, LivingEntity pEntityLiving, PoseStack pMatrixStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
        //Watut.dbg("setupRotations hook");
        Watut.getPlayerStatusManagerClient().setupRotationsHook(renderer, pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        renderer.setupRotations(pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks);
    }*/

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    public void setupAnim(EntityModel model, Entity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        model.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
        Watut.getPlayerStatusManagerClient().setupRotationsHook(model, pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
        //Watut.dbg("setupRotations hook");
        //renderer.setupRotations(pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks);
        //Watut.getPlayerStatusManagerClient().setupRotationsHook(renderer, pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks);
    }
}