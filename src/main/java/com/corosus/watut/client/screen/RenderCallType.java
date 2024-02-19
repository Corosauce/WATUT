package com.corosus.watut.client.screen;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum RenderCallType {
    INNER_BLIT,
    INNER_BLIT2;

    private static final Map<Integer, RenderCallType> lookup = new HashMap<>();

    static {
        for (RenderCallType e : EnumSet.allOf(RenderCallType.class)) {
            lookup.put(e.ordinal(), e);
        }
    }

    public static RenderCallType get(int intValue) {
        return lookup.get(intValue);
    }
}
