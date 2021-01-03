package com.corosus.watut.network;

import com.corosus.watut.PlayerStatus;
import com.corosus.watut.WATUT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ToClientPlayerStatusMessage {
	//private final List<UUID> players;
	//private final List<UUID> ;
	private final List<PlayerStatus> statuses;

	public ToClientPlayerStatusMessage(List<PlayerStatus> statuses) {
		this.statuses = statuses;
	}

	public void encode(PacketBuffer buffer) {
		buffer.writeVarInt(statuses.size());
		for (PlayerStatus status : statuses) {
			buffer.writeUniqueId(status.getUuid());
			buffer.writeInt(status.getStatusType().ordinal());
		}
	}

	public static ToClientPlayerStatusMessage decode(PacketBuffer buffer) {
		int count = buffer.readVarInt();
		List<PlayerStatus> statuses = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			statuses.add(new PlayerStatus(buffer.readUniqueId(), PlayerStatus.StatusType.get(buffer.readInt())));
		}

		return new ToClientPlayerStatusMessage(statuses);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			//WATUT.LOGGER.debug("received packet from server, statuses size: " + statuses.size());
			for (PlayerStatus status : statuses) {
				WATUT.playerManagerClient.getPlayerStatus(status.getUuid()).update(status);
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
