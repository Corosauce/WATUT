package com.corosus.watut.loader.forge;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.SpriteInfo;
import com.corosus.watut.WatutMod;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SpriteSourceProvider;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class ParticleDataGen extends SpriteSourceProvider {

    public ParticleDataGen(PackOutput output, ExistingFileHelper fileHelper)
    {
        super(output, fileHelper, WatutMod.MODID);
    }

    @Override
    protected void addSources()
    {
        for (SpriteInfo info : ParticleRegistry.particles) {
            registerSprites(info, this);
        }
    }

    public void addSprite(ResourceLocation res) {
        atlas(SpriteSourceProvider.PARTICLES_ATLAS).addSource(new SingleFile(res, Optional.empty()));
    }

    public void registerSprites(SpriteInfo info, ParticleDataGen registry) {
        if (info.isSpriteSet()) {
            for (int i = 0; i < info.getSpriteSet().getFrames(); i++) {
                registry.addSprite(info.getResLocationName(i));
            }
        } else {
            registry.addSprite(info.getResLocationName());
        }
    }

}
