package com.corosus.watut;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketNBTFromClient {
    private final CompoundTag nbt;

    public PacketNBTFromClient(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public static void encode(PacketNBTFromClient msg, FriendlyByteBuf buffer) {
        buffer.writeNbt(msg.nbt);
    }

    public static PacketNBTFromClient decode(FriendlyByteBuf buffer) {
        return new PacketNBTFromClient(buffer.readNbt());
    }

    public static class Handler {
        public static void handle(final PacketNBTFromClient msg, Supplier<NetworkEvent.Context> ctx) {

            ctx.get().enqueueWork(() -> {
                try {
                    CompoundTag nbt = msg.nbt;
                    String packetCommand = nbt.getString(WatutNetworking.NBTPacketCommand);

                    Watut.dbg("packet command from client: " + packetCommand + " - ");
                    if (packetCommand.equals(WatutNetworking.NBTPacketCommandUpdateStatusPlayer)) {
                        ServerPlayer playerEntity = ctx.get().getSender();
                        if (playerEntity != null) {
                            Watut.getPlayerStatusManagerServer().receiveStatus(playerEntity, PlayerStatus.get(nbt.getInt(WatutNetworking.NBTDataPlayerStatus)));
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            ctx.get().setPacketHandled(true);
        }
    }
}