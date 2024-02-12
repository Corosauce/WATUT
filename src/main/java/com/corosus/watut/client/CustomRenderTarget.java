package com.corosus.watut.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.IntBuffer;
import java.util.List;
import java.util.Objects;

public class CustomRenderTarget extends RenderTarget {
    public static final int DEFAULT_WIDTH = 854;
    public static final int DEFAULT_HEIGHT = 480;
    static final CustomRenderTarget.Dimension DEFAULT_DIMENSIONS = new CustomRenderTarget.Dimension(854, 480);
    public CustomRenderTarget(int p_166137_, int p_166138_, boolean p_166199_) {
        super(p_166199_);
        RenderSystem.assertOnRenderThreadOrInit();
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                this.createFrameBuffer(p_166137_, p_166138_);
            });
        } else {
            this.createFrameBuffer(p_166137_, p_166138_);
        }
    }

    private void createFrameBuffer(int p_166142_, int p_166143_) {
        RenderSystem.assertOnRenderThreadOrInit();
        CustomRenderTarget.Dimension CustomRenderTarget$dimension = this.allocateAttachments(p_166142_, p_166143_);
        this.frameBufferId = GlStateManager.glGenFramebuffers();
        GlStateManager._glBindFramebuffer(36160, this.frameBufferId);
        GlStateManager._bindTexture(this.colorTextureId);
        GlStateManager._texParameter(3553, 10241, 9728);
        GlStateManager._texParameter(3553, 10240, 9728);
        GlStateManager._texParameter(3553, 10242, 33071);
        GlStateManager._texParameter(3553, 10243, 33071);
        GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.colorTextureId, 0);
        GlStateManager._bindTexture(this.depthBufferId);
        GlStateManager._texParameter(3553, 34892, 0);
        GlStateManager._texParameter(3553, 10241, 9728);
        GlStateManager._texParameter(3553, 10240, 9728);
        GlStateManager._texParameter(3553, 10242, 33071);
        GlStateManager._texParameter(3553, 10243, 33071);
        GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthBufferId, 0);
        GlStateManager._bindTexture(0);
        this.viewWidth = CustomRenderTarget$dimension.width;
        this.viewHeight = CustomRenderTarget$dimension.height;
        this.width = CustomRenderTarget$dimension.width;
        this.height = CustomRenderTarget$dimension.height;
        this.checkStatus();
        GlStateManager._glBindFramebuffer(36160, 0);
    }

    private CustomRenderTarget.Dimension allocateAttachments(int p_166147_, int p_166148_) {
        RenderSystem.assertOnRenderThreadOrInit();
        this.colorTextureId = TextureUtil.generateTextureId();
        this.depthBufferId = TextureUtil.generateTextureId();
        CustomRenderTarget.AttachmentState CustomRenderTarget$attachmentstate = CustomRenderTarget.AttachmentState.NONE;

        for(CustomRenderTarget.Dimension CustomRenderTarget$dimension : CustomRenderTarget.Dimension.listWithFallback(p_166147_, p_166148_)) {
            CustomRenderTarget$attachmentstate = CustomRenderTarget.AttachmentState.NONE;
            if (this.allocateColorAttachment(CustomRenderTarget$dimension)) {
                CustomRenderTarget$attachmentstate = CustomRenderTarget$attachmentstate.with(CustomRenderTarget.AttachmentState.COLOR);
            }

            if (this.allocateDepthAttachment(CustomRenderTarget$dimension)) {
                CustomRenderTarget$attachmentstate = CustomRenderTarget$attachmentstate.with(CustomRenderTarget.AttachmentState.DEPTH);
            }

            if (CustomRenderTarget$attachmentstate == CustomRenderTarget.AttachmentState.COLOR_DEPTH) {
                return CustomRenderTarget$dimension;
            }
        }

        throw new RuntimeException("Unrecoverable GL_OUT_OF_MEMORY (allocated attachments = " + CustomRenderTarget$attachmentstate.name() + ")");
    }

    private boolean allocateColorAttachment(CustomRenderTarget.Dimension p_166140_) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._getError();
        GlStateManager._bindTexture(this.colorTextureId);
        GlStateManager._texImage2D(3553, 0, 32856, p_166140_.width, p_166140_.height, 0, 6408, 5121, (IntBuffer)null);
        return GlStateManager._getError() != 1285;
    }

    private boolean allocateDepthAttachment(CustomRenderTarget.Dimension p_166145_) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._getError();
        GlStateManager._bindTexture(this.depthBufferId);
        GlStateManager._texImage2D(3553, 0, 6402, p_166145_.width, p_166145_.height, 0, 6402, 5126, (IntBuffer)null);
        return GlStateManager._getError() != 1285;
    }

    static enum AttachmentState {
        NONE,
        COLOR,
        DEPTH,
        COLOR_DEPTH;

        private static final CustomRenderTarget.AttachmentState[] VALUES = values();

        CustomRenderTarget.AttachmentState with(CustomRenderTarget.AttachmentState p_166164_) {
            return VALUES[this.ordinal() | p_166164_.ordinal()];
        }
    }

    static class Dimension {
        public final int width;
        public final int height;

        Dimension(int p_166171_, int p_166172_) {
            this.width = p_166171_;
            this.height = p_166172_;
        }

        static List<CustomRenderTarget.Dimension> listWithFallback(int p_166174_, int p_166175_) {
            RenderSystem.assertOnRenderThreadOrInit();
            int i = RenderSystem.maxSupportedTextureSize();
            return p_166174_ > 0 && p_166174_ <= i && p_166175_ > 0 && p_166175_ <= i ? ImmutableList.of(new CustomRenderTarget.Dimension(p_166174_, p_166175_), CustomRenderTarget.DEFAULT_DIMENSIONS) : ImmutableList.of(CustomRenderTarget.DEFAULT_DIMENSIONS);
        }

        public boolean equals(Object p_166177_) {
            if (this == p_166177_) {
                return true;
            } else if (p_166177_ != null && this.getClass() == p_166177_.getClass()) {
                CustomRenderTarget.Dimension CustomRenderTarget$dimension = (CustomRenderTarget.Dimension)p_166177_;
                return this.width == CustomRenderTarget$dimension.width && this.height == CustomRenderTarget$dimension.height;
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(this.width, this.height);
        }

        public String toString() {
            return this.width + "x" + this.height;
        }
    }
}
