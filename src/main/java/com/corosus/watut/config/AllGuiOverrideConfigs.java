package com.corosus.watut.config;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.config.JsonObjects.GuiOverrideConfigs;
import com.corosus.watut.config.JsonObjects.ScreenRule;

import java.util.HashMap;
import java.util.List;

public class AllGuiOverrideConfigs {
    private HashMap<String, ScreenRule> lookup = new HashMap<>();

    public void generateLookups(GuiOverrideConfigs configs) {
        for (ScreenRule rule : configs.getScreenRules()) {
            if (lookup.containsKey(rule.getScreenClass())) {
                System.out.println("WATUT MODDED GUI ERROR: screen rule already exists in file " + configs.getPath() + " for " + rule.getScreenClass());
            } else {
                lookup.put(rule.getScreenClass(), rule);
            }
        }
    }

    public ScreenRule getScreenRuleByClass(String screenClass) {
        return lookup.get(screenClass);
    }
}