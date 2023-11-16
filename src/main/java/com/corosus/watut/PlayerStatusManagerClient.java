package com.corosus.watut;

import com.corosus.watut.particle.ParticleAnimated;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;

import java.util.HashMap;
import java.util.UUID;

public class PlayerStatusManagerClient extends PlayerStatusManager {

    private PlayerStatus selfPlayerStatus = PlayerStatus.NONE;
    public HashMap<UUID, PlayerStatus> lookupPlayerToStatusPrev = new HashMap<>();
    public HashMap<UUID, ParticleAnimated> lookupPlayerToParticle = new HashMap<>();

    public void tickPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.getUUID().equals(player.getUUID())) {
            tickLocalPlayerClient(player);

            //temp
            tickOtherPlayerClient(player);
        } else {
            tickOtherPlayerClient(player);
        }
    }

    public void tickLocalPlayerClient(Player player) {
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

    public void tickOtherPlayerClient(Player player) {
        //if (this.getStatus(player) == PlayerStatus.CHAT_OPEN) {
            Vec3 pos = player.position();
            PlayerStatus playerStatus = getStatus(player);
            PlayerStatus playerStatusPrev = getStatusPrev(player);
            if (playerStatus != playerStatusPrev || !lookupPlayerToParticle.containsKey(player.getUUID())) {
                if (lookupPlayerToParticle.containsKey(player.getUUID())) {
                    lookupPlayerToParticle.get(player.getUUID()).remove();
                }
                if (this.getStatus(player) == PlayerStatus.CHAT_OPEN) {
                    ParticleAnimated particleAnimated = new ParticleAnimated((ClientLevel) player.level(), pos.x + 0.0D, (double) pos.y + 2.5D, (double) pos.z + 0.0D
                            , 0, 0, 0, ParticleRegistry.chat_idle_set);
                    lookupPlayerToParticle.put(player.getUUID(), particleAnimated);
                    Minecraft.getInstance().particleEngine.add(particleAnimated);
                } else if (this.getStatus(player) == PlayerStatus.CHAT_TYPING) {
                    ParticleAnimated particleAnimated = new ParticleAnimated((ClientLevel) player.level(), pos.x + 0.0D, (double) pos.y + 2.5D, (double) pos.z + 0.0D
                            , 0, 0, 0, ParticleRegistry.chat_typing_set);
                    lookupPlayerToParticle.put(player.getUUID(), particleAnimated);
                    Minecraft.getInstance().particleEngine.add(particleAnimated);
                }
            } else {
                if (lookupPlayerToParticle.containsKey(player.getUUID())) {
                    ParticleAnimated particleAnimated = lookupPlayerToParticle.get(player.getUUID());
                    if (!particleAnimated.isAlive()) {
                        lookupPlayerToParticle.remove(player.getUUID());
                    } else {
                        particleAnimated.setPos(pos.x + 0.0D, (double)pos.y + 2.5D, (double)pos.z + 0.0D);
                        particleAnimated.setParticleSpeed(0, 0, 0);
                    }
                }
            }
            setStatusPrev(player, playerStatus);
        //}
    }

    public PlayerStatus getStatusPrev(Player player) {
        if (!lookupPlayerToStatusPrev.containsKey(player.getUUID())) {
            lookupPlayerToStatusPrev.put(player.getUUID(), PlayerStatus.NONE);
        }
        return lookupPlayerToStatusPrev.get(player.getUUID());
    }

    public void setStatusPrev(Player player, PlayerStatus statusType) {
        setStatusPrev(player.getUUID(), statusType);
    }

    public void setStatusPrev(UUID uuid, PlayerStatus statusType) {
        lookupPlayerToStatusPrev.put(uuid, statusType);
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
