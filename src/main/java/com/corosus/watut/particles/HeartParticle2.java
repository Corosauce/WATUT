package com.corosus.watut.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HeartParticle2 extends SpriteTexturedParticle {
   public HeartParticle2(ClientWorld world, double x, double y, double z) {
      super(world, x, y, z, 0.0D, 0.0D, 0.0D);
      this.motionX *= (double)0.01F;
      this.motionY *= (double)0.01F;
      this.motionZ *= (double)0.01F;
      this.motionY += 0.1D;
      this.particleScale *= 1.5F;
      this.maxAge = 16;
      this.canCollide = false;
   }

   public IParticleRenderType getRenderType() {
      return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public float getScale(float scaleFactor) {
      return this.particleScale * MathHelper.clamp(((float)this.age + scaleFactor) / (float)this.maxAge * 32.0F, 0.0F, 1.0F);
   }

   public void setMotion(float x, float y, float z) {
      motionX = x;
      motionY = y;
      motionZ = z;
   }

   public void tick() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.age++ >= this.maxAge) {
         this.setExpired();
      } else {
         this.move(this.motionX, this.motionY, this.motionZ);
         if (this.posY == this.prevPosY) {
            this.motionX *= 1.1D;
            this.motionZ *= 1.1D;
         }

         this.motionX *= (double)0.86F;
         this.motionY *= (double)0.86F;
         this.motionZ *= (double)0.86F;
         if (this.onGround) {
            this.motionX *= (double)0.7F;
            this.motionZ *= (double)0.7F;
         }

      }
   }

   @Override
   public void renderParticle(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
      //super.renderParticle(buffer, renderInfo, partialTicks);
      Vector3d vector3d = renderInfo.getProjectedView();
      float f = (float)(MathHelper.lerp((double)partialTicks, this.prevPosX, this.posX) - vector3d.getX());
      float f1 = (float)(MathHelper.lerp((double)partialTicks, this.prevPosY, this.posY) - vector3d.getY());
      float f2 = (float)(MathHelper.lerp((double)partialTicks, this.prevPosZ, this.posZ) - vector3d.getZ());
      Quaternion quaternion;
      if (this.particleAngle == 0.0F) {
         quaternion = renderInfo.getRotation();
      } else {
         quaternion = new Quaternion(renderInfo.getRotation());
         //quaternion = new Quaternion(0, 0, 0, 1);
         float f3 = MathHelper.lerp(partialTicks, this.prevParticleAngle, this.particleAngle);
         quaternion.multiply(Vector3f.ZP.rotation(f3));
         //quaternion.multiply(Vector3f.YP.rotation(f3));
      }

      Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
      vector3f1.transform(quaternion);
      Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
      float f4 = this.getScale(partialTicks);

      for(int i = 0; i < 4; ++i) {
         Vector3f vector3f = avector3f[i];
         vector3f.transform(quaternion);
         vector3f.mul(f4);
         vector3f.add(f, f1, f2);
      }

      float f7 = this.getMinU();
      float f8 = this.getMaxU();
      float f5 = this.getMinV();
      float f6 = this.getMaxV();
      int j = this.getBrightnessForRender(partialTicks);
      buffer.pos((double)avector3f[0].getX(), (double)avector3f[0].getY(), (double)avector3f[0].getZ()).tex(f8, f6).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j).endVertex();
      buffer.pos((double)avector3f[1].getX(), (double)avector3f[1].getY(), (double)avector3f[1].getZ()).tex(f8, f5).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j).endVertex();
      buffer.pos((double)avector3f[2].getX(), (double)avector3f[2].getY(), (double)avector3f[2].getZ()).tex(f7, f5).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j).endVertex();
      buffer.pos((double)avector3f[3].getX(), (double)avector3f[3].getY(), (double)avector3f[3].getZ()).tex(f7, f6).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).lightmap(j).endVertex();
   }

   @Override
   public void setSprite(TextureAtlasSprite sprite) {
      super.setSprite(sprite);
   }

   @Override
   public void setSize(float particleWidth, float particleHeight) {
      super.setSize(particleWidth, particleHeight);
   }

   public void setScale(float scale) {
      this.particleScale = scale;
   }

   @OnlyIn(Dist.CLIENT)
   public static class AngryVillagerFactory implements IParticleFactory<BasicParticleType> {
      private final IAnimatedSprite spriteSet;

      public AngryVillagerFactory(IAnimatedSprite spriteSet) {
         this.spriteSet = spriteSet;
      }

      public Particle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         HeartParticle2 heartparticle = new HeartParticle2(worldIn, x, y + 0.5D, z);
         heartparticle.selectSpriteRandomly(this.spriteSet);
         heartparticle.setColor(1.0F, 1.0F, 1.0F);
         return heartparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Factory implements IParticleFactory<BasicParticleType> {
      private final IAnimatedSprite spriteSet;

      public Factory(IAnimatedSprite spriteSet) {
         this.spriteSet = spriteSet;
      }

      public Particle makeParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         HeartParticle2 heartparticle = new HeartParticle2(worldIn, x, y, z);
         heartparticle.selectSpriteRandomly(this.spriteSet);
         return heartparticle;
      }
   }

   public void setAngle(float angle) {
      this.particleAngle = angle;
   }
}
