package com.corosus.watut;

import com.corosus.watut.particles.HeartParticle2;
import net.minecraft.client.Minecraft;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.event.RegistryEvent;

public class ModParticles {
	public static final ParticleType SPARKLE = new BasicParticleType(true);

	public void registerParticles(RegistryEvent.Register<ParticleType<?>> evt) {
		evt.getRegistry().register(SPARKLE);
		//register(evt.getRegistry(), "sparkle", SPARKLE);
	}

	public static class FactoryHandler {
		public static void registerFactories(ParticleFactoryRegisterEvent evt) {
			Minecraft.getInstance().particles.registerFactory(ModParticles.SPARKLE, HeartParticle2.Factory::new);
		}
	}
}
