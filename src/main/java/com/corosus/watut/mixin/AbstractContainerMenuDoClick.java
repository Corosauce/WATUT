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
    private void doClickPre(int pSlotId, int pButton, ClickType pClickType, Player pPlayer, CallbackInfo ci) {
        if (!pPlayer.level().isClientSide()) {
            WatutMod.getPlayerStatusManagerServer().doClickPre((AbstractContainerMenu)((Object)this), pSlotId, pButton, pClickType, pPlayer);
        }
    }

    @Inject(method = "doClick", at = @At("TAIL"))
    private void doClickPost(int pSlotId, int pButton, ClickType pClickType, Player pPlayer, CallbackInfo ci) {
        if (!pPlayer.level().isClientSide()) {
            WatutMod.getPlayerStatusManagerServer().doClickPost((AbstractContainerMenu)((Object)this), pSlotId, pButton, pClickType, pPlayer);
        }
    }
}