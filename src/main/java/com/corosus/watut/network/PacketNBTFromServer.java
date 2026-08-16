package com.corosus.watut.network;

import com.corosus.watut.WatutMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Pacchetto inviato dal Server ai Client (S2C) contenente gli stati aggiornati degli altri giocatori.
 */
public record PacketNBTFromServer(CompoundTag nbt) implements CustomPacketPayload {
    public static final Type<PacketNBTFromServer> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WatutMod.MODID, "nbt_client"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketNBTFromServer> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, PacketNBTFromServer::nbt,
            PacketNBTFromServer::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
