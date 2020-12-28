package com.corosus.watut;

import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

public class PlayerManager {

    protected HashMap<UUID, PlayerStatus> lookupPlayerStatus = new HashMap<>();

    public void tick(World world) {

    }

    @Nonnull
    public PlayerStatus getPlayerStatus(UUID uuid) {
        if (!lookupPlayerStatus.containsKey(uuid)) {
            lookupPlayerStatus.put(uuid, new PlayerStatus(uuid));
        }
        return lookupPlayerStatus.get(uuid);
    }

}
