package com.corosus.watut.config;

import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ConfigServerSyncHelper {

    private static ConfigServerSyncHelper instance;

    //private HashMap<Class, String> settingsTypes = new HashMap<>();
    private HashMap<String, Class> settingsTypes = new HashMap<>();
    private HashMap<String, Field> cache = new HashMap<>();

    public static ConfigServerSyncHelper getInstance() {
        if (instance == null) {
            instance = new ConfigServerSyncHelper();
            instance.init();
        }
        return instance;
    }

    private void init() {
        for (Field field : ConfigServerControlledSyncedToClient.class.getDeclaredFields()) {
            add(field);
        }
    }

    public CompoundTag getSyncableConfigOnServer() {
        CompoundTag nbt = new CompoundTag();
        try {
            for (Map.Entry<String, Class> entry : settingsTypes.entrySet()) {
                String name = entry.getKey();
                Class clazz = entry.getValue();
                Object value = getFieldValue(name);
                if (value != null) {
                    if (clazz == int.class) {
                        nbt.putInt(name, (Integer) value);
                    } else if (clazz == float.class) {
                        nbt.putFloat(name, (Float) value);
                    } else if (clazz == double.class) {
                        nbt.putDouble(name, (Double) value);
                    } else if (clazz == String.class) {
                        nbt.putString(name, (String) value);
                    } else if (clazz == boolean.class) {
                        nbt.putBoolean(name, (Boolean) value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nbt;
    }

    public void updateSyncableConfigOnClient(CompoundTag nbt) {
        try {
            for (Map.Entry<String, Class> entry : settingsTypes.entrySet()) {
                String name = entry.getKey();
                Class value = entry.getValue();
                if (value == int.class) {
                    setFieldValue(name, nbt.getIntOr(name, 0));
                } else if (value == float.class) {
                    setFieldValue(name, nbt.getFloatOr(name, 0f));
                } else if (value == double.class) {
                    setFieldValue(name, nbt.getDoubleOr(name, 0.0));
                } else if (value == String.class) {
                    setFieldValue(name, nbt.getStringOr(name, ""));
                } else if (value == boolean.class) {
                    setFieldValue(name, nbt.getBooleanOr(name, false));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setFieldValue(String name, Object value) {
        if (!cache.containsKey(name)) {
            try {
                Field field = ConfigServerControlledSyncedToClient.class.getDeclaredField(name);
                cache.put(name, field);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        try {
            cache.get(name).set(null, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Object getFieldValue(String name) {
        if (!cache.containsKey(name)) {
            try {
                Field field = ConfigServerControlledSyncedToClient.class.getDeclaredField(name);
                cache.put(name, field);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        try {
            return cache.get(name).get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void add(Field field) {
        settingsTypes.put(field.getName(), field.getType());
    }

}
