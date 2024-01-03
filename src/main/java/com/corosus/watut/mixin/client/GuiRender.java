package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IngameGui.class)
public abstract class GuiRender {

    @Inject(method = "render", at = @At("TAIL"))
    private void render(MatrixStack p_93031_, float p_93032_, CallbackInfo info) {
        WatutMod.getPlayerStatusManagerClient().onGuiRender();
    }
}