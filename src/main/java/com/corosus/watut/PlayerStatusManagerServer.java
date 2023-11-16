package com.corosus.watut;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;

public class PlayerStatusManagerServer extends PlayerStatusManager {

    public void receiveStatus(Player player, PlayerStatus playerStatus) {
        System.out.println("got status on server: " + playerStatus);
        setStatus(player, playerStatus);
        sendStatusToClients(player, playerStatus);
    }

    public void sendStatusToClients(Player player, PlayerStatus playerStatus) {
        CompoundTag data = new CompoundTag();
        data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateStatusPlayer);
        data.putString(WatutNetworking.NBTDataPlayerUUID, player.getUUID().toString());
        data.putInt(WatutNetworking.NBTDataPlayerStatus, playerStatus.ordinal());
        //WatutNetworking.HANDLER.sendTo(new PacketNBTFromClient(data), Minecraft.getInstance().player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
        WatutNetworking.HANDLER.send(PacketDistributor.ALL.noArg(), new PacketNBTFromServer(data));

        /*if (entP == null) {

            WeatherNetworking.HANDLER.send(PacketDistributor.DIMENSION.with(() -> getWorld().dimension()), new PacketNBTFromServer(data));
        } else {
            WeatherNetworking.HANDLER.sendTo(new PacketNBTFromServer(data), entP.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }*/
    }
}
