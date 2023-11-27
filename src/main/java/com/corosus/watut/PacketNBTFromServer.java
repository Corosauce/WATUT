package com.corosus.watut;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketNBTFromServer {
    private final CompoundTag nbt;

    public PacketNBTFromServer(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public static void encode(PacketNBTFromServer msg, FriendlyByteBuf buffer) {
        buffer.writeNbt(msg.nbt);
    }

    public static PacketNBTFromServer decode(FriendlyByteBuf buffer) {
        return new PacketNBTFromServer(buffer.readNbt());
    }

    public static class Handler {
        public static void handle(final PacketNBTFromServer msg, Supplier<NetworkEvent.Context> ctx) {

            ctx.get().enqueueWork(() -> {
                try {
                    CompoundTag nbt = msg.nbt;
                    String packetCommand = nbt.getString(WatutNetworking.NBTPacketCommand);

                    //Watut.dbg("packet command from server: " + packetCommand);
                    UUID uuid = UUID.fromString(nbt.getString(WatutNetworking.NBTDataPlayerUUID));
                    if (packetCommand.equals(WatutNetworking.NBTPacketCommandUpdateStatusPlayer)) {
                        Watut.getPlayerStatusManagerClient().receiveStatus(uuid, PlayerStatus.PlayerGuiState.get(nbt.getInt(WatutNetworking.NBTDataPlayerStatus)));
                    } else if (packetCommand.equals(WatutNetworking.NBTPacketCommandUpdateMousePlayer)) {
                        Watut.getPlayerStatusManagerClient().receiveMouse(uuid, nbt.getFloat(WatutNetworking.NBTDataPlayerMouseX), nbt.getFloat(WatutNetworking.NBTDataPlayerMouseY), nbt.getBoolean(WatutNetworking.NBTDataPlayerMousePressed));
                    } else if (packetCommand.equals(WatutNetworking.NBTPacketCommandUpdateStatusAny)) {
                        Watut.getPlayerStatusManagerClient().receiveAny(uuid, nbt);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            ctx.get().setPacketHandled(true);
        }
    }
}
