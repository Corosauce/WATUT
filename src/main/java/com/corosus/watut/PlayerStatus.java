package com.corosus.watut;

import com.corosus.watut.network.ToClientPlayerStatusMessage;
import net.minecraft.network.PacketBuffer;

import java.util.*;

public class PlayerStatus {

    private UUID uuid;
    private StatusType statusType = StatusType.NONE;

    public enum StatusType {
        NONE,
        CHAT,
        INVENTORY,
        MISC;

        private static final Map<Integer, StatusType> lookup = new HashMap<Integer, StatusType>();
        static { for(StatusType e : EnumSet.allOf(StatusType.class)) { lookup.put(e.ordinal(), e); } }
        public static StatusType get(int intValue) { return lookup.get(intValue); }
    }

    PlayerStatus(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public StatusType getStatusType() {
        return statusType;
    }

    public void setStatusType(StatusType statusType) {
        this.statusType = statusType;
    }

    public void encode(PacketBuffer buffer) {
        buffer.writeUniqueId(uuid);
        buffer.writeInt(statusType.ordinal());
    }

    public static ToClientPlayerStatusMessage decode(PacketBuffer buffer) {

        int count = buffer.readVarInt();
        List<UUID> players = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            players.add(buffer.readUniqueId());
        }

        return new ToClientPlayerStatusMessage(players);
    }

}
