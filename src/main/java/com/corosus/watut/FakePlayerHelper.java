package com.corosus.watut;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class FakePlayerHelper {

    // Cache for class lookups
    private static final Map<String, Class<?>> classCache = new HashMap<>();

    // Check if the given object is an instance of a FakePlayer
    public static boolean isFakePlayer(Player object) {
        if (object == null) {
            return false;
        }

        try {
            // Check for Fabric's FakePlayer
            Class<?> fabricFakePlayerClass = getClassFromCache("net.fabricmc.fabric.api.entity.FakePlayer");
            if (fabricFakePlayerClass != null && fabricFakePlayerClass.isInstance(object)) {
                return true;
            }

            // Check for Forge's FakePlayer
            Class<?> forgeFakePlayerClass = getClassFromCache("net.minecraftforge.common.util.FakePlayer");
            if (forgeFakePlayerClass != null && forgeFakePlayerClass.isInstance(object)) {
                return true;
            }

        } catch (Exception e) {
            // Catch any unexpected exceptions
            e.printStackTrace();
        }

        return false;
    }

    // Utility method to get a Class object with caching
    private static Class<?> getClassFromCache(String className) {
        return classCache.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                return null;
            }
        });
    }
}
