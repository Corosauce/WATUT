package com.corosus.watut.network;

import com.corosus.watut.PlayerStatus;
import com.corosus.watut.WATUT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ToServerPlayerStatusMessage {
	private final UUID uuid;
	private final PlayerStatus.StatusType type;

	public ToServerPlayerStatusMessage(UUID player, PlayerStatus.StatusType type) {
		this.uuid = player;
		this.type = type;
	}

	public void encode(PacketBuffer buffer) {
		buffer.writeUniqueId(uuid);
		buffer.writeInt(type.ordinal());
	}

	public static ToServerPlayerStatusMessage decode(PacketBuffer buffer) {
		return new ToServerPlayerStatusMessage(buffer.readUniqueId(), PlayerStatus.StatusType.get(buffer.readInt()));
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			WATUT.playerManagerServer.getPlayerStatus(uuid).setStatusType(type);
		});
		ctx.get().setPacketHandled(true);
	}
}
