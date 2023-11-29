package com.corosus.watut;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SpriteSourceProvider;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParticleRegistry extends SpriteSourceProvider {

    public static SpriteInfo inventory;
    public static SpriteInfo chest;
    public static SpriteInfo crafting;
    public static SpriteInfo chat_idle;
    public static SpriteInfo chat_typing;

    private static List<SpriteInfo> particles = new ArrayList<>();

    static {
        inventory = add("inventory_", 3, 0);
        chest = add("chest_", 3, 0);
        crafting = add("crafting_", 3, 0);
        chat_idle = add("chat_idle_", 2, 6);
        chat_typing = add("chat_typing_", 6, 2);
    }

    public static SpriteInfo add(String name) {
        return add(name, 0, 0);
    }

    public static SpriteInfo add(String name, int frames, int tickDelay) {
        SpriteInfo spriteInfo = new SpriteInfo(name, frames, tickDelay);
        particles.add(spriteInfo);
        return spriteInfo;
    }

    public ParticleRegistry(PackOutput output, ExistingFileHelper fileHelper)
    {
        super(output, fileHelper, Watut.MODID);
    }

    @Override
    protected void addSources()
    {
        for (SpriteInfo info : particles) {
            info.registerSprites(this);
        }
    }

    public void addSprite(ResourceLocation res) {
        atlas(SpriteSourceProvider.PARTICLES_ATLAS).addSource(new SingleFile(res, Optional.empty()));
    }

    @SubscribeEvent
    public static void getRegisteredParticles(TextureStitchEvent.Post event) {

        if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)) return;

        for (SpriteInfo info : particles) {
            info.setupSprites(event.getAtlas());
        }
    }

}
