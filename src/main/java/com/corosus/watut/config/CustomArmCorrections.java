package com.corosus.watut.config;

import com.corosus.coroutil.util.CULog;
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

public class CustomArmCorrections {

    private static HeldItemArmAdjustmentLists heldItemArmAdjustmentLists = null;

    public static boolean loadJsonConfigs() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("./config/" + WatutMod.configJSONName)) {
            heldItemArmAdjustmentLists = gson.fromJson(reader, HeldItemArmAdjustmentLists.class);
        } catch (IOException e) {
            CULog.err("FAILED TO LOAD watut-item-arm-adjustments.json, check its formatting!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static HeldItemArmAdjustmentLists getHeldItemArmAdjustmentLists() {
        return heldItemArmAdjustmentLists;
    }

    public static Vector3f getAdjustmentForArm(ItemStack stackMainArm, ItemStack stackotherHandArm, EquipmentSlot equipmentSlot) {
        if (getHeldItemArmAdjustmentLists() == null) return new Vector3f(0, 0, 0);
        try {
            for (HeldItemArmAdjustment heldItemArmAdjustment : getHeldItemArmAdjustmentLists().getHeldItemArmAdjustments()) {

                boolean shouldMatchmatchingHand = heldItemArmAdjustment.getAdjustment().getmatchingHandX() != "0" || heldItemArmAdjustment.getAdjustment().getmatchingHandY() != "0" || heldItemArmAdjustment.getAdjustment().getmatchingHandZ() != "0";
                boolean shouldMatchotherHand = heldItemArmAdjustment.getAdjustment().getotherHandX() != "0" || heldItemArmAdjustment.getAdjustment().getotherHandY() != "0" || heldItemArmAdjustment.getAdjustment().getotherHandZ() != "0";
                boolean matchmatchingHand = shouldMatchmatchingHand && filterMatches(heldItemArmAdjustment, stackMainArm);
                boolean matchotherHand = shouldMatchotherHand && filterMatches(heldItemArmAdjustment, stackotherHandArm);
                boolean modIDMatches = heldItemArmAdjustment.getOnly_if_mod_installed().equals("") || WatutMod.instance().isModInstalled(heldItemArmAdjustment.getOnly_if_mod_installed());
                boolean matchFound = (matchmatchingHand || matchotherHand) && modIDMatches;

                if (matchFound) {
                    float adjX = 0;
                    float adjY = 0;
                    float adjZ = 0;
                    if (matchmatchingHand) {
                        if (heldItemArmAdjustment.getAdjustment().getmatchingHandX().toLowerCase().startsWith("disable")) {
                            adjX = Float.MAX_VALUE;
                        } else {
                            adjX = Float.parseFloat(heldItemArmAdjustment.getAdjustment().getmatchingHandX());
                        }
                        if (heldItemArmAdjustment.getAdjustment().getmatchingHandY().toLowerCase().startsWith("disable")) {
                            adjY = Float.MAX_VALUE;
                        } else {
                            adjY = Float.parseFloat(heldItemArmAdjustment.getAdjustment().getmatchingHandY());
                        }
                        if (heldItemArmAdjustment.getAdjustment().getmatchingHandZ().toLowerCase().startsWith("disable")) {
                            adjZ = Float.MAX_VALUE;
                        } else {
                            adjZ = Float.parseFloat(heldItemArmAdjustment.getAdjustment().getmatchingHandZ());
                        }
                    } else if (matchotherHand) {
                        if (heldItemArmAdjustment.getAdjustment().getotherHandX().toLowerCase().startsWith("disable")) {
                            adjX = Float.MAX_VALUE;
                        } else {
                            adjX = Float.parseFloat(heldItemArmAdjustment.getAdjustment().getotherHandX());
                        }
                        if (heldItemArmAdjustment.getAdjustment().getotherHandY().toLowerCase().startsWith("disable")) {
                            adjY = Float.MAX_VALUE;
                        } else {
                            adjY = Float.parseFloat(heldItemArmAdjustment.getAdjustment().getotherHandY());
                        }
                        if (heldItemArmAdjustment.getAdjustment().getotherHandZ().toLowerCase().startsWith("disable")) {
                            adjZ = Float.MAX_VALUE;
                        } else {
                            adjZ = Float.parseFloat(heldItemArmAdjustment.getAdjustment().getotherHandZ());
                        }
                    }

                    return new Vector3f(adjX == Float.MAX_VALUE ? adjX : Mth.DEG_TO_RAD * adjX, adjY == Float.MAX_VALUE ? adjY : Mth.DEG_TO_RAD * adjY, adjZ == Float.MAX_VALUE ? adjZ : Mth.DEG_TO_RAD * adjZ);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return new Vector3f(0, 0, 0);
        }
        return new Vector3f(0, 0, 0);
    }

    private static boolean filterMatches(HeldItemArmAdjustment heldItemArmAdjustment, ItemStack stack) {
        //using OR logic
        boolean matchFound = false;
        for (String filter : heldItemArmAdjustment.getFilters()) {
            String fullname = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            if (fullname.matches("minecraft:air")) continue;

            String modID = fullname.split(":")[0];
            String name = fullname.split(":")[1];

            //mod id match check
            if (filter.contains("@")) {
                if (filter.substring(1).equals(modID)) {
                    matchFound = true;
                    break;
                }
            }

            //perfect match
            if (filter.equals(fullname)) {
                matchFound = true;
                break;
            }

            if (filter.startsWith("*") && filter.endsWith("*")) {
                String search = filter.replace("*", "").replace("*", "");
                if (fullname.contains(search)) {
                    matchFound = true;
                    break;
                }
            } else if (filter.startsWith("*")) {
                String search = filter.replace("*", "");
                if (fullname.endsWith(search)) {
                    matchFound = true;
                    break;
                }
            } else if (filter.endsWith("*")) {
                String search = filter.replace("*", "");
                if (fullname.startsWith(search)) {
                    matchFound = true;
                    break;
                }
            }
        }
        return matchFound;
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