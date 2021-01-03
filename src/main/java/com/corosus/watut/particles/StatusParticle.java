package com.corosus.watut.particles;

import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;

public class StatusParticle extends SpriteTexturedParticle {

    public StatusParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void setSprite(TextureAtlasSprite sprite) {
        super.setSprite(sprite);
    }

    @Override
    public void setSize(float particleWidth, float particleHeight) {
        super.setSize(particleWidth, particleHeight);
    }

    public void setScale(float scale) {
        this.particleScale = scale;
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_LIT;
    }

}
