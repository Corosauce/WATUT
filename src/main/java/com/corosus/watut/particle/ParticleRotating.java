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

public abstract class ParticleRotating extends SingleQuadParticle {

    public boolean useCustomRotation = true;
    public float prevRotationYaw;
    public float rotationYaw;
    public float prevRotationPitch;
    public float rotationPitch;
    public float prevRotationRoll;
    public float rotationRoll;
    public float brightness = 1F;

    //removes particle once hits 0, other things should reset this to keep it spawned
    public int despawnCountdown = 40;

    @Override
    public void tick() {
        despawnCountdown--;
        if (despawnCountdown <= 0) {
            remove();
        }
    }

    public float getColorRed() {
        return rCol;
    }

    public float getColorGreen() {
        return gCol;
    }

    public float getColorBlue() {
        return bCol;
    }

    public void keepAlive() {
        despawnCountdown = 40;
    }

    public ParticleRotating(ClientLevel pLevel, double pX, double pY, double pZ) {
        this(pLevel, pX, pY, pZ, null);
    }

    public ParticleRotating(ClientLevel pLevel, double pX, double pY, double pZ, TextureAtlasSprite sprite) {
        super(pLevel, pX, pY, pZ, sprite);
    }

    public void setQuadSize(float size) {
        this.quadSize = size;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTicks) {
        if (useCustomRotation) {
            Quaternionf quaternion = new Quaternionf(0, 0, 0, 1);
            quaternion.mul(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationYaw, rotationYaw)));
            quaternion.mul(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationPitch, rotationPitch)));
            quaternion.mul(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationRoll, rotationRoll)));
            this.extractRotatedQuad(renderState, camera, quaternion, partialTicks);

            Quaternionf backQuaternion = new Quaternionf(quaternion);
            backQuaternion.mul(Axis.YP.rotationDegrees(180));
            this.extractRotatedQuad(renderState, camera, backQuaternion, partialTicks);
        } else {
            super.extract(renderState, camera, partialTicks);
        }
    }

    public void setPosPrev(double pX, double pY, double pZ) {
        this.xo = pX;
        this.yo = pY;
        this.zo = pZ;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }
}
