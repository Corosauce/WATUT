package com.corosus.watut.client.screen;

import com.corosus.watut.config.JSONLoader;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;

import java.util.ArrayList;
import java.util.List;

public class ScreenData {

    private boolean isCapturing = false;
    private List<RenderCall> listRenderCalls = new ArrayList<>();

    private String screenClass = "";

    public void init() {


    }

    public void startCapture() {
        listRenderCalls.clear();
        isCapturing = true;
        //System.out.println("capture started");

        if (Minecraft.getInstance().screen != null) {
            screenClass = Minecraft.getInstance().screen.getClass().getCanonicalName();
        }
    }

    public void stopCapture() {
        isCapturing = false;
        //System.out.println("capture stopped - captured call count: " + listRenderCalls.size());
    }

    public void addRenderCall(RenderCall renderCall) {
        listRenderCalls.add(renderCall);
    }

    public boolean isCapturing() {
        return isCapturing;
    }

    public void setCapturing(boolean capturing) {
        isCapturing = capturing;
    }

    public List<RenderCall> getListRenderCalls() {
        return listRenderCalls;
    }

    public String getScreenClass() {
        return screenClass;
    }

    public void setScreenClass(String screenClass) {
        this.screenClass = screenClass;
    }
}
