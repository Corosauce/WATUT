package com.corosus.watut.mixin.client;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.client.screen.RenderHelper;
import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
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

        boolean dbg = RenderHelper.CAPTURE_DEBUG;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) {
            if (dbg) CULog.dbg("[watut-capture] aborted: mc.gui null");
            return;
        }
        Screen screen = mc.gui.screen();
        if (screen == null) {
            if (dbg) CULog.dbg("[watut-capture] aborted: no screen open");
            return;
        }

        GameRenderer self = (GameRenderer) (Object) this;
        GameRendererAccessor accessor = (GameRendererAccessor) self;
        GuiRenderer guiRenderer = accessor.watut_getGuiRenderer();
        GuiRenderState guiRenderState = self.gameRenderState().guiRenderState;

        ScreenParticleRenderer spr = ScreenParticleRenderer.getInstance();
        spr.checkSetup();

        RenderHelper.performingOwnRender = true;
        ScreenParticleRenderer.isRenderingParticleGUI2 = true;

        RenderTarget savedRT = accessor.watut_getMainRenderTarget();
        GpuTextureView savedOutputColorOverride = RenderSystem.outputColorTextureOverride;
        GpuTextureView savedOutputDepthOverride = RenderSystem.outputDepthTextureOverride;
        if (dbg) CULog.dbg("[watut-capture] BEGIN screen=" + screen.getClass().getName()
                + " sprTarget=" + spr.getMainRenderTarget().width + "x" + spr.getMainRenderTarget().height
                + " main=" + name(savedRT) + " ovColor=" + describeView(spr, savedOutputColorOverride)
                + " ovDepth=" + describeView(spr, savedOutputDepthOverride));
        try {
            int mouseX = RenderHelper.captureMouseX;
            int mouseY = RenderHelper.captureMouseY;
            GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, guiRenderState, mouseX, mouseY);

            accessor.watut_setMainRenderTarget(spr.getMainRenderTarget());
            RenderSystem.outputColorTextureOverride = spr.getMainRenderTarget().getColorTextureView();
            RenderSystem.outputDepthTextureOverride = spr.getMainRenderTarget().getDepthTextureView();
            spr.clearMainRenderTarget();

            if (dbg) CULog.dbg("[watut-capture] swapped: main=" + name(accessor.watut_getMainRenderTarget())
                    + " ovColor=" + describeView(spr, RenderSystem.outputColorTextureOverride)
                    + " ovDepth=" + describeView(spr, RenderSystem.outputDepthTextureOverride));

            screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, deltaTracker.getGameTimeDeltaTicks());

            if (dbg) CULog.dbg("[watut-capture] after extractRenderState: main=" + name(accessor.watut_getMainRenderTarget())
                    + " ovColor=" + describeView(spr, RenderSystem.outputColorTextureOverride)
                    + " ovDepth=" + describeView(spr, RenderSystem.outputDepthTextureOverride)
                    + " xaeroSentinel=" + RenderHelper.xaeroWorldMapTextureID);

            if (RenderHelper.isXaeroGuiMap(screen)) {
                RenderHelper.blitXaeroFboIntoCapture(spr.getMainRenderTarget());
            }

            guiRenderer.render();

            if (dbg) CULog.dbg("[watut-capture] after guiRenderer.render: main=" + name(accessor.watut_getMainRenderTarget())
                    + " ovColor=" + describeView(spr, RenderSystem.outputColorTextureOverride)
                    + " ovDepth=" + describeView(spr, RenderSystem.outputDepthTextureOverride));
        } catch (Exception e) {
            CULog.err("WATUT: secondary gui render pass failed: " + e);
            e.printStackTrace();
            return;
        } finally {
            RenderSystem.outputColorTextureOverride = savedOutputColorOverride;
            RenderSystem.outputDepthTextureOverride = savedOutputDepthOverride;
            accessor.watut_setMainRenderTarget(savedRT);
            ScreenParticleRenderer.isRenderingParticleGUI2 = false;
            RenderHelper.performingOwnRender = false;
            if (dbg) CULog.dbg("[watut-capture] END restored: main=" + name(accessor.watut_getMainRenderTarget())
                    + " ovColor=" + describeView(spr, RenderSystem.outputColorTextureOverride)
                    + " ovDepth=" + describeView(spr, RenderSystem.outputDepthTextureOverride));
        }

        spr.performCaptureBlurAndReadback(RenderHelper.captureMouseX, RenderHelper.captureMouseY);
    }

    private static String name(RenderTarget target) {
        return target == null ? "null" : target.getClass().getSimpleName() + "@"
                + Integer.toHexString(System.identityHashCode(target)) + "(" + target.width + "x" + target.height + ")";
    }

    private static String describeView(ScreenParticleRenderer spr, GpuTextureView view) {
        if (view == null) return "null";
        if (view == spr.getMainRenderTarget().getColorTextureView()) return "sprColor";
        if (view == spr.getMainRenderTarget().getDepthTextureView()) return "sprDepth";
        return view.getClass().getSimpleName();
    }
}
