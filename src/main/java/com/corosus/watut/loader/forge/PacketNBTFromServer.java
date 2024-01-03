package com.corosus.watut.loader.forge;

import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketNBTFromServer {
    private final CompoundNBT nbt;

    public PacketNBTFromServer(CompoundNBT nbt) {
        this.nbt = nbt;
    }

    public static void encode(PacketNBTFromServer msg, PacketBuffer buffer) {
        buffer.writeNbt(msg.nbt);
    }

    public static PacketNBTFromServer decode(PacketBuffer buffer) {
        return new PacketNBTFromServer(buffer.readNbt());
    }

    public static class Handler {
        public static void handle(final PacketNBTFromServer msg, Supplier<NetworkEvent.Context> ctx) {

            ctx.get().enqueueWork(() -> {
                try {
                    CompoundNBT nbt = msg.nbt;
                    UUID uuid = UUID.fromString(nbt.getString(WatutNetworking.NBTDataPlayerUUID));
                    WatutMod.getPlayerStatusManagerClient().receiveAny(uuid, nbt);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            ctx.get().setPacketHandled(true);
        }
    }
}
