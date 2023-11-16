package com.corosus.watut;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;

import java.util.UUID;

public class PlayerStatusManagerClient extends PlayerStatusManager {

    private PlayerStatus selfPlayerStatus = PlayerStatus.NONE;

    public void tickPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChatScreen) {
            ChatScreen chat = (ChatScreen) mc.screen;
            //System.out.println("chat: " + chat.input.getValue());
            //TODO: track for changes in text, aka typing vs stopped typing with message open still
            if (chat.input.getValue().length() > 0) {
                sendStatus(PlayerStatus.CHAT_TYPING);
            } else {
                sendStatus(PlayerStatus.CHAT_OPEN);
            }
        } else if (mc.screen == null) {
            sendStatus(PlayerStatus.NONE);
        }
    }

    public void sendStatus(PlayerStatus playerStatus) {
        if (selfPlayerStatus != playerStatus) {
            CompoundTag data = new CompoundTag();
            data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateStatusPlayer);
            data.putInt(WatutNetworking.NBTDataPlayerStatus, playerStatus.ordinal());
            WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_SERVER);
        }
        selfPlayerStatus = playerStatus;
    }

    public void receiveStatus(UUID uuid, PlayerStatus playerStatus) {
        PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
        System.out.println("got status on client: " + playerStatus + " for player name: " + playerInfo.getProfile().getName());
        setStatus(uuid, playerStatus);
    }

}
