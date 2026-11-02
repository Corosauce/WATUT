package com.corosus.watut.spritesets;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

import java.util.List;

public class SpriteSetPlayer implements SpriteSet {

    private int tickDelay;
    private int frames;
    private List<TextureAtlasSprite> list;

    public SpriteSetPlayer(int tickDelay, int frames) {
        this.tickDelay = tickDelay;
        this.frames = frames;
    }

    @Override
    public TextureAtlasSprite first() {
        if (list == null) return null;
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public TextureAtlasSprite get(int pAge, int pLifetime) {
        if (list == null || list.isEmpty()) return null;
        int index = 0;
        if (tickDelay > 0) {
            index = (pAge / tickDelay) % frames;
        }
        if (index < list.size()) {
            return list.get(index);
        } else {
            return list.get(0);
        }
    }

    public TextureAtlasSprite getFrame(int index) {
        if (list == null || list.isEmpty()) return null;
        return list.get(Math.max(0, Math.min(index, list.size() - 1)));
    }

    @Override
    public TextureAtlasSprite get(RandomSource pRandom) {
        if (list == null || list.isEmpty()) return null;
        return list.get(0);
    }

    public void setList(List<TextureAtlasSprite> list) {
        this.list = list;
    }

    public int getFrames() {
        return frames;
    }

    public List<TextureAtlasSprite> getList() {
        return list;
    }
}
