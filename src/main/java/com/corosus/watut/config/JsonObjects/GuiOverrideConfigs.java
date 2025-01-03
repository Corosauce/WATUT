package com.corosus.watut.config.JsonObjects;

import com.corosus.watut.config.JsonObjects.ScreenRule;

import java.util.HashMap;
import java.util.List;

public class GuiOverrideConfigs {

    private String path;
    private List<ScreenRule> screen_rules;

    public List<ScreenRule> getScreenRules() {
        return screen_rules;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}