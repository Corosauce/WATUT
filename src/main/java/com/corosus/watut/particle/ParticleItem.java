package com.corosus.watut.particle;

import com.corosus.coroutil.util.CULog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;

public class ParticleItem extends ItemPickupParticle {

    public static HashSet<String> itemBlacklist = new HashSet<>();

    private static int throwawayEntityId = -1;

    public ParticleItem(ClientLevel level, EntityRenderState itemRenderState, Vec3 to) {
        super(level, itemRenderState, Minecraft.getInstance().player, Vec3.ZERO);
        setTarget(to);
    }

    public static ParticleItem create(ClientLevel level, ItemStack itemStack, float xFrom, float yFrom, float zFrom, float xTo, float yTo, float zTo) {
        if (itemStack.isEmpty() || itemBlacklist.contains(itemStack.getItem().toString())) return null;
        try {
            ItemEntity itemEntity = new ItemEntity(level, xFrom, yFrom, zFrom, itemStack);
            itemEntity.setId(throwawayEntityId--);
            EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(itemEntity).createRenderState(itemEntity, 1.0F);
            return new ParticleItem(level, state, new Vec3(xTo, yTo, zTo));
        } catch (Exception exception) {
            CULog.err("ERROR, exception trying to render item: " + itemStack.getItem() + " - adding to ParticleItem render blacklist for this minecraft session");
            itemBlacklist.add(itemStack.getItem().toString());
            exception.printStackTrace();
            return null;
        }
    }

    private void setTarget(Vec3 to) {
        this.targetX = this.targetXOld = to.x;
        this.targetY = this.targetYOld = to.y;
        this.targetZ = this.targetZOld = to.z;
    }

    @Override
    public void tick() {
        this.life++;
        if (this.life == 3) {
            this.remove();
        }
        this.targetXOld = this.targetX;
        this.targetYOld = this.targetY;
        this.targetZOld = this.targetZ;
    }
}
