package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Filter out extra dark translucent backgrounds
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsCaptureBackgroundFilter {

    @Unique
    private static final Set<Identifier> watut$backdropTextures = Set.of(
            Identifier.withDefaultNamespace("textures/gui/menu_background.png"),
            Identifier.withDefaultNamespace("textures/gui/inworld_menu_background.png"),
            Identifier.withDefaultNamespace("textures/gui/menu_list_background.png"),
            Identifier.withDefaultNamespace("textures/gui/inworld_menu_list_background.png"),
            Identifier.withDefaultNamespace("textures/gui/header_separator.png"),
            Identifier.withDefaultNamespace("textures/gui/footer_separator.png"),
            Identifier.withDefaultNamespace("textures/gui/inworld_header_separator.png"),
            Identifier.withDefaultNamespace("textures/gui/inworld_footer_separator.png")
    );

    @Unique
    private static final int watut$maxBackdropChannel = 0x30;
    @Unique
    private static final int watut$maxBackdropAlpha = 0xF0;
    @Unique
    private static final float watut$minBackdropScreenFraction = 0.2F;

    @Inject(method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void watut$skipBackdropTexture(RenderPipeline renderPipeline, Identifier location, int x0, int x1, int y0, int y1,
                                           float u0, float u1, float v0, float v1, int color, CallbackInfo ci) {
        if (watut$filtering() && watut$backdropTextures.contains(location)) {
            ci.cancel();
        }
    }

    @Inject(method = "innerFill", at = @At("HEAD"), cancellable = true, require = 1)
    private void watut$skipBackdropFill(RenderPipeline renderPipeline, TextureSetup textureSetup, int x0, int y0, int x1, int y1,
                                        int color1, @Nullable Integer color2, CallbackInfo ci) {
        if (!watut$filtering()) return;
        if (!watut$isBackdropColor(color1) || (color2 != null && !watut$isBackdropColor(color2))) return;

        Minecraft mc = Minecraft.getInstance();
        float screenArea = (float) mc.getWindow().getGuiScaledWidth() * mc.getWindow().getGuiScaledHeight();
        float fillArea = (float) Math.abs(x1 - x0) * Math.abs(y1 - y0);
        if (screenArea > 0 && fillArea / screenArea >= watut$minBackdropScreenFraction) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean watut$filtering() {
        return ScreenParticleRenderer.isRenderingParticleGUI2 && ConfigServerControlledSyncedToClient.dynamicGuiDisableBackgroundRendering;
    }

    @Unique
    private static boolean watut$isBackdropColor(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        return alpha > 0 && alpha < watut$maxBackdropAlpha
                && red <= watut$maxBackdropChannel && green <= watut$maxBackdropChannel && blue <= watut$maxBackdropChannel;
    }
}
