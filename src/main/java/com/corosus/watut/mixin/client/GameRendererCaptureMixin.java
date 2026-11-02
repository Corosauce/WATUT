package com.corosus.watut.mixin.client;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.client.screen.RenderHelper;
import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererCaptureMixin {

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V",
            shift = At.Shift.AFTER
        ),
        require = 1
    )
    private void watut$afterGuiRender(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (!RenderHelper.captureRequested) return;
        RenderHelper.captureRequested = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return;
        Screen screen = mc.gui.screen();
        if (screen == null) return;

        GameRenderer self = (GameRenderer) (Object) this;
        GameRendererAccessor accessor = (GameRendererAccessor) self;
        GuiRenderer guiRenderer = accessor.watut_getGuiRenderer();
        GuiRenderState guiRenderState = self.gameRenderState().guiRenderState;

        ScreenParticleRenderer spr = ScreenParticleRenderer.getInstance();
        spr.checkSetup();

        RenderHelper.performingOwnRender = true;
        ScreenParticleRenderer.isRenderingParticleGUI2 = true;

        RenderTarget savedRT = accessor.watut_getMainRenderTarget();
        try {
            int mouseX = RenderHelper.captureMouseX;
            int mouseY = RenderHelper.captureMouseY;
            GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, guiRenderState, mouseX, mouseY);
            screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, deltaTracker.getGameTimeDeltaTicks());

            accessor.watut_setMainRenderTarget(spr.getMainRenderTarget());
            spr.clearMainRenderTarget();
            guiRenderer.render();
        } catch (Exception e) {
            CULog.err("WATUT: secondary gui render pass failed: " + e);
            e.printStackTrace();
            return;
        } finally {
            accessor.watut_setMainRenderTarget(savedRT);
            ScreenParticleRenderer.isRenderingParticleGUI2 = false;
            RenderHelper.performingOwnRender = false;
        }

        spr.performCaptureBlurAndReadback(RenderHelper.captureMouseX, RenderHelper.captureMouseY);
    }
}
