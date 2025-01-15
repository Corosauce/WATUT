package com.corosus.watut.mixin;

import com.corosus.watut.WatutMod;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuDoClick {

    @Shadow
    public final NonNullList<Slot> slots = NonNullList.create();

    @Inject(method = "doClick", at = @At("HEAD"))
    private void doClickPre(int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_, CallbackInfo ci) {
        if (!p_150434_.level().isClientSide()) {
            //WatutMod.getPlayerStatusManagerServer().doClickPre((AbstractContainerMenu)((Object)this), p_150431_, p_150432_, p_150433_, p_150434_);
        }
    }

    @Inject(method = "doClick", at = @At("TAIL"))
    private void doClickPost(int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_, CallbackInfo ci) {
        if (!p_150434_.level().isClientSide()) {
            //WatutMod.getPlayerStatusManagerServer().doClickPost((AbstractContainerMenu)((Object)this), p_150431_, p_150432_, p_150433_, p_150434_);
        }
    }
}