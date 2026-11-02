package com.corosus.watut.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class CustomParticleEngine {

    private final Set<Particle> tracked = Collections.newSetFromMap(new WeakHashMap<>());

    public void add(Particle particle) {
        tracked.add(particle);
        Minecraft.getInstance().particleEngine.add(particle);
    }

    public void tick() {
        tracked.removeIf(p -> !p.isAlive());
    }

    public void setLevel(ClientLevel level) {
        tracked.clear();
    }

    public String countParticles() {
        tracked.removeIf(p -> !p.isAlive());
        return String.valueOf(tracked.size());
    }
}
