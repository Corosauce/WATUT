package com.corosus.watut;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum PlayerStatus {

    NONE,
    CHAT_OPEN,
    CHAT_TYPING,
    INVENTORY,
    MISC;

    private static final Map<Integer, PlayerStatus> lookup = new HashMap<Integer, PlayerStatus>();
    static { for(PlayerStatus e : EnumSet.allOf(PlayerStatus.class)) { lookup.put(e.ordinal(), e); } }
    public static PlayerStatus get(int intValue) { return lookup.get(intValue); }

}
