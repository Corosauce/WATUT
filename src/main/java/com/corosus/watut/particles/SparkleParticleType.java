package com.corosus.watut.particles;

import com.mojang.serialization.Codec;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.ParticleType;

import javax.annotation.Nonnull;

public class SparkleParticleType extends ParticleType {
	public SparkleParticleType() {
		super(false, null);
	}

	@Nonnull
	@Override
	public Codec func_230522_e_() {
		return null;
	}

	/*public static class Factory implements IParticleFactory {
		private final IAnimatedSprite sprite;

		public Factory(IAnimatedSprite sprite) {
			this.sprite = sprite;
		}

		@Override
		public Particle makeParticle(SparkleParticleData data, ClientWorld world, double x, double y, double z, double mx, double my, double mz) {
			return new FXSparkle(world, x, y, z, data.size, data.r, data.g, data.b, data.m, data.fake, data.noClip, data.corrupt, sprite);
		}
	}*/
}
