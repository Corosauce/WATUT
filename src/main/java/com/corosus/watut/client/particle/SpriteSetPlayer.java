package com.corosus.watut.client.particle;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * SpriteSet per le particelle animate o LoD di WATUT.
 */
public class SpriteSetPlayer implements SpriteSet {

    private final int tickDelay;
    private final int frames;
    private List<TextureAtlasSprite> list;

    public SpriteSetPlayer(int tickDelay, int frames) {
        this.tickDelay = tickDelay;
        this.frames = frames;
    }

    @Override
    public TextureAtlasSprite get(int pAge, int pLifetime) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (tickDelay <= 0 || frames <= 0) {
            return list.get(0);
        }

        int index = (pAge / tickDelay) % frames;
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return list.get(0);
    }

    @Override
    public TextureAtlasSprite get(RandomSource pRandom) {
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public TextureAtlasSprite first() {
        return list != null && !list.isEmpty() ? list.get(0) : null;
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
