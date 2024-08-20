package com.corosus.watut.cloudRendering.threading.vanillaThreaded;

import com.corosus.coroutil.util.CULog;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33;

@OnlyIn(Dist.CLIENT)
public class ThreadedVertexBuffer implements AutoCloseable {
   private final ThreadedVertexBuffer.Usage usage;
   private int vertexBufferId;
   private int arrayObjectId;
   @Nullable
   private VertexFormat format;
   private int vertexCount;
   private VertexFormat.Mode mode;

   public ThreadedVertexBuffer(ThreadedVertexBuffer.Usage p_286252_) {
      this.usage = p_286252_;
      RenderSystem.assertOnRenderThread();
      this.vertexBufferId = GlStateManager._glGenBuffers();
      this.arrayObjectId = GlStateManager._glGenVertexArrays();
   }

   public void upload(ThreadedBufferBuilderPersistentStorage.RenderedBuffer p_231222_) {
      if (!this.isInvalid()) {
         RenderSystem.assertOnRenderThread();

         try {

            //testing what lines are needed by uncommenting them and seeing if mapped buffer clouds render
            //none of this is needed always, just has to run once,
            //so something here is needed to setup once for the actual rendering code

            ThreadedBufferBuilderPersistentStorage.DrawState bufferbuilder$drawstate = p_231222_.drawState();
            //needed - note i disabled the bufferdata within it
            //eventually does this from VertexFormatElement.POSITION:
            //GlStateManager._enableVertexAttribArray(p_167048_);
            //GlStateManager._vertexAttribPointer(p_167048_, p_167043_, p_167044_, false, p_167045_, p_167046_);
            //which is same thing the opengl test code does, but for render code, here were in vbo building context
            //confirmed it only has to be done once for vertexAttribPointer
            this.format = this.uploadVertexBuffer(bufferbuilder$drawstate, p_231222_.vertexBuffer());
            //this.format = bufferbuilder$drawstate.format();
            //needed - disabled bufferdata
            //this.sequentialIndices = this.uploadIndexBuffer(bufferbuilder$drawstate, p_231222_.indexBuffer());
            //needed
            //this.indexCount = bufferbuilder$drawstate.indexCount();
            this.vertexCount = bufferbuilder$drawstate.vertexCount();
            //not needed
            //this.indexType = bufferbuilder$drawstate.indexType();
            //needed
            this.mode = bufferbuilder$drawstate.mode();
         } finally {
            //not needed
            //p_231222_.release();
         }
      }
   }

   private VertexFormat uploadVertexBuffer(ThreadedBufferBuilderPersistentStorage.DrawState p_231219_, ByteBuffer p_231220_) {
      boolean flag = false;
      if (!p_231219_.format().equals(this.format)) {
         if (this.format != null) {
            this.format.clearBufferState();
         }

         GlStateManager._glBindBuffer(34962, this.vertexBufferId);
         p_231219_.format().setupBufferState();
         flag = true;
      }

      if (!p_231219_.indexOnly()) {
         if (!flag) {
            //GlStateManager._glBindBuffer(34962, this.vertexBufferId);
         }

         //CULog.log("a");
         //RenderSystem.glBufferData(34962, p_231220_, this.usage.id);
         //CULog.log("b");
      }

      return p_231219_.format();
   }

   public void bind() {
      BufferUploader.invalidate();
      GlStateManager._glBindVertexArray(this.arrayObjectId);
   }

   public static void unbind() {
      BufferUploader.invalidate();
      GlStateManager._glBindVertexArray(0);
   }

   public void draw() {
      //RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.getIndexType().asGLType);
      //switching to this broke the visual of the render, lots of triangles missing / misplaced
      long time = System.currentTimeMillis();
      GL33.glDrawArrays(this.mode.asGLMode, 0, this.vertexCount);
      //CULog.log("render time " + (time - System.currentTimeMillis()));
   }

   public void drawWithShader(Matrix4f p_254480_, Matrix4f p_254555_, ShaderInstance p_253993_) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            this._drawWithShader(new Matrix4f(p_254480_), new Matrix4f(p_254555_), p_253993_);
         });
      } else {
         this._drawWithShader(p_254480_, p_254555_, p_253993_);
      }

   }

   private void _drawWithShader(Matrix4f p_253705_, Matrix4f p_253737_, ShaderInstance p_166879_) {
      for(int i = 0; i < 12; ++i) {
         int j = RenderSystem.getShaderTexture(i);
         p_166879_.setSampler("Sampler" + i, j);
      }

      if (p_166879_.MODEL_VIEW_MATRIX != null) {
         p_166879_.MODEL_VIEW_MATRIX.set(p_253705_);
      }

      if (p_166879_.PROJECTION_MATRIX != null) {
         p_166879_.PROJECTION_MATRIX.set(p_253737_);
      }

      if (p_166879_.INVERSE_VIEW_ROTATION_MATRIX != null) {
         p_166879_.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
      }

      if (p_166879_.COLOR_MODULATOR != null) {
         p_166879_.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
      }

      if (p_166879_.GLINT_ALPHA != null) {
         p_166879_.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
      }

      if (p_166879_.FOG_START != null) {
         p_166879_.FOG_START.set(RenderSystem.getShaderFogStart());
      }

      if (p_166879_.FOG_END != null) {
         p_166879_.FOG_END.set(RenderSystem.getShaderFogEnd());
      }

      if (p_166879_.FOG_COLOR != null) {
         p_166879_.FOG_COLOR.set(RenderSystem.getShaderFogColor());
      }

      if (p_166879_.FOG_SHAPE != null) {
         p_166879_.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
      }

      if (p_166879_.TEXTURE_MATRIX != null) {
         p_166879_.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
      }

      if (p_166879_.GAME_TIME != null) {
         p_166879_.GAME_TIME.set(RenderSystem.getShaderGameTime());
      }

      if (p_166879_.SCREEN_SIZE != null) {
         Window window = Minecraft.getInstance().getWindow();
         p_166879_.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
      }

      if (p_166879_.LINE_WIDTH != null && (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP)) {
         p_166879_.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
      }

      RenderSystem.setupShaderLights(p_166879_);
      p_166879_.apply();
      this.draw();
      p_166879_.clear();
   }

   public void close() {
      if (this.vertexBufferId >= 0) {
         RenderSystem.glDeleteBuffers(this.vertexBufferId);
         this.vertexBufferId = -1;
      }

      if (this.arrayObjectId >= 0) {
         RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
         this.arrayObjectId = -1;
      }

   }

   public VertexFormat getFormat() {
      return this.format;
   }

   public boolean isInvalid() {
      return this.arrayObjectId == -1;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Usage {
      STATIC(35044),
      DYNAMIC(35048);

      final int id;

      private Usage(int p_286680_) {
         this.id = p_286680_;
      }
   }

   public int getVertexBufferId() {
      return vertexBufferId;
   }
}