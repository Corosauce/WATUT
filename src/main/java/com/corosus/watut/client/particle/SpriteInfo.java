package com.corosus.watut.client.particle;

import com.corosus.watut.WatutMod;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Informazioni e caricamento per uno sprite o serie di sprite dall'atlas delle particelle.
 */
public class SpriteInfo {

    private final String name;
    private TextureAtlasSprite sprite;
    private SpriteSetPlayer spriteSetPlayer;

    public SpriteInfo(String name, int size, int tickDelay) {
        this.name = name;
        if (size > 0) {
            this.spriteSetPlayer = new SpriteSetPlayer(tickDelay, size);
        }
    }

    public boolean isSpriteSet() {
        return spriteSetPlayer != null;
    }

    public Identifier getResLocationName() {
        return getResLocationName(0);
    }

    public Identifier getResLocationName(int index) {
        if (isSpriteSet()) {
            return Identifier.parse(WatutMod.MODID + ":particles/" + name + index);
        } else {
            return Identifier.parse(WatutMod.MODID + ":particles/" + name);
        }
    }

    public void setupSprites(TextureAtlas textureAtlas) {
        if (isSpriteSet()) {
            List<TextureAtlasSprite> list = new ArrayList<>();
            for (int i = 0; i < spriteSetPlayer.getFrames(); i++) {
                TextureAtlasSprite s = textureAtlas.getSprite(getResLocationName(i));
                if (s != null) {
                    list.add(s);
                }
            }
            this.spriteSetPlayer.setList(list);
            if (!list.isEmpty()) {
                sprite = list.get(0);
            }
        } else {
            sprite = textureAtlas.getSprite(getResLocationName());
        }
    }

    public TextureAtlasSprite getSprite() {
        return sprite;
    }

    public String getName() {
        return name;
    }

    public SpriteSetPlayer getSpriteSet() {
        return spriteSetPlayer;
    }
}
