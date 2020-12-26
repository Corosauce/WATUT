package com.corosus.watut;

import com.corosus.watut.network.ToClientPlayerStatusMessage;
import com.corosus.watut.network.ToServerPlayerStatusMessage;
import com.corosus.watut.network.WATUTNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraftforge.event.TickEvent;

public class PlayerManagerClient extends PlayerManager {

    private Class lastScreenClass = null;

    public void tick(TickEvent.WorldTickEvent event) {
        super.tick(event);
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
            } else if (curScreenClass == null) {
                triggerScreenChange(PlayerStatus.StatusType.NONE);
            }
        }

    }

    private void triggerScreenChange(PlayerStatus.StatusType type) {
        Minecraft mc = Minecraft.getInstance();
        lastScreenClass = mc.currentScreen.getClass();
        PlayerStatus status = getPlayerStatus(mc.player.getUniqueID());
        status.setStatusType(type);
        ToServerPlayerStatusMessage message = new ToServerPlayerStatusMessage(status.getUuid(), status.getStatusType());
        WATUTNetwork.CHANNEL.sendToServer(message);
        WATUT.LOGGER.debug("syncing state client -> server");
    }

}
