package com.corosus.watut;

import com.corosus.watut.spritesets.SpriteSetChatOpen;
import com.corosus.watut.spritesets.SpriteSetChatTyping;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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

    public static TextureAtlasSprite chat_idle_0;
    public static TextureAtlasSprite chat_idle_1;

    public static TextureAtlasSprite chat_typing_0;
    public static TextureAtlasSprite chat_typing_1;
    public static TextureAtlasSprite chat_typing_2;
    public static TextureAtlasSprite chat_typing_3;
    public static TextureAtlasSprite chat_typing_4;
    public static TextureAtlasSprite chat_typing_5;

    public static TextureAtlasSprite inventory;
    public static TextureAtlasSprite crafting;

    public static SpriteSet chat_idle_set;
    public static SpriteSet chat_typing_set;

    public ParticleRegistry(PackOutput output, ExistingFileHelper fileHelper)
    {
        super(output, fileHelper, Watut.MODID);
    }

    @Override
    protected void addSources()
    {
        addSprite(new ResourceLocation(Watut.MODID + ":particles/chat_idle_0"));
        addSprite(new ResourceLocation(Watut.MODID + ":particles/chat_idle_1"));

        addSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_0"));
        addSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_1"));
        addSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_2"));
        addSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_3"));
        addSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_4"));
        addSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_5"));

        addSprite(new ResourceLocation(Watut.MODID + ":particles/inventory"));
        addSprite(new ResourceLocation(Watut.MODID + ":particles/crafting"));
    }

    public void addSprite(ResourceLocation res) {
        atlas(SpriteSourceProvider.PARTICLES_ATLAS).addSource(new SingleFile(res, Optional.empty()));
    }

    @SubscribeEvent
    public static void getRegisteredParticles(TextureStitchEvent.Post event) {

        if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)) return;

        chat_idle_0 = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/chat_idle_0"));
        chat_idle_1 = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/chat_idle_1"));

        chat_typing_0 = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_0"));
        chat_typing_1 = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_1"));
        chat_typing_2 = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_2"));
        chat_typing_3 = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_3"));
        chat_typing_4 = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_4"));
        chat_typing_5 = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/chat_typing_5"));

        inventory = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/inventory"));
        crafting = event.getAtlas().getSprite(new ResourceLocation(Watut.MODID + ":particles/crafting"));

        List<TextureAtlasSprite> list = new ArrayList<>();
        list.add(chat_idle_0);
        list.add(chat_idle_1);
        chat_idle_set = new SpriteSetChatOpen(list);

        list = new ArrayList<>();
        list.add(chat_typing_0);
        list.add(chat_typing_1);
        list.add(chat_typing_2);
        list.add(chat_typing_3);
        list.add(chat_typing_4);
        list.add(chat_typing_5);
        chat_typing_set = new SpriteSetChatTyping(list);
    }

}
