package com.corosus.watut.particle;

import com.corosus.watut.spritesets.SpriteSetPlayer;
import net.minecraft.client.multiplayer.ClientLevel;

public class ParticleStaticLoD extends ParticleRotating {

    private final SpriteSetPlayer sprites;

    public ParticleStaticLoD(ClientLevel pLevel, double pX, double pY, double pZ, SpriteSetPlayer pSprites) {
        super(pLevel, pX, pY, pZ);
        this.sprites = pSprites;
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.setParticleFromDistanceToCamera(0);
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

    public void setParticleFromDistanceToCamera(float distanceToCamera) {
        float step = 3F;
        int i = (int) Math.max(0, Math.min(sprites.getFrames()-1, ((distanceToCamera - step) / step) + 1));
        this.setSprite(sprites.getList().get(i));
    }

}
