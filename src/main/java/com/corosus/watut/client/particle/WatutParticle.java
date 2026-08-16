package com.corosus.watut.client.particle;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Particella unificata per le icone e gli stati visualizzati sopra la testa del giocatore.
 * Supporta orientamento 3D arbitrario, animazioni a frame (SpriteSet), LoD e cropping parziale UV.
 */
public class WatutParticle extends SingleQuadParticle {

    public boolean useCustomRotation = true;
    public float prevRotationYaw;
    public float rotationYaw;
    public float prevRotationPitch;
    public float rotationPitch;
    public float prevRotationRoll;
    public float rotationRoll;
    public float brightness = 1.0F;

    private int despawnCountdown = 40;
    private SpriteSetPlayer spriteSet;
    private boolean isLoD = false;
    private float customU1;
    private float customV1;
    private boolean hasCustomUV = false;

    // Costruttore per sprite statico
    public WatutParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, float brightness) {
        super(level, x, y, z, sprite);
        initDefaults(brightness);
    }

    // Costruttore per SpriteSet animato o LoD
    public WatutParticle(ClientLevel level, double x, double y, double z, SpriteSetPlayer spriteSet, float brightness, boolean isLoD) {
        super(level, x, y, z, spriteSet != null ? spriteSet.first() : null);
        this.spriteSet = spriteSet;
        this.isLoD = isLoD;
        initDefaults(brightness);
        if (!isLoD && spriteSet != null) {
            this.setSpriteFromAge(spriteSet);
        }
    }

    // Costruttore con UV parziali (per texture di dimensioni non-quadrate es. 176x166)
    public WatutParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, float brightness, int subSizeX, int subSizeY) {
        super(level, x, y, z, sprite);
        initDefaults(brightness);
        if (this.sprite != null && this.sprite.contents() != null) {
            float baseSheetSize = 256.0f;
            float fractionX = (float) subSizeX / baseSheetSize;
            float fractionY = (float) subSizeY / baseSheetSize;
            this.customU1 = getU0() + ((this.sprite.getU1() - getU0()) * fractionX);
            this.customV1 = getV0() + ((this.sprite.getV1() - getV0()) * fractionY);
            this.hasCustomUV = true;
        }
    }

    private void initDefaults(float brightness) {
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.brightness = brightness;
        this.setColor(this.rCol * brightness, this.gCol * brightness, this.bCol * brightness);
    }

    @Override
    public void tick() {
        despawnCountdown--;
        if (despawnCountdown <= 0) {
            remove();
            return;
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            if (spriteSet != null && !isLoD) {
                this.setSpriteFromAge(this.spriteSet);
            }
        }
    }

    public void updateLoDFromDistance(float distanceToCamera) {
        if (isLoD && spriteSet != null && spriteSet.getList() != null && !spriteSet.getList().isEmpty()) {
            float step = 3.0F;
            int maxIndex = spriteSet.getList().size() - 1;
            int i = (int) Math.max(0, Math.min(maxIndex, ((distanceToCamera - step) / step) + 1));
            this.setSprite(spriteSet.getList().get(i));
        }
    }

    public void keepAlive() {
        this.despawnCountdown = 40;
    }

    public void setQuadSize(float size) {
        this.quadSize = size;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setPosPrev(double px, double py, double pz) {
        this.xo = px;
        this.yo = py;
        this.zo = pz;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
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

    @Override
    protected float getU1() {
        return hasCustomUV ? customU1 : super.getU1();
    }

    @Override
    protected float getV1() {
        return hasCustomUV ? customV1 : super.getV1();
    }
}
