package com.corosus.watut.client.particle;

import net.minecraft.client.renderer.texture.TextureAtlas;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro centrale di tutti gli sprite particellari di WATUT.
 */
public class ParticleRegistry {

    public static final List<SpriteInfo> PARTICLES = new ArrayList<>();

    public static final SpriteInfo INVENTORY = add("inventory_", 3, 0);
    public static final SpriteInfo CHEST = add("chest_", 3, 0);
    public static final SpriteInfo CRAFTING = add("crafting_", 3, 0);
    public static final SpriteInfo ESCAPE = add("escape_menu_", 3, 0);
    public static final SpriteInfo SIGN = add("sign");
    public static final SpriteInfo BOOK = add("book");
    public static final SpriteInfo ENCHANTING_TABLE = add("enchanting_table");
    public static final SpriteInfo ANVIL = add("anvil");
    public static final SpriteInfo BEACON = add("beacon");
    public static final SpriteInfo BREWING_STAND = add("brewing_stand");
    public static final SpriteInfo DISPENSER = add("dispenser");
    public static final SpriteInfo FURNACE = add("furnace");
    public static final SpriteInfo GRINDSTONE = add("grindstone");
    public static final SpriteInfo HOPPER = add("hopper");
    public static final SpriteInfo HORSE = add("horse");
    public static final SpriteInfo LOOM = add("loom");
    public static final SpriteInfo VILLAGER = add("villager");
    public static final SpriteInfo COMMAND_BLOCK = add("command_block");

    public static final SpriteInfo CHAT_IDLE = add("chat_idle_", 2, 6);
    public static final SpriteInfo CHAT_TYPING = add("chat_typing_", 6, 2);
    public static final SpriteInfo IDLE = add("idle");

    public static SpriteInfo add(String name) {
        return add(name, 0, 0);
    }

    public static SpriteInfo add(String name, int frames, int tickDelay) {
        SpriteInfo spriteInfo = new SpriteInfo(name, frames, tickDelay);
        PARTICLES.add(spriteInfo);
        return spriteInfo;
    }

    public static void textureAtlasUpload(TextureAtlas textureAtlas) {
        if (!textureAtlas.location().equals(TextureAtlas.LOCATION_PARTICLES)) return;
        for (SpriteInfo info : PARTICLES) {
            info.setupSprites(textureAtlas);
        }
    }
}
