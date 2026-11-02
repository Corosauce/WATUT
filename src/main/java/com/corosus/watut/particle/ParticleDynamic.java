package com.corosus.watut.particle;

import com.corosus.watut.client.ParticleRenderTypeOld;
import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.math.Axis;
import net.minecraft.client.particle.SingleQuadParticle;
import com.corosus.watut.WatutRenderPipelines;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParticleDynamic extends ParticleRotating {

    public ParticleRenderTypeOld particleRenderType;

    public ParticleRenderTypeOld getRenderTypeOld() {
        return particleRenderType;
    }


    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, ParticleRenderTypeOld particleRenderType) {
        this(pLevel, pX, pY, pZ, particleRenderType, 1F);
    }

    public ParticleDynamic(ClientLevel pLevel, double pX, double pY, double pZ, ParticleRenderTypeOld particleRenderType, float brightness) {
        super(pLevel, pX, pY, pZ);
        this.particleRenderType = particleRenderType;
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        //this.quadSize = 1F;
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
    protected boolean hasTexture() {
        return particleRenderType != null && particleRenderType.getTextureId() != null;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return new SingleQuadParticle.Layer(true, particleRenderType.getTextureId(), WatutRenderPipelines.TRANSLUCENT_PARTICLE_NO_CULL);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            return (float) (this.quadSize * 4F * ConfigClient.particleSizeScale);
        }
        return (float) (this.quadSize * 2F * ConfigClient.particleSizeScale);
    }

    @Override
    protected float getU0() {
        return 0;
    }

    @Override
    protected float getU1() {
        return 1;
    }

    @Override
    protected float getV0() {
        return 1;
    }

    @Override
    protected float getV1() {
        return 0;
    }

}
