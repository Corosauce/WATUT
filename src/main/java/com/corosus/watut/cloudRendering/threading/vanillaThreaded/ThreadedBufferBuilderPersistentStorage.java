package com.corosus.watut.cloudRendering.threading.vanillaThreaded;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.ARBBufferStorage.GL_MAP_COHERENT_BIT;
import static org.lwjgl.opengl.ARBBufferStorage.GL_MAP_PERSISTENT_BIT;
import static org.lwjgl.opengl.ARBMapBufferRange.GL_MAP_WRITE_BIT;

@OnlyIn(Dist.CLIENT)
public class ThreadedBufferBuilderPersistentStorage extends DefaultedVertexConsumer implements BufferVertexConsumer {
   private static final int GROWTH_SIZE = 2097152;
   private static final Logger LOGGER = LogUtils.getLogger();
   private ByteBuffer buffer;
   //private ByteBuffer bufferInternal;
   private int renderedBufferCount;
   private int renderedBufferPointer;
   private int nextElementByte;
   private int vertices;
   @Nullable
   private VertexFormatElement currentElement;
   private int elementIndex;
   private VertexFormat format;
   private VertexFormat.Mode mode;
   private boolean fastFormat;
   private boolean fullFormat;
   private boolean building;
   @Nullable
   private Vector3f[] sortingPoints;
   @Nullable
   private VertexSorting sorting;
   private boolean indexOnly;
   private int lastNextElementByte;

   public ThreadedBufferBuilderPersistentStorage(int p_85664_) {
      //this.buffer = MemoryTracker.create(p_85664_ * 6);
      //this.bufferInternal = BufferUtils.createByteBuffer(p_85664_ * 6);
      //this.buffer = BufferUtils.createByteBuffer(p_85664_ * 6);
      ByteBuffer buffer = BufferUtils.createByteBuffer(p_85664_ * 6);
      //ByteBuffer buffer = MemoryTracker.create(p_85664_ * 6);
      /*ARBBufferStorage.glBufferStorage(GL33.GL_ARRAY_BUFFER, buffer, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
      this.buffer = GL33.glMapBufferRange(GL33.GL_ARRAY_BUFFER, 0, p_85664_ * 6, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);*/

      ARBBufferStorage.glBufferStorage(GL33.GL_ARRAY_BUFFER, buffer, GL33.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT);
      this.buffer = GL33.glMapBufferRange(GL33.GL_ARRAY_BUFFER, 0, p_85664_ * 6, GL33.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT);
   }

   private void ensureVertexCapacity() {
      this.ensureCapacity(this.format.getVertexSize());
   }

   private void ensureCapacity(int p_85723_) {
      if (this.nextElementByte + p_85723_ > this.buffer.capacity()) {
         //System.out.println((this.nextElementByte + p_85723_) + " vs cur " + this.buffer.capacity());
         int i = this.buffer.capacity();
         int j = i + roundUp(p_85723_);
         LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", i, j);
         LOGGER.debug("??? {}", p_85723_);
         ByteBuffer bytebuffer = MemoryTracker.resize(this.buffer, j);
         bytebuffer.rewind();
         this.buffer = bytebuffer;
      }
   }

   private static int roundUp(int p_85726_) {
      int i = 2097152;
      if (p_85726_ == 0) {
         return i;
      } else {
         if (p_85726_ < 0) {
            i *= -1;
         }

         int j = p_85726_ % i;
         return j == 0 ? p_85726_ : p_85726_ + i - j;
      }
   }

   public void setQuadSorting(VertexSorting p_277454_) {
      if (this.mode == VertexFormat.Mode.QUADS) {
         this.sorting = p_277454_;
         if (this.sortingPoints == null) {
            this.sortingPoints = this.makeQuadSortingPoints();
         }

      }
   }

   public ThreadedBufferBuilderPersistentStorage.SortState getSortState() {
      return new ThreadedBufferBuilderPersistentStorage.SortState(this.mode, this.vertices, this.sortingPoints, this.sorting);
   }

   public void restoreSortState(ThreadedBufferBuilderPersistentStorage.SortState p_166776_) {
      this.buffer.rewind();
      this.mode = p_166776_.mode;
      this.vertices = p_166776_.vertices;
      this.nextElementByte = this.renderedBufferPointer;
      this.sortingPoints = p_166776_.sortingPoints;
      this.sorting = p_166776_.sorting;
      this.indexOnly = true;
   }

   public void begin(VertexFormat.Mode p_166780_, VertexFormat p_166781_) {
      if (this.building) {
         throw new IllegalStateException("Already building!");
      } else {
         this.building = true;
         this.mode = p_166780_;
         this.switchFormat(p_166781_);
         this.currentElement = p_166781_.getElements().get(0);
         this.elementIndex = 0;
         this.buffer.rewind();
      }
   }

   private void switchFormat(VertexFormat p_85705_) {
      if (this.format != p_85705_) {
         this.format = p_85705_;
         boolean flag = p_85705_ == DefaultVertexFormat.NEW_ENTITY;
         boolean flag1 = p_85705_ == DefaultVertexFormat.BLOCK;
         this.fastFormat = flag || flag1;
         this.fullFormat = flag;
      }
   }

   private IntConsumer intConsumer(int p_231159_, VertexFormat.IndexType p_231160_) {
      MutableInt mutableint = new MutableInt(p_231159_);
      IntConsumer intconsumer;
      switch (p_231160_) {
         case SHORT:
            intconsumer = (p_231167_) -> {
               this.buffer.putShort(mutableint.getAndAdd(2), (short)p_231167_);
            };
            break;
         case INT:
            intconsumer = (p_231163_) -> {
               this.buffer.putInt(mutableint.getAndAdd(4), p_231163_);
            };
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return intconsumer;
   }

   private Vector3f[] makeQuadSortingPoints() {
      FloatBuffer floatbuffer = this.buffer.asFloatBuffer();
      int i = this.renderedBufferPointer / 4;
      int j = this.format.getIntegerSize();
      int k = j * this.mode.primitiveStride;
      int l = this.vertices / this.mode.primitiveStride;
      Vector3f[] avector3f = new Vector3f[l];

      for(int i1 = 0; i1 < l; ++i1) {
         float f = floatbuffer.get(i + i1 * k + 0);
         float f1 = floatbuffer.get(i + i1 * k + 1);
         float f2 = floatbuffer.get(i + i1 * k + 2);
         float f3 = floatbuffer.get(i + i1 * k + j * 2 + 0);
         float f4 = floatbuffer.get(i + i1 * k + j * 2 + 1);
         float f5 = floatbuffer.get(i + i1 * k + j * 2 + 2);
         float f6 = (f + f3) / 2.0F;
         float f7 = (f1 + f4) / 2.0F;
         float f8 = (f2 + f5) / 2.0F;
         avector3f[i1] = new Vector3f(f6, f7, f8);
      }

      return avector3f;
   }

   private void putSortedQuadIndices(VertexFormat.IndexType p_166787_) {
      if (this.sortingPoints != null && this.sorting != null) {
         int[] aint = this.sorting.sort(this.sortingPoints);
         IntConsumer intconsumer = this.intConsumer(this.nextElementByte, p_166787_);

         for(int i : aint) {
            intconsumer.accept(i * this.mode.primitiveStride + 0);
            intconsumer.accept(i * this.mode.primitiveStride + 1);
            intconsumer.accept(i * this.mode.primitiveStride + 2);
            intconsumer.accept(i * this.mode.primitiveStride + 2);
            intconsumer.accept(i * this.mode.primitiveStride + 3);
            intconsumer.accept(i * this.mode.primitiveStride + 0);
         }

      } else {
         throw new IllegalStateException("Sorting state uninitialized");
      }
   }

   public boolean isCurrentBatchEmpty() {
      return this.vertices == 0;
   }

   @Nullable
   public ThreadedBufferBuilderPersistentStorage.RenderedBuffer endOrDiscardIfEmpty() {
      this.ensureDrawing();
      if (this.isCurrentBatchEmpty()) {
         this.reset();
         return null;
      } else {
         ThreadedBufferBuilderPersistentStorage.RenderedBuffer bufferbuilder$renderedbuffer = this.storeRenderedBuffer();
         this.reset();
         return bufferbuilder$renderedbuffer;
      }
   }

   public ThreadedBufferBuilderPersistentStorage.RenderedBuffer end() {
      this.lastNextElementByte = this.nextElementByte;
      this.ensureDrawing();
      ThreadedBufferBuilderPersistentStorage.RenderedBuffer bufferbuilder$renderedbuffer = this.storeRenderedBuffer();
      this.reset();
      return bufferbuilder$renderedbuffer;
   }

   private void ensureDrawing() {
      if (!this.building) {
         throw new IllegalStateException("Not building!");
      }
   }

   private ThreadedBufferBuilderPersistentStorage.RenderedBuffer storeRenderedBuffer() {
      int i = this.mode.indexCount(this.vertices);
      int j = !this.indexOnly ? this.vertices * this.format.getVertexSize() : 0;
      VertexFormat.IndexType vertexformat$indextype = VertexFormat.IndexType.least(i);
      boolean flag;
      int k;
      if (this.sortingPoints != null) {
         int l = Mth.roundToward(i * vertexformat$indextype.bytes, 4);
         this.ensureCapacity(l);
         this.putSortedQuadIndices(vertexformat$indextype);
         flag = false;
         this.nextElementByte += l;
         k = j + l;
      } else {
         flag = true;
         k = j;
      }

      int i1 = this.renderedBufferPointer;
      this.renderedBufferPointer += k;
      ++this.renderedBufferCount;
      ThreadedBufferBuilderPersistentStorage.DrawState bufferbuilder$drawstate = new ThreadedBufferBuilderPersistentStorage.DrawState(this.format, this.vertices, i, this.mode, vertexformat$indextype, this.indexOnly, flag);
      return new ThreadedBufferBuilderPersistentStorage.RenderedBuffer(i1, bufferbuilder$drawstate);
   }

   private void reset() {
      this.building = false;
      this.vertices = 0;
      this.currentElement = null;
      this.elementIndex = 0;
      this.sortingPoints = null;
      this.sorting = null;
      this.indexOnly = false;
   }

   public void putByte(int p_85686_, byte p_85687_) {
      this.buffer.put(this.nextElementByte + p_85686_, p_85687_);
   }

   public void putShort(int p_85700_, short p_85701_) {
      this.buffer.putShort(this.nextElementByte + p_85700_, p_85701_);
   }

   public void putFloat(int p_85689_, float p_85690_) {
      this.buffer.putFloat(this.nextElementByte + p_85689_, p_85690_);
   }

   public void endVertex() {
      if (this.elementIndex != 0) {
         throw new IllegalStateException("Not filled all elements of the vertex");
      } else {
         ++this.vertices;
         this.ensureVertexCapacity();
         if (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP) {
            int i = this.format.getVertexSize();
            this.buffer.put(this.nextElementByte, this.buffer, this.nextElementByte - i, i);
            this.nextElementByte += i;
            ++this.vertices;
            this.ensureVertexCapacity();
         }

      }
   }

   public void nextElement() {
      ImmutableList<VertexFormatElement> immutablelist = this.format.getElements();
      this.elementIndex = (this.elementIndex + 1) % immutablelist.size();
      this.nextElementByte += this.currentElement.getByteSize();
      VertexFormatElement vertexformatelement = immutablelist.get(this.elementIndex);
      this.currentElement = vertexformatelement;
      if (vertexformatelement.getUsage() == VertexFormatElement.Usage.PADDING) {
         this.nextElement();
      }

      if (this.defaultColorSet && this.currentElement.getUsage() == VertexFormatElement.Usage.COLOR) {
         BufferVertexConsumer.super.color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
      }

   }

   public VertexConsumer color(int p_85692_, int p_85693_, int p_85694_, int p_85695_) {
      if (this.defaultColorSet) {
         throw new IllegalStateException();
      } else {
         return BufferVertexConsumer.super.color(p_85692_, p_85693_, p_85694_, p_85695_);
      }
   }

   public void vertex(float p_85671_, float p_85672_, float p_85673_, float p_85674_, float p_85675_, float p_85676_, float p_85677_, float p_85678_, float p_85679_, int p_85680_, int p_85681_, float p_85682_, float p_85683_, float p_85684_) {
      if (this.defaultColorSet) {
         throw new IllegalStateException();
      } else if (this.fastFormat) {
         this.putFloat(0, p_85671_);
         this.putFloat(4, p_85672_);
         this.putFloat(8, p_85673_);
         this.putByte(12, (byte)((int)(p_85674_ * 255.0F)));
         this.putByte(13, (byte)((int)(p_85675_ * 255.0F)));
         this.putByte(14, (byte)((int)(p_85676_ * 255.0F)));
         this.putByte(15, (byte)((int)(p_85677_ * 255.0F)));
         this.putFloat(16, p_85678_);
         this.putFloat(20, p_85679_);
         int i;
         if (this.fullFormat) {
            this.putShort(24, (short)(p_85680_ & '\uffff'));
            this.putShort(26, (short)(p_85680_ >> 16 & '\uffff'));
            i = 28;
         } else {
            i = 24;
         }

         this.putShort(i + 0, (short)(p_85681_ & '\uffff'));
         this.putShort(i + 2, (short)(p_85681_ >> 16 & '\uffff'));
         this.putByte(i + 4, BufferVertexConsumer.normalIntValue(p_85682_));
         this.putByte(i + 5, BufferVertexConsumer.normalIntValue(p_85683_));
         this.putByte(i + 6, BufferVertexConsumer.normalIntValue(p_85684_));
         this.nextElementByte += i + 8;
         this.endVertex();
      } else {
         super.vertex(p_85671_, p_85672_, p_85673_, p_85674_, p_85675_, p_85676_, p_85677_, p_85678_, p_85679_, p_85680_, p_85681_, p_85682_, p_85683_, p_85684_);
      }
   }

   void releaseRenderedBuffer() {
      if (this.renderedBufferCount > 0 && --this.renderedBufferCount == 0) {
         this.clear();
      }

   }

   public void clear() {
      if (this.renderedBufferCount > 0) {
         LOGGER.warn("Clearing BufferBuilder with unused batches");
      }

      this.discard();
   }

   public void discard() {
      this.renderedBufferCount = 0;
      this.renderedBufferPointer = 0;
      this.nextElementByte = 0;
   }

   public VertexFormatElement currentElement() {
      if (this.currentElement == null) {
         throw new IllegalStateException("BufferBuilder not started");
      } else {
         return this.currentElement;
      }
   }

   public boolean building() {
      return this.building;
   }

   ByteBuffer bufferSlice(int p_231170_, int p_231171_) {
      return MemoryUtil.memSlice(this.buffer, p_231170_, p_231171_ - p_231170_);
   }

   @OnlyIn(Dist.CLIENT)
   public static record DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType, boolean indexOnly, boolean sequentialIndex) {
      public int vertexBufferSize() {
         return this.vertexCount * this.format.getVertexSize();
      }

      public int vertexBufferStart() {
         return 0;
      }

      public int vertexBufferEnd() {
         return this.vertexBufferSize();
      }

      public int indexBufferStart() {
         return this.indexOnly ? 0 : this.vertexBufferEnd();
      }

      public int indexBufferEnd() {
         return this.indexBufferStart() + this.indexBufferSize();
      }

      private int indexBufferSize() {
         return this.sequentialIndex ? 0 : this.indexCount * this.indexType.bytes;
      }

      public int bufferSize() {
         return this.indexBufferEnd();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class RenderedBuffer {
      private final int pointer;
      private final ThreadedBufferBuilderPersistentStorage.DrawState drawState;
      private boolean released;

      RenderedBuffer(int p_231194_, ThreadedBufferBuilderPersistentStorage.DrawState p_231195_) {
         this.pointer = p_231194_;
         this.drawState = p_231195_;
      }

      public ByteBuffer vertexBuffer() {
         return ThreadedBufferBuilderPersistentStorage.this.buffer;
      }

      public ThreadedBufferBuilderPersistentStorage.DrawState drawState() {
         return this.drawState;
      }

      public boolean isEmpty() {
         return this.drawState.vertexCount == 0;
      }

      public void release() {
         if (this.released) {
            throw new IllegalStateException("Buffer has already been released!");
         } else {
            ThreadedBufferBuilderPersistentStorage.this.releaseRenderedBuffer();
            this.released = true;
         }
      }

      public boolean isReleased() {
         return released;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SortState {
      final VertexFormat.Mode mode;
      final int vertices;
      @Nullable
      final Vector3f[] sortingPoints;
      @Nullable
      final VertexSorting sorting;

      SortState(VertexFormat.Mode p_278011_, int p_277510_, @Nullable Vector3f[] p_278102_, @Nullable VertexSorting p_277855_) {
         this.mode = p_278011_;
         this.vertices = p_277510_;
         this.sortingPoints = p_278102_;
         this.sorting = p_277855_;
      }
   }

   // Forge start
   public void putBulkData(ByteBuffer buffer) {
      ensureCapacity(buffer.limit() + this.format.getVertexSize());
      this.buffer.position(this.nextElementByte);
      this.buffer.put(buffer);
      this.buffer.position(0);
      this.vertices += buffer.limit() / this.format.getVertexSize();
      this.nextElementByte += buffer.limit();
   }

   public ByteBuffer getBuffer() {
      return buffer;
   }

   public int getLastNextElementByte() {
      return lastNextElementByte;
   }

   public int getRenderedBufferCount() {
      return renderedBufferCount;
   }
}
