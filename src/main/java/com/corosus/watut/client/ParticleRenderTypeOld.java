package com.corosus.watut.client;

import net.minecraft.resources.Identifier;

public interface ParticleRenderTypeOld {

    default boolean isTranslucent() {
        return true;
    }

    default Identifier getTextureId() {
        return null;
    }

}
