package com.corosus.watut.client.screen;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutMod;
import com.corosus.watut.client.input.InputTracker;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.corosus.watut.mixin.client.AbstractContainerScreenAccessor;
import com.corosus.watut.status.PlayerGuiState;
import com.corosus.watut.status.PlayerStatus;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Gestore sicuro ad alte prestazioni per la cattura e lo streaming dello schermo / GUI con proporzioni dinamiche esatte.
 */
public class DynamicScreenManager {

    private static final DynamicScreenManager INSTANCE = new DynamicScreenManager();
    private long lastCaptureGameTime = 0;
    private final CRC32 crc32 = new CRC32();
    private boolean isCapturing = false;

    public static DynamicScreenManager getInstance() {
        return INSTANCE;
    }

    public boolean isDynamicGuiEnabled() {
        if (ConfigServerControlledSyncedToClient.dynamicGuiUseOldSimpleGUIVisual) return false;
        if (ConfigClient.dontSendDetailedGUIInfo) return false;
        return true;
    }

    public boolean isValidScreenForCapture(Screen screen) {
        if (screen == null) return false;
        if (screen instanceof ChatScreen || screen instanceof LevelLoadingScreen) return false;
        PlayerGuiState state = WatutMod.getPlayerStatusManagerClient().getStatusLocal().getPlayerGuiState();
        return state != PlayerGuiState.NONE && state != PlayerGuiState.CHAT_SCREEN;
    }

    public void tryCaptureCurrentScreen() {
        if (!isDynamicGuiEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null || mc.gameRenderer.mainRenderTarget() == null) return;

        Screen screen = InputTracker.getCurrentScreen();
        if (!isValidScreenForCapture(screen)) return;

        long gameTime = mc.level.getGameTime();
        int tickRate = Math.max(4, ConfigServerControlledSyncedToClient.dynamicGuiTickSendRateOfGUIUpdates);

        if (gameTime < lastCaptureGameTime + tickRate) {
            return;
        }

        if (!hasNearbyObservingPlayers(mc)) {
            return;
        }

        if (isCapturing) return;
        isCapturing = true;
        lastCaptureGameTime = gameTime;

        try {
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), fullImage -> {
                try {
                    if (fullImage != null && !fullImage.isClosed()) {
                        processCapturedImage(fullImage, screen);
                    }
                } catch (Exception ex) {
                    CULog.dbg("Watut error in screenshot callback: " + ex.getMessage());
                } finally {
                    if (fullImage != null && !fullImage.isClosed()) {
                        fullImage.close();
                    }
                    isCapturing = false;
                }
            });
        } catch (Exception ex) {
            isCapturing = false;
            CULog.dbg("Watut: error triggering screenshot capture: " + ex.getMessage());
        }
    }

    private void processCapturedImage(NativeImage fullImage, Screen screen) {
        int fullW = fullImage.getWidth();
        int fullH = fullImage.getHeight();

        Minecraft mc = Minecraft.getInstance();
        double guiScale = mc.getWindow().getGuiScale();

        int srcX, srcY, srcW, srcH;
        int guiAspectW, guiAspectH;

        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) containerScreen;
            int guiLeft = accessor.getLeftPos();
            int guiTop = accessor.getTopPos();
            int guiWidth = Math.max(16, accessor.getImageWidth());
            int guiHeight = Math.max(16, accessor.getImageHeight());

            guiAspectW = guiWidth;
            guiAspectH = guiHeight;

            int physLeft = (int) Math.round(guiLeft * guiScale);
            int physTop = (int) Math.round(guiTop * guiScale);
            int physWidth = (int) Math.round(guiWidth * guiScale);
            int physHeight = (int) Math.round(guiHeight * guiScale);

            srcX = Math.max(0, Math.min(physLeft, fullW - 1));
            srcY = Math.max(0, Math.min(physTop, fullH - 1));
            srcW = Math.max(16, Math.min(physWidth, fullW - srcX));
            srcH = Math.max(16, Math.min(physHeight, fullH - srcY));
        } else {
            int srcBoxW = Math.min(fullW, (int) (fullW * 0.65f));
            int srcBoxH = Math.min(fullH, (int) (fullH * 0.65f));
            srcX = (fullW - srcBoxW) / 2;
            srcY = (fullH - srcBoxH) / 2;
            srcW = srcBoxW;
            srcH = srcBoxH;
            guiAspectW = 256;
            guiAspectH = 256;
        }

        int targetRes = 256;
        NativeImage cropped = new NativeImage(NativeImage.Format.RGBA, targetRes, targetRes, false);
        fullImage.resizeSubRectTo(srcX, srcY, srcW, srcH, cropped);

        ByteBuffer pixelBuffer = cropped.getPixelBytes();
        pixelBuffer.rewind();

        crc32.reset();
        crc32.update(pixelBuffer);
        long currentHash = crc32.getValue();
        pixelBuffer.rewind();

        PlayerStatus localStatus = WatutMod.getPlayerStatusManagerClient().getStatusLocal();
        ScreenData screenData = localStatus.getScreenData();

        if (currentHash != screenData.getLastFrameHash() || screenData.getLastScreen() != screen
                || screenData.getWidth() != guiAspectW || screenData.getHeight() != guiAspectH) {

            screenData.setLastFrameHash(currentHash);
            screenData.setLastScreen(screen);
            screenData.setWidth(guiAspectW);
            screenData.setHeight(guiAspectH);

            byte[] bytes = new byte[pixelBuffer.remaining()];
            pixelBuffer.get(bytes);
            ByteBuffer copy = ByteBuffer.wrap(bytes);

            RenderHelper.processor.submitForProcessing(copy);
        }

        cropped.close();
    }

    private boolean hasNearbyObservingPlayers(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return false;
        double maxDist = ConfigServerControlledSyncedToClient.distanceRequiredToShowGUIInfo;
        Vec3 pos = mc.player.position();

        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (info.getProfile() != null && !info.getProfile().id().equals(mc.player.getUUID())) {
                var other = mc.level.getPlayerByUUID(info.getProfile().id());
                if (other != null && other.position().distanceTo(pos) <= maxDist) {
                    return true;
                }
            }
        }
        return false;
    }
}
