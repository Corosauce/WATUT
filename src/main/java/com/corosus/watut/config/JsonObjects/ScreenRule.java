package com.corosus.watut.config.JsonObjects;

import com.google.gson.annotations.SerializedName;

public class ScreenRule {
    @SerializedName("screen_class")
    private String screenClass;
    @SerializedName("render_type")
    private String renderType;
    private String texture;
    private int[] pos = {0, 0};
    private int[] size = {256, 256};
    @SerializedName("texture_size")
    private int[] textureSize = {256, 256};
    private float scale = 1;

    // getters and setters

    public  class RenderTypes {

        public static String ALL_TEXTURES = "all_textures";
        public static String BIGGEST_TEXTURE = "biggest_texture";
        public static String SINGLE_TEXTURE = "single_texture";
        public static String GENERIC_INVENTORY = "generic_inventory";

    }

    public String getScreenClass() {
        return screenClass;
    }

    public void setScreenClass(String screenClass) {
        this.screenClass = screenClass;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public int[] getPos() {
        return pos;
    }

    public void setPos(int[] pos) {
        this.pos = pos;
    }

    public int[] getSize() {
        return size;
    }

    public void setSize(int[] size) {
        this.size = size;
    }

    public int[] getTextureSize() {
        return textureSize;
    }

    public void setTextureSize(int[] textureSize) {
        this.textureSize = textureSize;
    }

    public String getRenderType() {
        return renderType;
    }

    public void setRenderType(String renderType) {
        this.renderType = renderType;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
}