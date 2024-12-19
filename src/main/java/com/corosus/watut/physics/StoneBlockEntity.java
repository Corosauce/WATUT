package com.corosus.watut.physics;

import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.shape.MinecraftShape;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A stone block entity that uses an internally stored {@link EntityRigidBody} to behave using realistic physics.
 */
public class StoneBlockEntity extends LivingEntity implements EntityPhysicsElement {

    private final EntityRigidBody rigidBody;

    public StoneBlockEntity(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        this.rigidBody = new EntityRigidBody(this);
        this.rigidBody.setMass(20.0f); // 20 kg

    }/*

    @Override
    public MinecraftShape.Convex createShape() {
        return EntityPhysicsElement.super.createShape();
    }*/

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot p_21127_) {
        return new ItemStack(Items.AIR);
    }

    @Override
    public void setItemSlot(EquipmentSlot p_21036_, ItemStack p_21037_) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return null;
    }

    @Override
    public boolean isSilent() {
        return true;
    }

    @Override
    public EntityRigidBody getRigidBody() {
        return this.rigidBody;
    }

}