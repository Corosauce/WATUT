package com.corosus.watut.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ParticleStatic extends ParticleRotating {

    public ParticleStatic(ClientLevel pLevel, double pX, double pY, double pZ, TextureAtlasSprite sprite) {
        super(pLevel, pX, pY, pZ);
        this.sprite = sprite;
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
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

}
