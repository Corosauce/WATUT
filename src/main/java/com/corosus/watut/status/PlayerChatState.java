package com.corosus.watut.status;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Rappresenta lo stato di digitazione chat del giocatore.
 */
public enum PlayerChatState {
    NONE,
    CHAT_FOCUSED,
    CHAT_TYPING;

    private static final Map<Integer, PlayerChatState> LOOKUP = new HashMap<>();

    static {
        for (PlayerChatState state : EnumSet.allOf(PlayerChatState.class)) {
            LOOKUP.put(state.ordinal(), state);
        }
    }

    public static PlayerChatState get(int ordinal) {
        return LOOKUP.getOrDefault(ordinal, NONE);
    }
}
