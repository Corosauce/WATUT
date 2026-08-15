package com.corosus.watut.network;

import com.corosus.coroutil.config.ConfigCoroUtil;
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

import java.util.UUID;

public record PacketNBTFromServer(CompoundTag nbt) implements PacketBase
{
	public static final Type<PacketNBTFromServer> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WatutMod.MODID, "nbt_client"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PacketNBTFromServer> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG, PacketNBTFromServer::nbt,
			PacketNBTFromServer::new);

	public PacketNBTFromServer(FriendlyByteBuf buf)
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
			if (nbt.contains(WatutNetworking.NBTDataPlayerUUID)) {
				UUID uuid = UUID.fromString(nbt.getStringOr(WatutNetworking.NBTDataPlayerUUID, ""));
				WatutMod.getPlayerStatusManagerClient().receiveAny(uuid, nbt);
			} else if (nbt.contains(WatutNetworking.NBTDataServerConfig)) {
				WatutMod.getPlayerStatusManagerClient().receiveServerConfig(nbt);
			} else if (nbt.contains(WatutNetworking.NBTDataItemTransferItemStack)) {
				WatutMod.getPlayerStatusManagerClient().receiveItemMove(nbt);
			}
		} catch (Exception ex) {
			CULog.dbg("WATUT ERROR: packet with invalid uuid sent from server");
			CULog.dbg("full nbt data: " + nbt);
			if (ConfigCoroUtil.useLoggingDebug) {
				ex.printStackTrace();
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
