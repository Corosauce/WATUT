package com.corosus.watut.config;

import com.corosus.watut.WatutMod;
import com.google.gson.Gson;
import com.ibm.icu.impl.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Gestisce le correzioni personalizzate degli angoli delle braccia in base all'oggetto impugnato.
 * 
 * COME FUNZIONA:
 * Alcuni oggetti (es. spade enormi, scudi, mappe, balestre o armi di altre mod) possono
 * collidere visivamente con la testa o avere una posa strana quando il giocatore apre un inventario o digita.
 * 
 * Questo sistema legge il file JSON (config/watut-item-arm-adjustments.json) e permette di:
 * 1. Filtrare oggetti per ID esatto ("minecraft:shield"), per mod ("@modid") o con wildcard ("*sword*").
 * 2. Applicare rotazioni correttive (espresse in gradi nel JSON, convertite in radianti per Minecraft).
 * 3. Disattivare l'animazione di un braccio usando la parola chiave "disable" (rappresentata come Float.MAX_VALUE).
 */
public class CustomArmCorrections {

    private static HeldItemArmAdjustmentLists heldItemArmAdjustmentLists = null;

    /**
     * Carica il file di configurazione JSON da disco.
     * @return true se il file è stato caricato e parsato correttamente, false altrimenti.
     */
    public static boolean loadJsonConfigs() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("./config/" + WatutMod.configJSONName)) {
            heldItemArmAdjustmentLists = gson.fromJson(reader, HeldItemArmAdjustmentLists.class);
        } catch (IOException e) {
            System.out.println("FAILED TO LOAD watut-item-arm-adjustments.json, check its formatting!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static HeldItemArmAdjustmentLists getHeldItemArmAdjustmentLists() {
        return heldItemArmAdjustmentLists;
    }

    /**
     * Calcola il vettore di rotazione correttivo (in radianti) da applicare al braccio.
     * 
     * @param stackMainArm    Oggetto nella mano associata a questo braccio
     * @param stackOtherHandArm Oggetto nell'altra mano
     * @param equipmentSlot   Slot dell'equipaggiamento analizzato
     * @return Vector3f contenente (xRot, yRot, zRot) in radianti, oppure Float.MAX_VALUE se disattivato
     */
    public static Vector3f getAdjustmentForArm(ItemStack stackMainArm, ItemStack stackOtherHandArm, EquipmentSlot equipmentSlot) {
        if (getHeldItemArmAdjustmentLists() == null) {
            return new Vector3f(0, 0, 0);
        }

        try {
            for (HeldItemArmAdjustment heldItemArmAdjustment : getHeldItemArmAdjustmentLists().getHeldItemArmAdjustments()) {
                Adjustment adj = heldItemArmAdjustment.getAdjustment();
                if (adj == null) continue;

                // Controlla se la regola specifica rotazioni per la mano corrispondente o per l'altra mano
                boolean shouldMatchMatchingHand = isConfigured(adj.getmatchingHandX()) || isConfigured(adj.getmatchingHandY()) || isConfigured(adj.getmatchingHandZ());
                boolean shouldMatchOtherHand = isConfigured(adj.getotherHandX()) || isConfigured(adj.getotherHandY()) || isConfigured(adj.getotherHandZ());

                boolean matchMatchingHand = shouldMatchMatchingHand && filterMatches(heldItemArmAdjustment, stackMainArm);
                boolean matchOtherHand = shouldMatchOtherHand && filterMatches(heldItemArmAdjustment, stackOtherHandArm);

                // Controlla se la regola è vincolata alla presenza di una specifica mod installata
                String modReq = heldItemArmAdjustment.getOnly_if_mod_installed();
                boolean modIDMatches = modReq == null || modReq.isEmpty() || WatutMod.instance().isModInstalled(modReq);

                if ((matchMatchingHand || matchOtherHand) && modIDMatches) {
                    if (matchMatchingHand) {
                        return parseArmVector(adj.getmatchingHandX(), adj.getmatchingHandY(), adj.getmatchingHandZ());
                    } else {
                        return parseArmVector(adj.getotherHandX(), adj.getotherHandY(), adj.getotherHandZ());
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new Vector3f(0, 0, 0);
    }

    /**
     * Verifica se un valore nel JSON rappresenta una rotazione configurata (non vuota e diversa da "0").
     */
    private static boolean isConfigured(String val) {
        return val != null && !val.trim().isEmpty() && !val.trim().equals("0");
    }

    /**
     * Converte le stringhe X, Y, Z in un vettore Vector3f di angoli in radianti.
     * Gestisce la parola chiave "disable" impostando il valore a Float.MAX_VALUE.
     */
    private static Vector3f parseArmVector(String xStr, String yStr, String zStr) {
        float x = parseArmAngle(xStr);
        float y = parseArmAngle(yStr);
        float z = parseArmAngle(zStr);

        return new Vector3f(
            x == Float.MAX_VALUE ? x : Mth.DEG_TO_RAD * x,
            y == Float.MAX_VALUE ? y : Mth.DEG_TO_RAD * y,
            z == Float.MAX_VALUE ? z : Mth.DEG_TO_RAD * z
        );
    }

    /**
     * Converte una singola stringa dal JSON nel valore float corrispondente.
     */
    private static float parseArmAngle(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equals("0")) {
            return 0f;
        }
        if (value.trim().toLowerCase().startsWith("disable")) {
            return Float.MAX_VALUE;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    /**
     * Verifica se l'ItemStack soddisfa uno dei filtri specificati nella regola JSON.
     * Supporta:
     * - ID completo: "minecraft:shield"
     * - Mod ID: "@twilightforest"
     * - Wildcard inizio/fine/contenuto: "*sword*", "*bow", "modid:*"
     */
    private static boolean filterMatches(HeldItemArmAdjustment heldItemArmAdjustment, ItemStack stack) {
        if (stack == null || stack.isEmpty() || heldItemArmAdjustment.getFilters() == null) {
            return false;
        }

        String fullName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (fullName.equals("minecraft:air")) {
            return false;
        }

        String[] parts = fullName.split(":");
        String modID = parts[0];

        for (String filter : heldItemArmAdjustment.getFilters()) {
            if (filter == null || filter.isEmpty()) continue;

            // Controllo per prefisso mod '@' (es. "@farmersdelight")
            if (filter.startsWith("@")) {
                if (filter.substring(1).equalsIgnoreCase(modID)) {
                    return true;
                }
                continue;
            }

            // Corrispondenza esatta
            if (filter.equalsIgnoreCase(fullName)) {
                return true;
            }

            // Corrispondenza con caratteri jolly (wildcard '*')
            if (filter.startsWith("*") && filter.endsWith("*")) {
                String search = filter.substring(1, filter.length() - 1);
                if (fullName.contains(search)) {
                    return true;
                }
            } else if (filter.startsWith("*")) {
                String search = filter.substring(1);
                if (fullName.endsWith(search)) {
                    return true;
                }
            } else if (filter.endsWith("*")) {
                String search = filter.substring(0, filter.length() - 1);
                if (fullName.startsWith(search)) {
                    return true;
                }
            }
        }
        return false;
    }
}

class HeldItemArmAdjustmentLists {
    private List<HeldItemArmAdjustment> held_item_arm_adjustments;

    // Getter and setter
    public List<HeldItemArmAdjustment> getHeldItemArmAdjustments() {
        return held_item_arm_adjustments;
    }

    public void setHeldItemArmAdjustments(List<HeldItemArmAdjustment> held_item_arm_adjustments) {
        this.held_item_arm_adjustments = held_item_arm_adjustments;
    }

    @Override
    public String toString() {
        return "RootObject{" +
                "held_item_arm_adjustments=" + held_item_arm_adjustments +
                '}';
    }
}

class HeldItemArmAdjustment {
    private List<String> filters;
    private Adjustment adjustment;
    private String only_if_mod_installed = "";

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public Adjustment getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(Adjustment adjustment) {
        this.adjustment = adjustment;
    }

    public String getOnly_if_mod_installed() {
        return only_if_mod_installed;
    }

    public void setOnly_if_mod_installed(String only_if_mod_installed) {
        this.only_if_mod_installed = only_if_mod_installed;
    }

    @Override
    public String toString() {
        return "HeldItemArmAdjustment{" +
                ", filters=" + filters +
                ", adjustment=" + adjustment +
                '}';
    }
}

class Adjustment {
    private String matchingHandX = "0";
    private String otherHandX = "0";
    private String matchingHandY = "0";
    private String otherHandY = "0";
    private String matchingHandZ = "0";
    private String otherHandZ = "0";

    // Getters and setters
    public String getmatchingHandX() {
        return matchingHandX;
    }

    public void setmatchingHandX(String matchingHandX) {
        this.matchingHandX = matchingHandX;
    }

    public String getotherHandX() {
        return otherHandX;
    }

    public void setotherHandX(String otherHandX) {
        this.otherHandX = otherHandX;
    }

    public String getmatchingHandY() {
        return matchingHandY;
    }

    public void setmatchingHandY(String matchingHandY) {
        this.matchingHandY = matchingHandY;
    }

    public String getotherHandY() {
        return otherHandY;
    }

    public void setotherHandY(String otherHandY) {
        this.otherHandY = otherHandY;
    }

    public String getmatchingHandZ() {
        return matchingHandZ;
    }

    public void setmatchingHandZ(String matchingHandZ) {
        this.matchingHandZ = matchingHandZ;
    }

    public String getotherHandZ() {
        return otherHandZ;
    }

    public void setotherHandZ(String otherHandZ) {
        this.otherHandZ = otherHandZ;
    }

    @Override
    public String toString() {
        return "Adjustment{" +
                "matchingHandX='" + matchingHandX + '\'' +
                ", otherHandX='" + otherHandX + '\'' +
                '}';
    }
}