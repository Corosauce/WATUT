package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
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

    //private boolean isCapturing = false;
    private List<RenderCall> listRenderCalls = new ArrayList<>();

    private String screenClass = "";

    public void init() {


    }

    public synchronized void startCapture() {
        listRenderCalls.clear();
        ScreenParticleRenderer.isCapturing = true;
        //System.out.println("capture started");

        if (Minecraft.getInstance().screen != null) {
            screenClass = Minecraft.getInstance().screen.getClass().getCanonicalName();
        } else {
            CULog.dbg("watut screen capture started but screen is null?");
        }
    }

    public synchronized void stopCapture() {
        ScreenParticleRenderer.isCapturing = false;
        //System.out.println("capture stopped - captured call count: " + listRenderCalls.size());
    }

    public void addRenderCall(RenderCall renderCall) {
        listRenderCalls.add(renderCall);
    }

    public synchronized boolean isCapturing() {
        return ScreenParticleRenderer.isCapturing;
    }

    public synchronized void setCapturing(boolean capturing) {
        ScreenParticleRenderer.isCapturing = capturing;
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
