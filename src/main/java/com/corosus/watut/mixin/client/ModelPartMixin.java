package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelPart.class)
public abstract class ModelPartMixin {


    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    public void render(ModelPart instance, PoseStack poseStack, VertexConsumer vertexConsumer, int buffer, int packedLight, int packedOverlay) {

        if (WatutMod.getPlayerStatusManagerClient().renderModelPart(instance, poseStack, vertexConsumer, buffer, packedLight, packedOverlay)) {
            instance.render(poseStack, vertexConsumer, buffer, packedLight, packedOverlay);
        }


    }

    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    public void translateAndRotate(ModelPart instance, PoseStack poseStack) {

        if (WatutMod.getPlayerStatusManagerClient().translateAndRotate(instance, poseStack)) {
            instance.translateAndRotate(poseStack);
        }


    }

}