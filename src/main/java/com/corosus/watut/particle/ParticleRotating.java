package com.corosus.watut.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public abstract class ParticleRotating extends SpriteTexturedParticle {

    public boolean useCustomRotation = true;
    public float prevRotationYaw;
    public float rotationYaw;
    public float prevRotationPitch;
    public float rotationPitch;
    public float prevRotationRoll;
    public float rotationRoll;

    //removes particle once hits 0, other things should reset this to keep it spawned
    public int despawnCountdown = 40;


    public static IParticleRenderType PARTICLE_SHEET_TRANSLUCENT_NO_FACE_CULL = new IParticleRenderType() {
        public void begin(BufferBuilder p_107455_, TextureManager p_107456_) {
            RenderSystem.depthMask(true);
            //RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            p_107456_.bind(AtlasTexture.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            p_107455_.begin(7, DefaultVertexFormats.PARTICLE);
        }

        public void end(Tessellator p_107458_) {
            p_107458_.end();
            RenderSystem.enableCull();
        }

        public String toString() {
            return "PARTICLE_SHEET_TRANSLUCENT_NO_FACE_CULL";
        }
    };

    @Override
    public void tick() {
        despawnCountdown--;
        if (despawnCountdown <= 0) {
            remove();
        }
    }

    public void keepAlive() {
        despawnCountdown = 40;
    }

    public ParticleRotating(ClientWorld pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ);
    }

    public void setQuadSize(float size) {
        this.quadSize = size;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public IParticleRenderType getRenderType() {
        return PARTICLE_SHEET_TRANSLUCENT_NO_FACE_CULL;
    }

    public void render(IVertexBuilder pBuffer, ActiveRenderInfo pRenderInfo, float pPartialTicks) {
        Vector3d vec3 = pRenderInfo.getPosition();
        float f = (float)(MathHelper.lerp((double)pPartialTicks, this.xo, this.x) - vec3.x());
        float f1 = (float)(MathHelper.lerp((double)pPartialTicks, this.yo, this.y) - vec3.y());
        float f2 = (float)(MathHelper.lerp((double)pPartialTicks, this.zo, this.z) - vec3.z());
        Quaternion quaternion;
        if (useCustomRotation) {
            quaternion = new Quaternion(0, 0, 0, 1);
            quaternion.mul(Vector3f.YP.rotationDegrees(MathHelper.lerp(pPartialTicks, this.prevRotationYaw, rotationYaw)));
            quaternion.mul(Vector3f.XP.rotationDegrees(MathHelper.lerp(pPartialTicks, this.prevRotationPitch, rotationPitch)));
            quaternion.mul(Vector3f.ZP.rotationDegrees(MathHelper.lerp(pPartialTicks, this.prevRotationRoll, rotationRoll)));
        } else {
            if (this.roll == 0.0F) {
                quaternion = pRenderInfo.rotation();
            } else {
                quaternion = new Quaternion(pRenderInfo.rotation());
                //quaternion.rotateZ(MathHelper.lerp(pPartialTicks, this.oRoll, this.roll));
            }
        }

        Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float f3 = this.getQuadSize(pPartialTicks);

        for(int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.transform(quaternion);
            vector3f.mul(f3);
            vector3f.add(f, f1, f2);
        }

        float f6 = this.getU0();
        float f7 = this.getU1();
        float f4 = this.getV0();
        float f5 = this.getV1();
        int j = this.getLightColor(pPartialTicks);
        pBuffer.vertex((double)avector3f[0].x(), (double)avector3f[0].y(), (double)avector3f[0].z()).uv(f7, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex((double)avector3f[1].x(), (double)avector3f[1].y(), (double)avector3f[1].z()).uv(f7, f4).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex((double)avector3f[2].x(), (double)avector3f[2].y(), (double)avector3f[2].z()).uv(f6, f4).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex((double)avector3f[3].x(), (double)avector3f[3].y(), (double)avector3f[3].z()).uv(f6, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
    }

    public void setPosPrev(double pX, double pY, double pZ) {
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
    }
}
