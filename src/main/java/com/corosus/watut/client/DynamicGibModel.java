//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.corosus.watut.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DynamicGibModel extends EntityModel<DynamicGibRenderState> {
    private static final String BONE = "bone";
    private final ModelPart bone;
    private float rollAmount;

    public DynamicGibModel(ModelPart root) {
        super(root);
        this.bone = root.getChild("bone");
        ModelPart modelpart = this.bone.getChild("body");/*
        this.stinger = modelpart.getChild("stinger");
        this.leftAntenna = modelpart.getChild("left_antenna");
        this.rightAntenna = modelpart.getChild("right_antenna");
        this.rightWing = this.bone.getChild("right_wing");
        this.leftWing = this.bone.getChild("left_wing");
        this.frontLeg = this.bone.getChild("front_legs");
        this.midLeg = this.bone.getChild("middle_legs");
        this.backLeg = this.bone.getChild("back_legs");*/
    }

    /*public ModelPart getModelPart() {

    }*/

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));
        PartDefinition partdefinition2 = partdefinition1.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F), PartPose.ZERO);
        partdefinition2.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(26, 7).addBox(0.0F, -1.0F, 5.0F, 0.0F, 1.0F, 2.0F), PartPose.ZERO);
        partdefinition2.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(2, 0).addBox(1.5F, -2.0F, -3.0F, 1.0F, 2.0F, 3.0F), PartPose.offset(0.0F, -2.0F, -5.0F));
        partdefinition2.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(2, 3).addBox(-2.5F, -2.0F, -3.0F, 1.0F, 2.0F, 3.0F), PartPose.offset(0.0F, -2.0F, -5.0F));
        CubeDeformation cubedeformation = new CubeDeformation(0.001F);
        partdefinition1.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 18).addBox(-9.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F, cubedeformation), PartPose.offsetAndRotation(-1.5F, -4.0F, -3.0F, 0.0F, -0.2618F, 0.0F));
        partdefinition1.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F, cubedeformation), PartPose.offsetAndRotation(1.5F, -4.0F, -3.0F, 0.0F, 0.2618F, 0.0F));
        partdefinition1.addOrReplaceChild("front_legs", CubeListBuilder.create().addBox("front_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 1), PartPose.offset(1.5F, 3.0F, -2.0F));
        partdefinition1.addOrReplaceChild("middle_legs", CubeListBuilder.create().addBox("middle_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 3), PartPose.offset(1.5F, 3.0F, 0.0F));
        partdefinition1.addOrReplaceChild("back_legs", CubeListBuilder.create().addBox("back_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 5), PartPose.offset(1.5F, 3.0F, 2.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setupAnim(DynamicGibRenderState p_362113_) {
        super.setupAnim(p_362113_);
        this.rollAmount = p_362113_.rollAmount;
        /*this.stinger.visible = p_362113_.hasStinger;
        float f1;
        if (!p_362113_.isOnGround) {
            f1 = p_362113_.ageInTicks * 120.32113F * 0.017453292F;
            this.rightWing.yRot = 0.0F;
            this.rightWing.zRot = Mth.cos(f1) * 3.1415927F * 0.15F;
            this.leftWing.xRot = this.rightWing.xRot;
            this.leftWing.yRot = this.rightWing.yRot;
            this.leftWing.zRot = -this.rightWing.zRot;
            this.frontLeg.xRot = 0.7853982F;
            this.midLeg.xRot = 0.7853982F;
            this.backLeg.xRot = 0.7853982F;
        }

        if (!p_362113_.isAngry && !p_362113_.isOnGround) {
            f1 = Mth.cos(p_362113_.ageInTicks * 0.18F);
            this.bone.xRot = 0.1F + f1 * 3.1415927F * 0.025F;
            this.leftAntenna.xRot = f1 * 3.1415927F * 0.03F;
            this.rightAntenna.xRot = f1 * 3.1415927F * 0.03F;
            this.frontLeg.xRot = -f1 * 3.1415927F * 0.1F + 0.3926991F;
            this.backLeg.xRot = -f1 * 3.1415927F * 0.05F + 0.7853982F;
            this.bone.y -= Mth.cos(p_362113_.ageInTicks * 0.18F) * 0.9F;
        }*/

        if (this.rollAmount > 0.0F) {
            this.bone.xRot = Mth.rotLerpRad(this.rollAmount, this.bone.xRot, 3.0915928F);
        }

    }
}
