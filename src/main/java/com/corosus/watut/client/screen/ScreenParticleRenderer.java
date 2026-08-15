package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.blaze3d.pipeline.MainTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class ScreenParticleRenderer {

    public static boolean isRenderingParticleGUI = false;
    public static boolean isRenderingParticleGUI2 = false;

    //used on client with gui open side
    //used to capture raw copy of minecraft screen
    private MainTarget mainRenderTarget;

    //used to render a sized down and cropped version of the above raw copy
    private MainTarget mainRenderTargetScaledDown;

    public int width;
    public int height;
    public static int defaultWidthScaledDown = 256;
    public static int defaultHeightScaledDown = 256;
    public static int bytesPerPixel = 4;
    public int widthScaledDown = defaultWidthScaledDown;
    public int heightScaledDown = defaultHeightScaledDown;
    public boolean needsInit = true;

    private static ScreenParticleRenderer instance;

    public static ScreenParticleRenderer getInstance() {
        if (instance == null) {
            instance = new ScreenParticleRenderer();
        }
        return instance;
    }

    public void checkSetup() {
        if (needsInit) {
            needsInit = false;
            setup();
        }
    }

    public void setup() {
        Minecraft mc = Minecraft.getInstance();
        width = mc.getWindow().getWidth();
        height = mc.getWindow().getHeight();
        mainRenderTarget = new MainTarget(width, height);

        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            widthScaledDown = width;
            heightScaledDown = height;
        } else {
            widthScaledDown = defaultWidthScaledDown;
            heightScaledDown = defaultHeightScaledDown;
        }

        mainRenderTargetScaledDown = new MainTarget(widthScaledDown, heightScaledDown);
    }

    public synchronized void resize(int width, int height) {
        this.width = width;
        this.height = height;
        checkSetup();
        if (mainRenderTarget != null) {
            mainRenderTarget.resize(width, height);
        }
        resizeScaledDown(width, height);
    }

    public void resizeScaledDown(int width, int height) {
        int widthToUse = defaultWidthScaledDown;
        int heightToUse = defaultHeightScaledDown;
        if (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen) {
            widthToUse = width;
            heightToUse = height;
        }

        widthScaledDown = widthToUse;
        heightScaledDown = heightToUse;

        CULog.dbg("resizeScaledDown to " + widthToUse + " " + heightToUse);

        if (mainRenderTargetScaledDown != null && (mainRenderTargetScaledDown.width != widthToUse || mainRenderTargetScaledDown.height != heightToUse)) {
            mainRenderTargetScaledDown.resize(widthToUse, heightToUse);
        }
    }

    public void bind() {
    }

    public void unbind() {
    }

    public void bindScaledDown() {
    }

    public void unbindScaledDown() {
    }

    public MainTarget getMainRenderTarget() {
        return mainRenderTarget;
    }

    public MainTarget getMainRenderTargetScaledDown() {
        return mainRenderTargetScaledDown;
    }

    public void setMainRenderTarget(MainTarget mainRenderTarget) {
        this.mainRenderTarget = mainRenderTarget;
    }
}
