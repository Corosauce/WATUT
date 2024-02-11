package com.corosus.watut.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ParticleStaticPartial extends ParticleRotating {

    public float customU1;
    public float customV1;

    public ParticleStaticPartial(ClientLevel pLevel, double pX, double pY, double pZ, TextureAtlasSprite sprite, float brightness, int subSizeX, int subSizeY) {
        super(pLevel, pX, pY, pZ);
        this.sprite = sprite;
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        float subSizeXFloat = (float)subSizeX / (float)this.sprite.contents().width();
        float getU0 = getU0();
        float getU1 = getU1();
        customU1 = getU0() + ((this.sprite.getU1() - getU0()) * subSizeXFloat);
        float subSizeYFloat = (float)subSizeY / (float)this.sprite.contents().height();
        customV1 = getV0() + ((this.sprite.getV1() - getV0()) * subSizeYFloat);

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

    protected float getU0() {
        return this.sprite.getU0();
    }

    protected float getU1() {
        return customU1;
    }

    protected float getV0() {
        return this.sprite.getV0();
    }

    protected float getV1() {
        return customV1;
    }

}
