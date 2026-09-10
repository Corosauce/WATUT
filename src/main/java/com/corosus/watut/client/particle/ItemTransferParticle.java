package com.corosus.watut.client.particle;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.HashSet;
import java.util.Set;

/**
 * Particella che visualizza un oggetto in movimento tra il giocatore e un contenitore (es. baule).
 */
public class ItemTransferParticle extends WatutParticle {

    public static final Set<String> ITEM_BLACKLIST = new HashSet<>();

    public final ItemStack itemStack;
    public final float xFrom, yFrom, zFrom;
    public final float xTo, yTo, zTo;

    private static TextureAtlasSprite getSpriteForItem(ClientLevel level, ItemStack stack) {
        try {
            if (level != null && stack != null && !stack.isEmpty()) {
                ItemStackRenderState state = new ItemStackRenderState();
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GROUND, level, null, 0);
                Material.Baked material = state.pickParticleMaterial(level.getRandom());
                if (material != null && material.sprite() != null) {
                    return material.sprite();
                }
            }
        } catch (Exception ignored) {}
        return ParticleRegistry.CHEST.getSprite();
    }

    public ItemTransferParticle(ClientLevel level, float brightness, ItemStack itemStack,
                                float xFrom, float yFrom, float zFrom,
                                float xTo, float yTo, float zTo) {
        super(level, xFrom, yFrom, zFrom, getSpriteForItem(level, itemStack), brightness);
        this.itemStack = itemStack;
        this.xFrom = xFrom;
        this.yFrom = yFrom;
        this.zFrom = zFrom;
        this.xTo = xTo;
        this.yTo = yTo;
        this.zTo = zTo;
        this.rotationYaw = level.getRandom().nextFloat() * 360.0F;
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
        if (itemStack == null || this.sprite == null || ITEM_BLACKLIST.contains(itemStack.getItem().toString())) return;

        float f = (float) Mth.lerp(partialTicks, this.xo, this.x);
        float f1 = (float) Mth.lerp(partialTicks, this.yo, this.y);
        float f2 = (float) Mth.lerp(partialTicks, this.zo, this.z);

        float lerp = ((float) this.age + partialTicks) / 3.0F;
        double curX = Mth.lerp(lerp, f, xTo);
        double curY = Mth.lerp(lerp, f1, yTo);
        double curZ = Mth.lerp(lerp, f2, zTo);

        if (this.age >= 3) {
            curX = xTo;
            curY = yTo;
            curZ = zTo;
        }

        Quaternionf quaternion = new Quaternionf(0, 0, 0, 1);
        quaternion.mul(Axis.YP.rotationDegrees(this.rotationYaw));

        this.extractRotatedQuad(renderState, quaternion, (float) curX, (float) curY, (float) curZ, partialTicks);
    }
}
