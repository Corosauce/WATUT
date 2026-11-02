package com.corosus.watut.network;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.WatutNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public record PacketNBTFromClient(CompoundTag nbt) implements PacketBase
{
	public static final Type<PacketNBTFromClient> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WatutMod.MODID, "nbt_server"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PacketNBTFromClient> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG, PacketNBTFromClient::nbt,
			PacketNBTFromClient::new);

	public PacketNBTFromClient {
	}

	public PacketNBTFromClient(RegistryFriendlyByteBuf buf)
	{
		this(buf.readNbt());
	}


	public void write(FriendlyByteBuf buf)
	{
		buf.writeNbt(nbt);
	}


	public void handle(Player player)
	{
		try {
			if (player != null) {
				WatutMod.getPlayerStatusManagerServer().receiveAny(player, nbt);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
