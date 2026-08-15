package com.corosus.watut.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import com.mojang.math.Axis;
import org.joml.Quaternionf;

import java.util.HashSet;

public class ParticleItem extends ParticleRotating {

    public static HashSet<String> itemBlacklist = new HashSet<>();

    public ItemStack itemStack;
    public float xFrom;
    public float yFrom;
    public float zFrom;
    public float xTo;
    public float yTo;
    public float zTo;

    private static TextureAtlasSprite getSpriteForItem(ClientLevel level, ItemStack itemStack) {
        try {
            if (level != null && itemStack != null && !itemStack.isEmpty()) {
                ItemStackRenderState itemState = new ItemStackRenderState();
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(itemState, itemStack, ItemDisplayContext.GROUND, level, null, 0);
                Material.Baked material = itemState.pickParticleMaterial(level.getRandom());
                if (material != null && material.sprite() != null) {
                    return material.sprite();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public ParticleItem(ClientLevel pLevel, float brightness, ItemStack itemStack, Object renderBuffers, Object entityRenderDispatcher, float xFrom, float yFrom, float zFrom, float xTo, float yTo, float zTo) {
        super(pLevel, xFrom, yFrom, zFrom, getSpriteForItem(pLevel, itemStack));
        this.lifetime = Integer.MAX_VALUE;
        this.gravity = 0.0F;
        this.setSize(0.2F, 0.2F);
        this.quadSize = 0.5F;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.xFrom = xFrom;
        this.yFrom = yFrom;
        this.zFrom = zFrom;
        this.xTo = xTo;
        this.yTo = yTo;
        this.zTo = zTo;
        this.setColor(this.getColorRed() * brightness, this.getColorGreen() * brightness, this.getColorBlue() * brightness);
        this.itemStack = itemStack;
        this.rotationYaw = pLevel.getRandom().nextFloat() * 360;
    }

    public void setSize(float pWidth, float pHeight) {
        super.setSize(pWidth, pHeight);
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= 6) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
        }
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTicks) {
        if (itemBlacklist.contains(this.itemStack.getItem().toString())) return;

        float f = (float)(Mth.lerp(partialTicks, this.xo, this.x));
        float f1 = (float)(Mth.lerp(partialTicks, this.yo, this.y));
        float f2 = (float)(Mth.lerp(partialTicks, this.zo, this.z));

        float lerp = ((float)this.age + partialTicks) / 3.0F;
        double d0 = xTo;
        double d1 = yTo;
        double d2 = zTo;
        double curX = Mth.lerp(lerp, f, d0);
        double curY = Mth.lerp(lerp, f1, d1);
        double curZ = Mth.lerp(lerp, f2, d2);

        if (this.age >= 3) {
            curX = xTo;
            curY = yTo;
            curZ = zTo;
        }

        Quaternionf quaternion = new Quaternionf(0, 0, 0, 1);
        quaternion.mul(Axis.YP.rotationDegrees(this.rotationYaw));

        this.extractRotatedQuad(renderState, quaternion, (float)curX, (float)curY, (float)curZ, partialTicks);
    }
}
