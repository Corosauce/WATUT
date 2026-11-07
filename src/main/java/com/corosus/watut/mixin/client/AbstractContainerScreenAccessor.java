package com.corosus.watut.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int watut_getLeftPos();

    @Accessor("topPos")
    int watut_getTopPos();

    @Accessor("imageWidth")
    int watut_getImageWidth();

    @Accessor("imageHeight")
    int watut_getImageHeight();
}
