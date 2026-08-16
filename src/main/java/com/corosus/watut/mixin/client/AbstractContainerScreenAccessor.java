package com.corosus.watut.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("imageWidth")
    int getImageWidth();

    @Accessor("imageHeight")
    int getImageHeight();

    @Accessor("leftPos")
    int getLeftPos();

    @Accessor("topPos")
    int getTopPos();
}
