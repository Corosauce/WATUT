package com.corosus.watut.client;

import com.corosus.watut.particle.ParticleRotating;
import com.google.common.collect.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Reasons for having my own copy of ParticleEngine
 * - oculus makes a copy of the particlerender types via Sets.filter, suffers from performance issues with there is a large number of entries in the map
 * -- https://github.com/Asek3/Oculus/blob/1.20.1-new/src/main/java/net/irisshaders/iris/mixin/fantastic/MixinParticleEngine.java#L85-L88
 * - watut is adding a render type per player now, large servers could potentially suffer performance issues so rolling out my own ParticleEngine works around oculus's mixins on ParticleEngine that cause the performance issues
 * - we also dont use our particle shader when oculus shaders are on, we switch back to vanillas one, our doesnt work for some reason, we only lose our change that lets it render particles with alpha < 0.1, it is noticable though
 * -
 */
public class CustomParticleEngine implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particles");
   private static final ResourceLocation PARTICLES_ATLAS_INFO = ResourceLocation.parse("particles");
   private static final int MAX_PARTICLES_PER_LAYER = 16384;
   private static final List<ParticleRenderTypeOld> RENDER_ORDER = ImmutableList.of();
   protected ClientLevel level;
   private final Map<ParticleRenderTypeOld, Queue<ParticleRotating>> particles = Maps.newTreeMap(makeParticleRenderTypeOldComparator(RENDER_ORDER));
   private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
   private final TextureManager textureManager;
   private final RandomSource random = RandomSource.create();
   private final Map<ResourceLocation, ParticleProvider<?>> providers = new java.util.HashMap<>();
   private final Queue<ParticleRotating> particlesToAdd = Queues.newArrayDeque();
   private final Map<ResourceLocation, CustomParticleEngine.MutableSpriteSet> spriteSets = Maps.newHashMap();
   public final TextureAtlas textureAtlas;
   private final Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts = new Object2IntOpenHashMap<>();

   public CustomParticleEngine(ClientLevel p_107299_, TextureManager p_107300_) {
      this.textureAtlas = new TextureAtlas(TextureAtlas.LOCATION_PARTICLES);
      //p_107300_.register(this.textureAtlas.location(), this.textureAtlas);
      this.level = p_107299_;
      this.textureManager = p_107300_;
   }

   public static Comparator<ParticleRenderTypeOld> makeParticleRenderTypeOldComparator(List<ParticleRenderTypeOld> renderOrder)
   {
      Comparator<ParticleRenderTypeOld> vanillaComparator = Comparator.comparingInt(renderOrder::indexOf);
      return (typeOne, typeTwo) ->
      {
         boolean vanillaOne = renderOrder.contains(typeOne);
         boolean vanillaTwo = renderOrder.contains(typeTwo);

         if (vanillaOne && vanillaTwo)
         {
            return vanillaComparator.compare(typeOne, typeTwo);
         }
         if (!vanillaOne && !vanillaTwo)
         {
            return Integer.compare(System.identityHashCode(typeOne), System.identityHashCode(typeTwo));
         }
         return vanillaOne ? -1 : 1;
      };
   }

   @Override
   public CompletableFuture<Void> reload(
           PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor
   ) {
      record ParticleDefinition(ResourceLocation id, Optional<List<ResourceLocation>> sprites) {
      }

      CompletableFuture<List<ParticleDefinition>> completablefuture = CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(
                      () -> PARTICLE_LISTER.listMatchingResources(manager), backgroundExecutor
              )
              .thenCompose(
                      p_247914_ -> {
                         List<CompletableFuture<ParticleDefinition>> list = new ArrayList<>(p_247914_.size());
                         p_247914_.forEach(
                                 (p_247903_, p_247904_) -> {
                                    ResourceLocation resourcelocation = PARTICLE_LISTER.fileToId(p_247903_);
                                    list.add(
                                            CompletableFuture.supplyAsync(
                                                    () -> new ParticleDefinition(resourcelocation, this.loadParticleDescription(resourcelocation, p_247904_)), backgroundExecutor
                                            )
                                    );
                                 }
                         );
                         return Util.sequence(list);
                      }
              );
      CompletableFuture<SpriteLoader.Preparations> completablefuture1 = SpriteLoader.create(this.textureAtlas)
              .loadAndStitch(manager, PARTICLES_ATLAS_INFO, 0, backgroundExecutor)
              .thenCompose(SpriteLoader.Preparations::waitForUpload);
      return CompletableFuture.allOf(completablefuture1, completablefuture).thenCompose(barrier::wait).thenAcceptAsync(p_372548_ -> {
         this.clearParticles();
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push("upload");
         SpriteLoader.Preparations spriteloader$preparations = completablefuture1.join();
         this.textureAtlas.upload(spriteloader$preparations);
         profilerfiller.popPush("bindSpriteSets");
         Set<ResourceLocation> set = new HashSet<>();
         TextureAtlasSprite textureatlassprite = spriteloader$preparations.missing();
         completablefuture.join().forEach(p_247911_ -> {
            Optional<List<ResourceLocation>> optional = p_247911_.sprites();
            if (!optional.isEmpty()) {
               List<TextureAtlasSprite> list = new ArrayList<>();

               for (ResourceLocation resourcelocation : optional.get()) {
                  TextureAtlasSprite textureatlassprite1 = spriteloader$preparations.regions().get(resourcelocation);
                  if (textureatlassprite1 == null) {
                     set.add(resourcelocation);
                     list.add(textureatlassprite);
                  } else {
                     list.add(textureatlassprite1);
                  }
               }

               if (list.isEmpty()) {
                  list.add(textureatlassprite);
               }

               this.spriteSets.get(p_247911_.id()).rebind(list);
            }
         });
         if (!set.isEmpty()) {
            LOGGER.warn("Missing particle sprites: {}", set.stream().sorted().map(ResourceLocation::toString).collect(Collectors.joining(",")));
         }

         profilerfiller.pop();
      }, gameExecutor);
   }

   public void close() {
      this.textureAtlas.clearTextureData();
   }

   private Optional<List<ResourceLocation>> loadParticleDescription(ResourceLocation p_250648_, Resource p_248793_) {
      if (!this.spriteSets.containsKey(p_250648_)) {
         LOGGER.debug("Redundant texture list for particle: {}", (Object)p_250648_);
         return Optional.empty();
      } else {
         try (Reader reader = p_248793_.openAsReader()) {
            ParticleDescription particledescription = ParticleDescription.fromJson(GsonHelper.parse(reader));
            return Optional.of(particledescription.getTextures());
         } catch (IOException ioexception) {
            throw new IllegalStateException("Failed to load description for particle " + p_250648_, ioexception);
         }
      }
   }

   public void add(ParticleRotating p_107345_) {
      Optional<ParticleGroup> optional = p_107345_.getParticleGroup();
      if (optional.isPresent()) {
         if (this.hasSpaceInParticleLimit(optional.get())) {
            this.particlesToAdd.add(p_107345_);
            this.updateCount(optional.get(), 1);
         }
      } else {
         this.particlesToAdd.add(p_107345_);
      }

   }

   public void tick() {
      this.particles.forEach((p_288249_, p_288250_) -> {
         //this.level.getProfiler().push(p_288249_.toString());
         this.tickParticleList(p_288250_);
         //this.level.getProfiler().pop();
      });
      if (!this.trackingEmitters.isEmpty()) {
         List<TrackingEmitter> list = Lists.newArrayList();

         for(TrackingEmitter trackingemitter : this.trackingEmitters) {
            trackingemitter.tick();
            if (!trackingemitter.isAlive()) {
               list.add(trackingemitter);
            }
         }

         this.trackingEmitters.removeAll(list);
      }

      ParticleRotating particle;
      if (!this.particlesToAdd.isEmpty()) {
         while((particle = this.particlesToAdd.poll()) != null) {
            this.particles.computeIfAbsent(particle.getRenderTypeOld(), (p_107347_) -> {
               return EvictingQueue.create(16384);
            }).add(particle);
         }
      }

   }

   private void tickParticleList(Collection<ParticleRotating> p_107385_) {
      if (!p_107385_.isEmpty()) {
         Iterator<ParticleRotating> iterator = p_107385_.iterator();

         while(iterator.hasNext()) {
            Particle particle = iterator.next();
            this.tickParticle(particle);
            if (!particle.isAlive()) {
               particle.getParticleGroup().ifPresent((p_172289_) -> {
                  this.updateCount(p_172289_, -1);
               });
               iterator.remove();
            }
         }
      }

   }

   private void updateCount(ParticleGroup p_172282_, int p_172283_) {
      this.trackedParticleCounts.addTo(p_172282_, p_172283_);
   }

   private void tickParticle(Particle p_107394_) {
      try {
         p_107394_.tick();
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking Particle");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being ticked");
         crashreportcategory.setDetail("Particle", p_107394_::toString);
         crashreportcategory.setDetail("Particle Type", p_107394_.getRenderType()::toString);
         throw new ReportedException(crashreport);
      }
   }

   @Deprecated
   public void render(PoseStack p_107337_, MultiBufferSource.BufferSource p_107338_, LightTexture p_107339_, Camera p_107340_, float p_107341_) {
       render(p_107337_, p_107338_, p_107339_, p_107340_, p_107341_);
   }

   public void render(LightTexture lightTexture, Camera camera, float partialTick) {
      if (lightTexture != null) lightTexture.turnOnLightLayer();
      RenderSystem.enableDepthTest();
      //TODO porting: is this even needed with the particle render order fix???
      RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
      RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);

      /**
       * ParticleItem using special item/terrain pickup breaks particle render state so we just make sure to render it last
       * - related: see classic forge issue of the item pickup particle breaking depth testing and darkening particles
       * -- https://github.com/MinecraftForge/MinecraftForge/pull/8378/files
       * --- detailed breakdown: https://github.com/Asek3/Oculus/issues/149#issuecomment-1727945597
       * I cant precompute the render order because ParticleDynamic uses a new ParticleRenderTypeOld per player and I don't want to keep recomputing the render order.
       * This solution works enough and should avoid any performance overhead
       *
       * If I switch to using vanilla particle renderer, my ParticleItem renders for forge, but not for fabric, didn't dig into why.
       */
      this.render(lightTexture, camera, partialTick, false);
      this.render(lightTexture, camera, partialTick, true);

      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      if (lightTexture != null) lightTexture.turnOffLightLayer();
   }

   public void render(LightTexture lightTexture, Camera camera, float partialTick, boolean pickupParticleMode) {

      /**
       * ParticleItem using special item/terrain pickup breaks particle render state so we just make sure to render it last
       * - related: see classic forge issue of the item pickup particle breaking depth testing and darkening particles
       * -- https://github.com/MinecraftForge/MinecraftForge/pull/8378/files
       * --- detailed breakdown: https://github.com/Asek3/Oculus/issues/149#issuecomment-1727945597
       * I cant precompute the render order because ParticleDynamic uses a new ParticleRenderTypeOld per player and I don't want to keep recomputing the render order.
       * This solution works enough and should avoid any performance overhead
       *
       * If I switch to using vanilla particle renderer, my ParticleItem renders for forge, but not for fabric, didn't dig into why.
       */
      for (ParticleRenderTypeOld particlerendertype : this.particles.keySet()) { // Neo: allow custom IParticleRenderTypeOld's
         //if (particlerendertype == ParticleRenderTypeOld.NO_RENDER/* || !renderTypePredicate.test(particlerendertype)*/) continue;
         if (pickupParticleMode) {
            if (particlerendertype != ParticleRotating.TERRAIN_SHEET_TRANSLUCENT_NO_FACE_CULL) continue;
         } else {
            if (particlerendertype == ParticleRotating.TERRAIN_SHEET_TRANSLUCENT_NO_FACE_CULL) continue;
         }
         Queue<ParticleRotating> queue = this.particles.get(particlerendertype);
         if (queue != null && !queue.isEmpty()) {
            RenderSystem.setShader(CoreShaders.PARTICLE);
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = particlerendertype.begin(tesselator, this.textureManager);
            if (bufferbuilder != null) {
               for (Particle particle : queue) {
                  //if (frustum != null && !frustum.isVisible(particle.getRenderBoundingBox(partialTick))) continue;
                  try {
                     particle.render(bufferbuilder, camera, partialTick);
                  } catch (Throwable throwable) {
                     CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Particle");
                     CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being rendered");
                     crashreportcategory.setDetail("Particle", particle::toString);
                     crashreportcategory.setDetail("Particle Type", particlerendertype::toString);
                     throw new ReportedException(crashreport);
                  }
               }

               MeshData meshdata = bufferbuilder.build();
               if (meshdata != null) {
                  BufferUploader.drawWithShader(meshdata);
               }
            }
            RenderSystem.enableCull();
         }
      }
   }

   public void setLevel(ClientLevel p_107343_) {
      this.level = p_107343_;
      this.clearParticles();
      this.trackingEmitters.clear();
   }

   public String countParticles() {
      return String.valueOf(this.particles.values().stream().mapToInt(Collection::size).sum());
   }

   private boolean hasSpaceInParticleLimit(ParticleGroup p_172280_) {
      return this.trackedParticleCounts.getInt(p_172280_) < p_172280_.getLimit();
   }

   private void clearParticles() {
      this.particles.clear();
      this.particlesToAdd.clear();
      this.trackingEmitters.clear();
      this.trackedParticleCounts.clear();
   }

   static class MutableSpriteSet implements SpriteSet {
      private List<TextureAtlasSprite> sprites;

      public TextureAtlasSprite get(int p_107413_, int p_107414_) {
         return this.sprites.get(p_107413_ * (this.sprites.size() - 1) / p_107414_);
      }

      public TextureAtlasSprite get(RandomSource p_233889_) {
         return this.sprites.get(p_233889_.nextInt(this.sprites.size()));
      }

      public void rebind(List<TextureAtlasSprite> p_107416_) {
         this.sprites = ImmutableList.copyOf(p_107416_);
      }
   }

   @FunctionalInterface
   public interface SpriteParticleRegistration<T extends ParticleOptions> {
      ParticleProvider<T> create(SpriteSet p_107420_);
   }
}
