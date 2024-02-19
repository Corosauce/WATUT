package com.corosus.watut.particle;

import com.corosus.watut.client.ScreenCapturing;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParticleDynamic extends ParticleRotating {

    public ParticleRenderType particleRenderType;

    public ParticleRenderType getRenderType() {
        return particleRenderType;
    }


    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, ParticleRenderType particleRenderType) {
        this(pLevel, pX, pY, pZ, particleRenderType, 1F);
    }

    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, ParticleRenderType particleRenderType, float brightness) {
        super(pLevel, pX, pY, pZ);
        this.particleRenderType = particleRenderType;
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.setColor(this.getColorRed() * brightness, this.getColorGreen() * brightness, this.getColorBlue() * brightness);
    }

    public void setSize(float pWidth, float pHeight) {
        super.setSize(pWidth, pHeight);
    }

    public void tick() {
        super.tick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
        }
    }

    public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        Vec3 vec3 = pRenderInfo.getPosition();
        float f = (float)(Mth.lerp(pPartialTicks, this.xo, this.x) - vec3.x());
        float f1 = (float)(Mth.lerp(pPartialTicks, this.yo, this.y) - vec3.y());
        float f2 = (float)(Mth.lerp(pPartialTicks, this.zo, this.z) - vec3.z());
        Quaternionf quaternion;
        if (useCustomRotation) {
            quaternion = new Quaternionf(0, 0, 0, 1);
            quaternion.mul(Axis.YP.rotationDegrees(Mth.lerp(pPartialTicks, this.prevRotationYaw, rotationYaw)));
            quaternion.mul(Axis.XP.rotationDegrees(Mth.lerp(pPartialTicks, this.prevRotationPitch, rotationPitch)));
            quaternion.mul(Axis.ZP.rotationDegrees(Mth.lerp(pPartialTicks, this.prevRotationRoll, rotationRoll)));
        } else {
            if (this.roll == 0.0F) {
                quaternion = pRenderInfo.rotation();
            } else {
                quaternion = new Quaternionf(pRenderInfo.rotation());
                quaternion.rotateZ(Mth.lerp(pPartialTicks, this.oRoll, this.roll));
            }
        }

        //Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
        float aspectRatio = 1920F/1080F;
        float height = 1F / aspectRatio;
        Vector3f[] avector3f = new Vector3f[]{
                new Vector3f(-1.0F, height, 0.0F),
                new Vector3f(-1.0F, -height, 0.0F),
                new Vector3f(1.0F, -height, 0.0F),
                new Vector3f(1.0F, height, 0.0F)};
        float f3 = this.getQuadSize(pPartialTicks);

        for(int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.rotate(quaternion);
            vector3f.mul(f3 * 3F);
            vector3f.add(f, f1, f2);
        }

        /*float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();*/

        float u0 = 0;
        float u1 = 1;
        float v0 = 0;
        float v1 = 1;

        int j = this.getLightColor(pPartialTicks);
        pBuffer.vertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
        pBuffer.vertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
    }

}
