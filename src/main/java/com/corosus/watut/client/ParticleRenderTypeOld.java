package com.corosus.watut.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.texture.TextureManager;

public interface ParticleRenderTypeOld {

    BufferBuilder begin(Tesselator var1, TextureManager var2);

    default boolean isTranslucent() {
        return true;
    }

}
