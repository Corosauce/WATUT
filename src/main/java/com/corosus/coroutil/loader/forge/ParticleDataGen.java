package com.corosus.coroutil.loader.forge;

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
            info.registerSprites(this);
        }
    }

    public void addSprite(ResourceLocation res) {
        atlas(SpriteSourceProvider.PARTICLES_ATLAS).addSource(new SingleFile(res, Optional.empty()));
    }

    @SubscribeEvent
    public static void getRegisteredParticles(TextureStitchEvent.Post event) {

        if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)) return;

        for (SpriteInfo info : ParticleRegistry.particles) {
            info.setupSprites(event.getAtlas());
        }
    }

}
