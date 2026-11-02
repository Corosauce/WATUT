package com.corosus.watut.particle;

import com.corosus.watut.WatutRenderPipelines;
import com.corosus.watut.client.ParticleRenderTypeOld;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
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

    public static boolean USE_VANILLA_TICK = false;

    public static ParticleRenderTypeOld CUSTOM = new ParticleRenderTypeOld() {
        public String toString() {
            return "CUSTOM";
        }
    };

    public static ParticleRenderTypeOld PARTICLE_SHEET_TRANSLUCENT_NO_FACE_CULL = new ParticleRenderTypeOld() {
        public String toString() {
            return "PARTICLE_SHEET_TRANSLUCENT_NO_FACE_CULL";
        }
    };

    public static ParticleRenderTypeOld TERRAIN_SHEET_TRANSLUCENT_NO_FACE_CULL = new ParticleRenderTypeOld() {
        public String toString() {
            return "TERRAIN_SHEET_TRANSLUCENT_NO_FACE_CULL";
        }
    };

    @Override
    public void tick() {
        if (USE_VANILLA_TICK) {
            super.tick();
        }
        despawnCountdown--;
        if (despawnCountdown <= 0) {
            this.remove();
        }
    }

    public float getColorRed() {
        return this.rCol;
    }

    public float getColorGreen() {
        return this.gCol;
    }

    public float getColorBlue() {
        return this.bCol;
    }

    public void keepAlive() {
        despawnCountdown = 40;
    }

    public ParticleRotating(ClientLevel pLevel, double pX, double pY, double pZ, @Nullable TextureAtlasSprite pSprite) {
        super(pLevel, pX, pY, pZ, pSprite);
    }

    public ParticleRotating(ClientLevel pLevel, double pX, double pY, double pZ) {
        super(pLevel, pX, pY, pZ, null);
    }

    public void setQuadSize(float size) {
        this.quadSize = size;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    @Deprecated
    public ParticleRenderTypeOld getRenderTypeOld() {
        return PARTICLE_SHEET_TRANSLUCENT_NO_FACE_CULL;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, WatutRenderPipelines.TRANSLUCENT_PARTICLE_NO_CULL);
    }

    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        if (!hasTexture()) return;
        if (useCustomRotation) {
            Quaternionf quaternion = new Quaternionf(0, 0, 0, 1);
            quaternion.mul(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationYaw, rotationYaw)));
            quaternion.mul(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationPitch, rotationPitch)));
            quaternion.mul(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationRoll, rotationRoll)));
            this.extractRotatedQuad(state, camera, quaternion, partialTicks);
        } else {
            super.extract(state, camera, partialTicks);
        }
    }

    protected boolean hasTexture() {
        return this.sprite != null;
    }

    @Override
    protected void extractRotatedQuad(QuadParticleRenderState state, Quaternionf rotation, float x, float y, float z, float partialTicks) {
        state.add(
                this.getLayer(),
                x, y, z,
                rotation.x, rotation.y, rotation.z, rotation.w,
                this.getQuadSize(partialTicks),
                this.getU1(), this.getU0(), this.getV0(), this.getV1(),
                ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol),
                this.getLightCoords(partialTicks)
        );
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
