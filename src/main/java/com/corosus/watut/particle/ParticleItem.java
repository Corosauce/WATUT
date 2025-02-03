package com.corosus.watut.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ParticleItem extends ParticleRotating {

    public BakedModel bakedModel;
    public ItemStack itemStack;
    private final RenderBuffers renderBuffers;
    private final EntityRenderDispatcher entityRenderDispatcher;
    public float xFrom;
    public float yFrom;
    public float zFrom;
    public float xTo;
    public float yTo;
    public float zTo;

    public ParticleItem(ClientLevel pLevel, float brightness, ItemStack itemStack, RenderBuffers renderBuffers, EntityRenderDispatcher entityRenderDispatcher, float xFrom, float yFrom, float zFrom, float xTo, float yTo, float zTo) {
        super(pLevel, xFrom, yFrom, zFrom);
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.xFrom = xFrom;
        this.yFrom = yFrom;
        this.zFrom = zFrom;
        this.xTo = xTo;
        this.yTo = yTo;
        this.zTo = zTo;
        this.setColor(this.getColorRed() * brightness, this.getColorGreen() * brightness, this.getColorBlue() * brightness);
        this.bakedModel = Minecraft.getInstance().getItemRenderer().getModel(itemStack, Minecraft.getInstance().level, null, 0);
        this.itemStack = itemStack;
        this.entityRenderDispatcher = entityRenderDispatcher;
        this.renderBuffers = renderBuffers;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return TERRAIN_SHEET_TRANSLUCENT_NO_FACE_CULL;
    }

    protected float getU0() {
        return bakedModel.getParticleIcon().getU0();
    }

    protected float getU1() {
        return bakedModel.getParticleIcon().getU1();
    }

    protected float getV0() {
        return bakedModel.getParticleIcon().getV0();
    }

    protected float getV1() {
        return bakedModel.getParticleIcon().getV1();
    }

    public void setSize(float pWidth, float pHeight) {
        super.setSize(pWidth, pHeight);
    }

    public void tick() {
        super.tick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= 6) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
        }
    }

    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        Vec3 vec3 = pRenderInfo.getPosition();
        float f = (float)(Mth.lerp(pPartialTicks, this.xo, this.x));
        float f1 = (float)(Mth.lerp(pPartialTicks, this.yo, this.y));
        float f2 = (float)(Mth.lerp(pPartialTicks, this.zo, this.z));

        float lerp = ((float)this.age + pPartialTicks) / 3.0F;
        //lerp *= lerp;
        double d0 = xTo;
        double d1 = yTo;
        double d2 = zTo;
        double x = Mth.lerp(lerp, f, d0);
        double y = Mth.lerp(lerp, f1, d1);
        double z = Mth.lerp(lerp, f2, d2);

        if (this.age >= 3) {
            x = xTo;
            y = yTo;
            z = zTo;
        }

        x = x - vec3.x();
        y = y - vec3.y();
        z = z - vec3.z();

        int j = this.getLightColor(pPartialTicks);

        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(quadSize, quadSize, quadSize);

        //RenderSystem.disableDepthTest();
        //RenderSystem.depthMask(false);
        Minecraft.getInstance().getItemRenderer().render(itemStack, ItemDisplayContext.GUI, false, pose, renderBuffers.bufferSource(), j, OverlayTexture.NO_OVERLAY, bakedModel);
        renderBuffers.bufferSource().endBatch();
    }

}
