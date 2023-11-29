package com.corosus.watut;

import com.corosus.watut.config.ConfigCommon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

public class PlayerStatusManagerServer extends PlayerStatusManager {

    public void receiveStatus(Player player, PlayerStatus.PlayerGuiState playerGuiState) {
        //Watut.dbg("got status on server: " + playerStatus);
        getStatus(player).setPlayerGuiState(playerGuiState);
        sendStatusToClients(player, playerGuiState);
    }

    public void receiveMouse(Player player, float x, float y, boolean pressed) {
        //Watut.dbg("got status on server: " + playerStatus);
        setMouse(player.getUUID(), x, y, pressed);
        sendMouseToClients(player, x, y, pressed);
    }

    /**
     * receive data from client, inject the relevant player uuid, and send it right back to the rest of the relevant clients except sender
     *
     * @param player
     * @param data
     */
    //TODO: consolidate the other receiving packet methods into this strategy
    public void receiveAny(Player player, CompoundTag data) {
        data.putString(WatutNetworking.NBTDataPlayerUUID, player.getUUID().toString());
        if (data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) {
            handleIdleState(player, data.getInt(WatutNetworking.NBTDataPlayerIdleTicks));
        }
        if (data.contains(WatutNetworking.NBTDataPlayerStatus)) {
            WatutNetworking.HANDLER.send(PacketDistributor.ALL.noArg(), new PacketNBTFromServer(data));
        } else {
            WatutNetworking.HANDLER.send(PacketDistributor.NEAR.with(() ->
                            new PacketDistributor.TargetPoint(player.getX(), player.getY(), player.getZ(), 16, player.level().dimension())),
                    new PacketNBTFromServer(data));
        }
    }

    public void handleIdleState(Player player, int idleTicks) {
        PlayerStatus status = getStatus(player);
        if (ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerCount() > 1 || singleplayerTesting) {
            if (idleTicks > 0) {
                if (!status.isIdle()) {
                    broadcast(player.getDisplayName().getString() + " has gone idle");
                }
            } else {
                if (status.isIdle()) {
                    broadcast(player.getDisplayName().getString() + " is no longer idle");
                }
            }
        }
        status.setIdleTicks(idleTicks);
    }

    public void broadcast(String msg) {
        if (ConfigCommon.announceIdleStatesInChat) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
        }
    }

    public void sendStatusToClients(Player player, PlayerStatus.PlayerGuiState playerStatus) {
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

    public void sendMouseToClients(Player player, float x, float y, boolean pressed) {
        CompoundTag data = new CompoundTag();
        data.putString(WatutNetworking.NBTPacketCommand, WatutNetworking.NBTPacketCommandUpdateMousePlayer);
        data.putString(WatutNetworking.NBTDataPlayerUUID, player.getUUID().toString());
        data.putFloat(WatutNetworking.NBTDataPlayerMouseX, x);
        data.putFloat(WatutNetworking.NBTDataPlayerMouseY, y);
        data.putBoolean(WatutNetworking.NBTDataPlayerMousePressed, pressed);

        WatutNetworking.HANDLER.send(PacketDistributor.NEAR.with(() ->
                new PacketDistributor.TargetPoint(player.getX(), player.getY(), player.getZ(), 16, player.level().dimension())),
                new PacketNBTFromServer(data));
        //WatutNetworking.HANDLER.send(PacketDistributor.DIMENSION.with(() -> player.level().dimension()), new PacketNBTFromServer(data));
    }
}
