package com.corosus.watut.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("guiRenderer")
    GuiRenderer watut_getGuiRenderer();

    @Accessor("mainRenderTarget")
    RenderTarget watut_getMainRenderTarget();

    @Accessor("mainRenderTarget")
    @Mutable
    void watut_setMainRenderTarget(RenderTarget renderTarget);
}
