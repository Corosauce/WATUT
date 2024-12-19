package com.corosus.watut.physics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class StoneBlockEntityModel extends EntityModel<StoneBlockEntity> {

    private final ModelPart modelPart;

    public StoneBlockEntityModel(int x, int y, int z) {
        var cuboidData = CubeListBuilder.create().addBox(0, 0, 0, x, y, z).getCubes();
        var cuboids = new ArrayList<ModelPart.Cube>();

        for (var data : cuboidData) {
            cuboids.add(data.bake(32, 32));
        }

       modelPart = new ModelPart(cuboids, new HashMap<>());
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, float f, float g, float h, float k) {
        modelPart.render(poseStack, vertexConsumer, i, j, f, g, h, k);
    }

    @Override
    public void setupAnim(StoneBlockEntity p_102618_, float p_102619_, float p_102620_, float p_102621_, float p_102622_, float p_102623_) {
    }
}
