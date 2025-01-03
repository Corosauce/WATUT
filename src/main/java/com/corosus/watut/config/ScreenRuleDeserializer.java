package com.corosus.watut.config;

import com.corosus.watut.config.JsonObjects.ScreenRule;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

public class ScreenRuleDeserializer implements JsonDeserializer<ScreenRule> {
    @Override
    public ScreenRule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        ScreenRule screenRule = new ScreenRule();
        screenRule.setScreenClass(jsonObject.get("screen_class").getAsString());
        screenRule.setRenderType(jsonObject.get("render_type").getAsString());

        //optionals / context dependant
        if (jsonObject.has("texture")) screenRule.setTexture(jsonObject.get("texture").getAsString());
        if (jsonObject.has("pos")) screenRule.setPos(context.deserialize(jsonObject.get("pos"), int[].class));
        if (jsonObject.has("size")) screenRule.setSize(context.deserialize(jsonObject.get("size"), int[].class));
        if (jsonObject.has("scale")) screenRule.setScale(context.deserialize(jsonObject.get("scale"), float.class));
        return screenRule;
    }
}