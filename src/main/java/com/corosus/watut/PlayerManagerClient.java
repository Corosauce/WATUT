package com.corosus.watut;

import com.corosus.watut.network.ToClientPlayerStatusMessage;
import com.corosus.watut.network.ToServerPlayerStatusMessage;
import com.corosus.watut.network.WATUTNetwork;
import com.corosus.watut.particles.HeartParticle2;
import com.corosus.watut.particles.StatusParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.particle.TexturedParticle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;

public class PlayerManagerClient extends PlayerManager {

    private Class lastScreenClass = null;

    public void tick(World world) {
        super.tick(world);
        Minecraft mc = Minecraft.getInstance();
        if (mc.world == null || mc.player == null) return;
        //List<AbstractClientPlayerEntity> listClosePlayers = mc.world.getPlayers().stream().filter((player) -> mc.player.getDistance(player) < 24).collect(Collectors.toList());
        //listClosePlayers.stream().forEach();
        Class curScreenClass = null;
        if (mc.currentScreen != null) {
            curScreenClass = mc.currentScreen.getClass();
        }
        if (curScreenClass != lastScreenClass) {
            if (mc.currentScreen instanceof ChatScreen) {
                triggerScreenChange(PlayerStatus.StatusType.CHAT);
                lastScreenClass = mc.currentScreen.getClass();
            } else if (curScreenClass == null) {
                triggerScreenChange(PlayerStatus.StatusType.NONE);
                lastScreenClass = null;
            }
        }

        if (mc.world.getGameTime() % 5 == 0) {
            for (PlayerEntity playerEntity : mc.world.getPlayers()) {
                if (mc.player.getDistance(playerEntity) < 20) {
                    if (WATUT.playerManagerClient.getPlayerStatus(mc.player.getUniqueID()).getStatusType() == PlayerStatus.StatusType.CHAT) {
                        /*StatusParticle particle = new StatusParticle(mc.world, playerEntity.getPosX(), playerEntity.getPosY() + 2.2, playerEntity.getPosZ());
                        particle.setSprite(EventHandlerForge.square16);
                        particle.setMaxAge(5);
                        particle.setSize(0.5F, 0.5F);
                        particle.setScale(0.2F);*/
                        HeartParticle2 particle = new HeartParticle2(mc.world, playerEntity.getPosX(), playerEntity.getPosY() + 2.2, playerEntity.getPosZ());
                        //particle.setSprite(EventHandlerForge.square16);
                        //particle.setMaxAge(5);
                        //particle.setSize(0.5F, 0.5F);
                        //particle.setScale(0.2F);
                        mc.particles.addEffect(particle);
                    }
                }


            }
        }

        lookupPlayerStatus.entrySet().stream().forEach(entrySet -> {
            //WATUT.LOGGER.debug("client:" + world.getGameTime() + " - " + entrySet.getValue().getUuid() + " -> " + entrySet.getValue().getStatusType());
        });

    }

    private void triggerScreenChange(PlayerStatus.StatusType type) {
        Minecraft mc = Minecraft.getInstance();
        PlayerStatus status = getPlayerStatus(mc.player.getUniqueID());
        status.setStatusType(type);
        ToServerPlayerStatusMessage message = new ToServerPlayerStatusMessage(status.getUuid(), status.getStatusType());
        WATUTNetwork.CHANNEL.sendToServer(message);
        WATUT.LOGGER.debug("syncing state client -> server");
    }

}
