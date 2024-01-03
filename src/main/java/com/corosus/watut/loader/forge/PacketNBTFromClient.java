package com.corosus.watut.loader.forge;

import com.corosus.watut.WatutMod;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketNBTFromClient {
    private final CompoundNBT nbt;

    public PacketNBTFromClient(CompoundNBT nbt) {
        this.nbt = nbt;
    }

    public static void encode(PacketNBTFromClient msg, PacketBuffer buffer) {
        buffer.writeNbt(msg.nbt);
    }

    public static PacketNBTFromClient decode(PacketBuffer buffer) {
        return new PacketNBTFromClient(buffer.readNbt());
    }

    public static class Handler {
        public static void handle(final PacketNBTFromClient msg, Supplier<NetworkEvent.Context> ctx) {

            ctx.get().enqueueWork(() -> {
                try {
                    CompoundNBT nbt = msg.nbt;
                    ServerPlayerEntity playerEntity = ctx.get().getSender();
                    if (playerEntity != null) {
                        WatutMod.getPlayerStatusManagerServer().receiveAny(playerEntity, nbt);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            ctx.get().setPacketHandled(true);
        }
    }
}