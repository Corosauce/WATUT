package com.corosus.watut.particle;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class ParticleDynamic extends ParticleRotating {

    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, Object particleRenderType) {
        this(pLevel, pX, pY, pZ, particleRenderType, 1F);
    }

    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, Object particleRenderType, float brightness) {
        super(pLevel, pX, pY, pZ, (TextureAtlasSprite) null);
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

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTicks) {
        Quaternionf quaternion;
        if (useCustomRotation) {
            quaternion = new Quaternionf(0, 0, 0, 1);
            quaternion.mul(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationYaw, rotationYaw)));
            quaternion.mul(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationPitch, rotationPitch)));
            quaternion.mul(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationRoll, rotationRoll)));
        } else {
            quaternion = new Quaternionf(camera.rotation());
            if (this.roll != 0.0F) {
                quaternion.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
            }
        }
        this.extractRotatedQuad(renderState, camera, quaternion, partialTicks);
    }
}
