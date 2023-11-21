package com.corosus.watut.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;

public class ParticleAnimated extends ParticleRotating {

    private final SpriteSet sprites;

    public ParticleAnimated(ClientLevel pLevel, double pX, double pY, double pZ, SpriteSet pSprites) {
        super(pLevel, pX, pY, pZ);
        this.sprites = pSprites;
        //this.lifetime = Integer.MAX_VALUE;
        //TODO: temp until i clean up stagnant particles from disconnects etc
        this.lifetime = 20*60*1;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.setSpriteFromAge(pSprites);
    }

    public void setSize(float pWidth, float pHeight) {
        super.setSize(pWidth, pHeight);
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            //this.yd -= (double)this.gravity;
            this.move(this.xd, this.yd, this.zd);
            this.setSpriteFromAge(this.sprites);
        }
    }

}
