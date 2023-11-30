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
                    ServerPlayer playerEntity = ctx.get().getSender();
                    if (playerEntity != null) {
                        if (packetCommand.equals(WatutNetworking.NBTPacketCommandUpdateStatusPlayer)) {
                            Watut.getPlayerStatusManagerServer().receiveStatus(playerEntity, PlayerStatus.PlayerGuiState.get(nbt.getInt(WatutNetworking.NBTDataPlayerStatus)));
                        } else if (packetCommand.equals(WatutNetworking.NBTPacketCommandUpdateMousePlayer)) {
                            Watut.getPlayerStatusManagerServer().receiveMouse(playerEntity, nbt.getFloat(WatutNetworking.NBTDataPlayerMouseX), nbt.getFloat(WatutNetworking.NBTDataPlayerMouseY), nbt.getBoolean(WatutNetworking.NBTDataPlayerMousePressed));
                        } else if (packetCommand.equals(WatutNetworking.NBTPacketCommandUpdateStatusAny)) {
                            Watut.getPlayerStatusManagerServer().receiveAny(playerEntity, nbt);
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