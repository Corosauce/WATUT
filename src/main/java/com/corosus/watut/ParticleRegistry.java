package com.corosus.watut;

import net.minecraft.client.renderer.texture.TextureAtlas;

import java.util.ArrayList;
import java.util.List;

public class ParticleRegistry {

    public static SpriteInfo inventory;
    public static SpriteInfo chest;
    public static SpriteInfo crafting;
    public static SpriteInfo escape;
    public static SpriteInfo sign;
    public static SpriteInfo book;
    public static SpriteInfo enchanting_table;
    public static SpriteInfo anvil;
    public static SpriteInfo beacon;
    public static SpriteInfo brewing_stand;
    public static SpriteInfo dispenser;
    public static SpriteInfo furnace;
    public static SpriteInfo grindstone;
    public static SpriteInfo hopper;
    public static SpriteInfo horse;
    public static SpriteInfo loom;
    public static SpriteInfo villager;
    public static SpriteInfo command_block;
    public static SpriteInfo chat_idle;
    public static SpriteInfo chat_typing;
    public static SpriteInfo idle;
    public static List<SpriteInfo> particles = new ArrayList<>();

    static {
        inventory = add("inventory_", 3, 0);
        chest = add("chest_", 3, 0);
        crafting = add("crafting_", 3, 0);
        escape = add("escape_menu_", 3, 0);
        sign = add("sign", 0, 0);
        book = add("book", 0, 0);
        enchanting_table = add("enchanting_table", 0, 0);
        anvil = add("anvil", 0, 0);
        beacon = add("beacon", 0, 0);
        brewing_stand = add("brewing_stand", 0, 0);
        dispenser = add("dispenser", 0, 0);
        furnace = add("furnace", 0, 0);
        grindstone = add("grindstone", 0, 0);
        hopper = add("hopper", 0, 0);
        horse = add("horse", 0, 0);
        loom = add("loom", 0, 0);
        villager = add("villager", 0, 0);
        command_block = add("command_block", 0, 0);

        chat_idle = add("chat_idle_", 2, 6);
        chat_typing = add("chat_typing_", 6, 2);
        idle = add("idle");
    }

    public static SpriteInfo add(String name) {
        return add(name, 0, 0);
    }

    public static SpriteInfo add(String name, int frames, int tickDelay) {
        SpriteInfo spriteInfo = new SpriteInfo(name, frames, tickDelay);
        particles.add(spriteInfo);
        return spriteInfo;
    }

    public static void textureAtlasUpload(TextureAtlas textureAtlas) {
        if (!textureAtlas.location().equals(TextureAtlas.LOCATION_PARTICLES)) return;
        for (SpriteInfo info : ParticleRegistry.particles) {
            info.setupSprites(textureAtlas);
        }
    }
}
