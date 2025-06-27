package com.corosus.watut.client;

import com.corosus.watut.DynamicGib;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DynamicGibRenderer extends AgeableMobRenderer<DynamicGib, DynamicGibRenderState, DynamicGibModel> {
    private static final ResourceLocation ANGRY_BEE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee_angry.png");
    private static final ResourceLocation ANGRY_NECTAR_BEE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee_angry_nectar.png");
    private static final ResourceLocation BEE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");
    private static final ResourceLocation NECTAR_BEE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee_nectar.png");

    public DynamicGibRenderer(EntityRendererProvider.Context p_173931_) {
        super(p_173931_, new DynamicGibModel(p_173931_.bakeLayer(ModelLayers.BEE)), new DynamicGibModel(p_173931_.bakeLayer(ModelLayers.BEE_BABY)), 0.4F);
    }

    public ResourceLocation getTextureLocation(DynamicGibRenderState p_363810_) {
        if (p_363810_.isAngry) {
            return p_363810_.hasNectar ? ANGRY_NECTAR_BEE_TEXTURE : ANGRY_BEE_TEXTURE;
        } else {
            return p_363810_.hasNectar ? NECTAR_BEE_TEXTURE : BEE_TEXTURE;
        }
    }

    public DynamicGibRenderState createRenderState() {
        return new DynamicGibRenderState();
    }

    public void extractRenderState(DynamicGib p_362879_, DynamicGibRenderState p_360596_, float p_365357_) {
        super.extractRenderState(p_362879_, p_360596_, p_365357_);
        /*p_360596_.rollAmount = p_362879_.getRollAmount(p_365357_);
        p_360596_.hasStinger = !p_362879_.hasStung();
        p_360596_.isOnGround = p_362879_.onGround() && p_362879_.getDeltaMovement().lengthSqr() < 1.0E-7;
        p_360596_.isAngry = p_362879_.isAngry();
        p_360596_.hasNectar = p_362879_.hasNectar();*/
    }
}
