package com.corosus.watut.mixin;

import com.corosus.watut.WatutMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//https://spark.lucko.me/PgzUbILKHt

@Mixin(Chicken.class)
public abstract class FixChickenLoadBomb {

    @Redirect(method = "readAdditionalSaveData",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Animal;readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"))
    public void readAdditionalSaveData(Animal instance, CompoundTag p_27576_) {
        //cancelled
        //System.out.println("cancelled");
    }

}