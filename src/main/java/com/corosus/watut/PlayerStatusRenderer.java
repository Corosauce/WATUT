package com.corosus.watut;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.IWorldReader;

public class PlayerStatusRenderer {

    private static final RenderType SHADOW_RENDER_TYPE = RenderType.getEntityShadow(new ResourceLocation("textures/misc/shadow.png"));

    public static void renderShadow(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, Entity entityIn, float partialTicks, IWorldReader worldIn, float sizeIn) {
        float f = sizeIn;
        if (entityIn instanceof MobEntity) {
            MobEntity mobentity = (MobEntity)entityIn;
            if (mobentity.isChild()) {
                f = sizeIn * 0.5F;
            }
        }

        double x = MathHelper.lerp((double)partialTicks, entityIn.lastTickPosX, entityIn.getPosX());
        double y = MathHelper.lerp((double)partialTicks, entityIn.lastTickPosY, entityIn.getPosY() + 2);
        double z = MathHelper.lerp((double)partialTicks, entityIn.lastTickPosZ, entityIn.getPosZ());
        int i = MathHelper.floor(x - (double)f);
        int j = MathHelper.floor(x + (double)f);
        int k = MathHelper.floor(y - (double)f);
        int l = MathHelper.floor(y);
        int i1 = MathHelper.floor(z - (double)f);
        int j1 = MathHelper.floor(z + (double)f);
        MatrixStack.Entry matrixstack$entry = matrixStackIn.getLast();
        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(SHADOW_RENDER_TYPE);

        float alpha = 0.5F;
        float texU = 0;
        float texV = 0;

        /*for(BlockPos blockpos : BlockPos.getAllInBoxMutable(new BlockPos(i, k, i1), new BlockPos(j, l, j1))) {
            renderBlockShadow(matrixstack$entry, ivertexbuilder, worldIn, blockpos, x, y, z, f, weightIn);
        }*/

        /*Minecraft.getInstance().worldRenderer.
        Vector3d vector3d = entityrenderer.getRenderOffset(entityIn, partialTicks);
        matrixStackIn.translate(-vector3d.getX(), -vector3d.getY(), -vector3d.getZ());*/

        //matrixStackIn.pop();

        Vector3f[] avector3f = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)};
        /*float f7 = this.getMinU();
        float f8 = this.getMaxU();
        float f5 = this.getMinV();
        float f6 = this.getMaxV();*/
        /*float f7 = 0;
        float f8 = 64;
        float f5 = 0;
        float f6 = 64;*/
        float f7 = 0;
        float f8 = 1;
        float f5 = 0;
        float f6 = 1;

        //Tessellator.getInstance().getBuffer().begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
        //Tessellator.getInstance().getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

        /*ivertexbuilder.pos(matrixstack$entry.getMatrix(), avector3f[0].getX(), avector3f[0].getY(), avector3f[0].getZ())
                .tex(texU, texV)
                .color(1.0F, 1.0F, 1.0F, alpha)
                *//*.overlay(OverlayTexture.NO_OVERLAY).lightmap(15728880)*//**//*.normal(matrixstack$entry.getNormal(), 0.0F, 1.0F, 0.0F)*//*.endVertex();

        ivertexbuilder.pos(matrixstack$entry.getMatrix(), avector3f[1].getX(), avector3f[1].getY(), avector3f[1].getZ())
                .tex(texU, texV)
                .color(1.0F, 1.0F, 1.0F, alpha)
                *//*.overlay(OverlayTexture.NO_OVERLAY).lightmap(15728880)*//**//*.normal(matrixstack$entry.getNormal(), 0.0F, 1.0F, 0.0F)*//*.endVertex();

        ivertexbuilder.pos(matrixstack$entry.getMatrix(), avector3f[2].getX(), avector3f[2].getY(), avector3f[2].getZ())
                .tex(texU, texV)
                .color(1.0F, 1.0F, 1.0F, alpha)
                *//*.overlay(OverlayTexture.NO_OVERLAY).lightmap(15728880)*//**//*.normal(matrixstack$entry.getNormal(), 0.0F, 1.0F, 0.0F)*//*.endVertex();

        ivertexbuilder.pos(matrixstack$entry.getMatrix(), avector3f[3].getX(), avector3f[3].getY(), avector3f[3].getZ())
                .tex(texU, texV)
                .color(1.0F, 1.0F, 1.0F, alpha)
                *//*.overlay(OverlayTexture.NO_OVERLAY).lightmap(15728880)*//**//*.normal(matrixstack$entry.getNormal(), 0.0F, 1.0F, 0.0F)*//*.endVertex();*/

        /*ivertexbuilder.pos((double)avector3f[0].getX(), (double)avector3f[0].getY(), (double)avector3f[0].getZ()).tex(f8, f6).color(1.0F, 1.0F, 1.0F, alpha).lightmap(j).endVertex();
        ivertexbuilder.pos((double)avector3f[0].getX(), (double)avector3f[0].getY(), (double)avector3f[0].getZ()).tex(f8, f6).color(1.0F, 1.0F, 1.0F, alpha).lightmap(j).endVertex();
        ivertexbuilder.pos((double)avector3f[0].getX(), (double)avector3f[0].getY(), (double)avector3f[0].getZ()).tex(f8, f6).color(1.0F, 1.0F, 1.0F, alpha).lightmap(j).endVertex();
        ivertexbuilder.pos((double)avector3f[0].getX(), (double)avector3f[0].getY(), (double)avector3f[0].getZ()).tex(f8, f6).color(1.0F, 1.0F, 1.0F, alpha).lightmap(j).endVertex();

        Tessellator.getInstance().draw();*/

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexbuffer = tessellator.getBuffer();
        Minecraft.getInstance().getTextureManager().bindTexture(new ResourceLocation("textures/misc/shadow.png"));
        vertexbuffer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
        vertexbuffer.pos(x + avector3f[0].getX(), y + avector3f[0].getY(), z + avector3f[0].getZ()).tex(f8, f6).color(1F, 1F, 1F, 0.5F).lightmap(j).endVertex();
        vertexbuffer.pos(x + avector3f[1].getX(), y + avector3f[1].getY(), z + avector3f[1].getZ()).tex(f8, f5).color(1F, 1F, 1F, 0.5F).lightmap(j).endVertex();
        vertexbuffer.pos(x + avector3f[2].getX(), y + avector3f[2].getY(), z + avector3f[2].getZ()).tex(f7, f5).color(1F, 1F, 1F, 0.5F).lightmap(j).endVertex();
        vertexbuffer.pos(x + avector3f[3].getX(), y + avector3f[3].getY(), z + avector3f[3].getZ()).tex(f7, f6).color(1F, 1F, 1F, 0.5F).lightmap(j).endVertex();
        Tessellator.getInstance().draw();

    }

}
