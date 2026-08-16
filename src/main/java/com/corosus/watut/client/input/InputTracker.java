package com.corosus.watut.client.input;

import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.status.PlayerChatState;
import com.corosus.watut.status.PlayerGuiState;
import com.corosus.watut.status.PlayerStatus;
import com.ibm.icu.impl.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.world.entity.player.Player;

/**
 * Gestisce il tracciamento degli input dell'utente (mouse, tastiera, stato GUI e velocità di digitazione).
 */
public class InputTracker {

    private final long typingIdleTimeout = 60;
    private int typeRatePollCounter = 0;
    private boolean wasMousePressed = false;
    private int mousePressedCountdown = 0;

    public static Screen getCurrentScreen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gui != null ? mc.gui.screen() : null;
    }

    public PlayerGuiState detectGuiState(Screen screen) {
        if (screen == null || screen instanceof DeathScreen) {
            return PlayerGuiState.NONE;
        }

        return switch (screen) {
            case ChatScreen _                          -> PlayerGuiState.CHAT_SCREEN;
            case InventoryScreen _                     -> PlayerGuiState.INVENTORY;
            case CraftingScreen _                      -> PlayerGuiState.CRAFTING;
            case PauseScreen _                         -> PlayerGuiState.ESCAPE;
            case BookEditScreen _                      -> PlayerGuiState.EDIT_BOOK;
            case AbstractSignEditScreen _              -> PlayerGuiState.EDIT_SIGN;
            case ContainerScreen _, ShulkerBoxScreen _ -> PlayerGuiState.CHEST;
            case EnchantmentScreen _                   -> PlayerGuiState.ENCHANTING_TABLE;
            case AnvilScreen _                         -> PlayerGuiState.ANVIL;
            case BeaconScreen _                        -> PlayerGuiState.BEACON;
            case BrewingStandScreen _                  -> PlayerGuiState.BREWING_STAND;
            case DispenserScreen _                     -> PlayerGuiState.DISPENSER;
            case AbstractFurnaceScreen<?> _            -> PlayerGuiState.FURNACE;
            case GrindstoneScreen _                    -> PlayerGuiState.GRINDSTONE;
            case HopperScreen _                        -> PlayerGuiState.HOPPER;
            case HorseInventoryScreen _                -> PlayerGuiState.HORSE;
            case LoomScreen _                          -> PlayerGuiState.LOOM;
            case MerchantScreen _                      -> PlayerGuiState.VILLAGER;
            case AbstractCommandBlockEditScreen _      -> PlayerGuiState.COMMAND_BLOCK;
            default                                    -> PlayerGuiState.MISC;
        };
    }

    public boolean isGuiFocusedOnTextBox(Screen screen) {
        return screen instanceof ChatScreen
                || screen instanceof AbstractSignEditScreen
                || screen instanceof BookEditScreen
                || screen instanceof AbstractCommandBlockEditScreen;
    }

    public String extractTextFromScreen(Screen screen) {
        if (screen instanceof ChatScreen chatScreen) {
            return chatScreen.input.getValue();
        } else if (screen instanceof BookEditScreen bookEditScreen) {
            return bookEditScreen.page != null ? bookEditScreen.page.getValue() : "";
        } else if (screen instanceof AbstractSignEditScreen signScreen) {
            return signScreen.signField.getMessageFn.get();
        } else if (screen instanceof AbstractCommandBlockEditScreen cmdScreen) {
            return cmdScreen.commandEdit.getValue();
        }
        return "";
    }

    public boolean checkIfTyping(String input, Player player, PlayerStatus statusLocal, PlayerStatus statusPrevLocal) {
        if (statusPrevLocal.getPlayerGuiState() == PlayerGuiState.NONE &&
                statusLocal.getPlayerGuiState() != PlayerGuiState.NONE) {
            statusLocal.setLastTypeString(input);
            statusLocal.setLastTypeStringForAmp(input);
            statusLocal.setTypingAmplifier(0);
            statusLocal.setLastTypeDiff(0);
            statusLocal.setLastTypeTime(0);
        }

        typeRatePollCounter++;
        if (!input.isEmpty()) {
            if (!input.startsWith("/")) {
                if (!input.equals(statusLocal.getLastTypeString())) {
                    statusLocal.setLastTypeString(input);
                    statusLocal.setLastTypeTime(player.level().getGameTime());
                }

                if (typeRatePollCounter >= 10) {
                    typeRatePollCounter = 0;
                    int lengthPrev = statusLocal.getLastTypeStringForAmp().length();
                    if (!input.equals(statusLocal.getLastTypeStringForAmp())) {
                        statusLocal.setLastTypeStringForAmp(input);
                        statusLocal.setLastTypeTimeForAmp(player.level().getGameTime());
                        int length = input.length();
                        int newDiff = length - lengthPrev;
                        float amp = Math.max(0, Math.min(8, (newDiff / 6.0F) * 2.0F));
                        statusLocal.setTypingAmplifier(ConfigClient.sendTypingSpeed ? amp : 1.0F);
                    } else {
                        if (ConfigClient.sendTypingSpeed) statusLocal.setTypingAmplifier(0);
                    }
                }
            }
        } else {
            statusLocal.setLastTypeString(input);
            statusLocal.setLastTypeDiff(0);
            return false;
        }

        return statusLocal.getLastTypeTime() + typingIdleTimeout >= player.level().getGameTime();
    }

    public Pair<Float, Float> getMousePos() {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();
        double guiScaleMax = 4.0;
        double xPercent = (mc.mouseHandler.xpos() / mc.getWindow().getScreenWidth()) - 0.5;
        double yPercent = (mc.mouseHandler.ypos() / mc.getWindow().getScreenHeight()) - 0.5;

        double emphasis = guiScaleMax / guiScale;
        double edgeLimit = 0.75;
        xPercent *= emphasis;
        yPercent *= emphasis;
        xPercent = Math.max(Math.min(xPercent, edgeLimit), -edgeLimit);
        yPercent = Math.max(Math.min(yPercent, edgeLimit), -edgeLimit);

        return Pair.of((float) xPercent, (float) yPercent);
    }

    public boolean isWasMousePressed() {
        return wasMousePressed;
    }

    public void setWasMousePressed(boolean wasMousePressed) {
        this.wasMousePressed = wasMousePressed;
    }

    public int getMousePressedCountdown() {
        return mousePressedCountdown;
    }

    public void setMousePressedCountdown(int countdown) {
        this.mousePressedCountdown = countdown;
    }

    public void decrementMousePressedCountdown() {
        if (this.mousePressedCountdown > 0) {
            this.mousePressedCountdown--;
        }
    }
}
