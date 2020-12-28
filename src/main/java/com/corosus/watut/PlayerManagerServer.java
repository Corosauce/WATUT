package com.corosus.watut;

import com.corosus.watut.network.ToClientPlayerStatusMessage;
import com.corosus.watut.network.WATUTNetwork;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerManagerServer extends PlayerManager {

    public void tick(World world) {
        super.tick(world);
        if (world.getGameTime() % 10 == 0) {
            WATUT.LOGGER.debug("?");
            List<? extends PlayerEntity> players = world.getPlayers();
            players.forEach(player -> {
                ToClientPlayerStatusMessage message = new ToClientPlayerStatusMessage(players.stream()
                        .filter(player2 -> player != player2 && player2.getDistance(player) < 24)
                        .map(player2 -> player2.getUniqueID()).collect(Collectors.toList()));

                WATUTNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), message);
            });

            lookupPlayerStatus.entrySet().stream().forEach(entrySet -> {
                    WATUT.LOGGER.debug(world.getGameTime() + " - " + entrySet.getValue().getUuid() + " -> " + entrySet.getValue().getStatusType());
            });
        }
    }

}
