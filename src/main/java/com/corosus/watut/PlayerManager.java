package com.corosus.watut;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerManager {

    private HashMap<UUID, PlayerStatus> lookupPlayerStatus = new HashMap<>();

    public void tick(TickEvent.WorldTickEvent event) {

    }

    @Nonnull
    public PlayerStatus getPlayerStatus(UUID uuid) {
        if (!lookupPlayerStatus.containsKey(uuid)) {
            lookupPlayerStatus.put(uuid, new PlayerStatus(uuid));
        }
        return lookupPlayerStatus.get(uuid);
    }

}
