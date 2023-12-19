package com.corosus.watut;

import java.util.ArrayList;
import java.util.List;

public class ParticleRegistry {

    public static SpriteInfo inventory;
    public static SpriteInfo chest;
    public static SpriteInfo crafting;
    public static SpriteInfo escape;
    public static SpriteInfo chat_idle;
    public static SpriteInfo chat_typing;
    public static SpriteInfo idle;
    public static List<SpriteInfo> particles = new ArrayList<>();

    static {
        inventory = add("inventory_", 3, 0);
        chest = add("chest_", 3, 0);
        crafting = add("crafting_", 3, 0);
        escape = add("escape_menu_", 3, 0);
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
}
