package com.corosus.watut.status;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rappresenta il tipo di interfaccia grafica (GUI) attualmente aperta dal giocatore.
 */
public enum PlayerGuiState {
    NONE,
    CHAT_SCREEN,
    INVENTORY,
    CRAFTING,
    ESCAPE,
    EDIT_SIGN,
    EDIT_BOOK,
    CHEST,
    ENCHANTING_TABLE,
    ANVIL,
    BEACON,
    BREWING_STAND,
    DISPENSER,
    FURNACE,
    GRINDSTONE,
    HOPPER,
    HORSE,
    LOOM,
    VILLAGER,
    COMMAND_BLOCK,
    MISC;

    private static final Map<Integer, PlayerGuiState> LOOKUP = new HashMap<>();
    private static final List<PlayerGuiState> POINTING_GUIS = new ArrayList<>();
    private static final List<PlayerGuiState> TYPING_GUIS = new ArrayList<>();
    private static final List<PlayerGuiState> SOUND_MAKER_GUIS = new ArrayList<>();

    static {
        for (PlayerGuiState state : EnumSet.allOf(PlayerGuiState.class)) {
            LOOKUP.put(state.ordinal(), state);
            POINTING_GUIS.add(state);
            SOUND_MAKER_GUIS.add(state);
        }
        POINTING_GUIS.remove(NONE);
        POINTING_GUIS.remove(CHAT_SCREEN);
        POINTING_GUIS.remove(EDIT_BOOK);
        POINTING_GUIS.remove(EDIT_SIGN);
        POINTING_GUIS.remove(COMMAND_BLOCK);

        TYPING_GUIS.add(CHAT_SCREEN);
        TYPING_GUIS.add(EDIT_BOOK);
        TYPING_GUIS.add(EDIT_SIGN);
        TYPING_GUIS.add(COMMAND_BLOCK);

        SOUND_MAKER_GUIS.remove(NONE);
        SOUND_MAKER_GUIS.remove(CHAT_SCREEN);
        SOUND_MAKER_GUIS.remove(CHEST);
    }

    public static boolean isPointingGui(PlayerGuiState state) {
        return POINTING_GUIS.contains(state);
    }

    public static boolean isTypingGui(PlayerGuiState state) {
        return TYPING_GUIS.contains(state);
    }

    public static boolean canPreventIdleInGui(PlayerGuiState state) {
        return POINTING_GUIS.contains(state);
    }

    public static boolean isSoundMakerGui(PlayerGuiState state) {
        return SOUND_MAKER_GUIS.contains(state);
    }

    public static PlayerGuiState get(int ordinal) {
        return LOOKUP.getOrDefault(ordinal, NONE);
    }
}
