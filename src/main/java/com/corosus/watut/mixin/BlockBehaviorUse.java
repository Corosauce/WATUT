package com.corosus.watut.mixin;

import com.corosus.watut.WatutMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviorUse {

    @Inject(method = "use", at = @At("HEAD"))
    private void use(Level p_60665_, Player p_60666_, InteractionHand p_60667_, BlockHitResult p_60668_, CallbackInfoReturnable<InteractionResult> cir) {
        if (!p_60665_.isClientSide()) {
            WatutMod.getPlayerStatusManagerServer().useBlock(p_60666_, p_60668_.getBlockPos());
        }
    }
}