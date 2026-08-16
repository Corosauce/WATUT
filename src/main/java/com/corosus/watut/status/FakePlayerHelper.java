package com.corosus.watut.status;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility per rilevare istanze di FakePlayer create da mod terze (es. Fabric/Forge FakePlayer).
 */
public class FakePlayerHelper {
    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>();

    public static boolean isFakePlayer(Player player) {
        if (player == null) return false;

        try {
            Class<?> fabricFakePlayer = getClassFromCache("net.fabricmc.fabric.api.entity.FakePlayer");
            if (fabricFakePlayer != null && fabricFakePlayer.isInstance(player)) {
                return true;
            }

            Class<?> forgeFakePlayer = getClassFromCache("net.minecraftforge.common.util.FakePlayer");
            if (forgeFakePlayer != null && forgeFakePlayer.isInstance(player)) {
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private static Class<?> getClassFromCache(String className) {
        return CLASS_CACHE.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        });
    }
}
