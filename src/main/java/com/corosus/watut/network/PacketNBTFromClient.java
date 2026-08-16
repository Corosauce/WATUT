package com.corosus.watut.network;

import com.corosus.watut.WatutMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Pacchetto inviato dal Client al Server (C2S) contenente gli stati aggiornati del giocatore.
 */
public record PacketNBTFromClient(CompoundTag nbt) implements CustomPacketPayload {
    public static final Type<PacketNBTFromClient> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WatutMod.MODID, "nbt_server"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketNBTFromClient> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, PacketNBTFromClient::nbt,
            PacketNBTFromClient::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
