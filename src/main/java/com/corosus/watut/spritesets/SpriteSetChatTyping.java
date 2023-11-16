package com.corosus.watut.spritesets;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Random;

public class SpriteSetChatTyping implements SpriteSet {

    public List<TextureAtlasSprite> list;

    public SpriteSetChatTyping(List<TextureAtlasSprite> list) {
        this.list = list;
    }

    @Override
    public TextureAtlasSprite get(int pAge, int pLifetime) {
        Random rand = new Random();
        int index = rand.nextInt(2);
        int tickDelay = 2;
        int frames = 6;
        index = (pAge / tickDelay) % frames;
        //System.out.println(index);
        if (index < list.size()) {
            return list.get(index);
        } else {
            return list.get(0);
        }

    }

    @Override
    public TextureAtlasSprite get(RandomSource pRandom) {
        return list.get(0);
    }
}
