package com.corosus.watut;

import com.corosus.watut.client.CustomParticleEngine;
import com.corosus.watut.client.screen.RenderHelper;
import com.corosus.coroutil.util.CULog;
import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.corosus.watut.config.*;
import com.corosus.watut.math.Lerpables;
import com.corosus.watut.particle.*;
import com.ibm.icu.impl.Pair;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.util.*;

public class PlayerStatusManagerClient extends PlayerStatusManager {

    //selfPlayer statuses are important, a use case: tracking things to send packets for locally,
    //then we allow for packet to be sent to self as well, where we also use the lookup to then compare previous state so we can correctly setup pose for self as well as others
    //local use case:
    //idle state tracking and comparison when it changes from selfPlayer previous
    //remote use case:
    //idle state pose setup when it changes from NON selfPlayer previous, setting up lerp
    private PlayerStatus selfPlayerStatus = new PlayerStatus(PlayerStatus.PlayerGuiState.NONE);
    private PlayerStatus selfPlayerStatusPrev = new PlayerStatus(PlayerStatus.PlayerGuiState.NONE);

    public HashMap<UUID, PlayerStatus> lookupPlayerToStatusPrev = new HashMap<>();
    private long typingIdleTimeout = 60;
    private int armMouseTickRate = 5;
    private int typeRatePollCounter = 0;

    //using gametime is buggy on client, gets synced, skips ticks, etc, this is better
    private int steadyTickCounter = 0;
    private int forcedSyncRate = 40;

    private Level lastLevel;

    private boolean wasMousePressed = false;
    private int mousePressedCountdown = 0;

    public static ShaderInstanceBlur positionTexBlur;
    public static ShaderInstanceBlur positionTexBlurHorizontal;
    public static ShaderInstanceBlur positionTexBlurVertical;
    public static ShaderInstanceBlur particle;

    private static CustomParticleEngine customParticleEngine;

    public static CustomParticleEngine getParticleEngine() {
        if (customParticleEngine == null) {
            customParticleEngine = new CustomParticleEngine(Minecraft.getInstance().level, Minecraft.getInstance().getTextureManager());
            ((ReloadableResourceManager)Minecraft.getInstance().getResourceManager()).registerReloadListener(customParticleEngine);
        }
        return customParticleEngine;
    }

    public void tickGame() {
        steadyTickCounter++;
        if (steadyTickCounter == Integer.MAX_VALUE) steadyTickCounter = 0;
        Level level = Minecraft.getInstance().level;

        //reset any players data that disconnected
        if (Minecraft.getInstance().getConnection() != null) {
            for (Iterator<Map.Entry<UUID, PlayerStatus>> it = lookupPlayerToStatus.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, PlayerStatus> entry = it.next();
                PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(entry.getKey());
                PlayerStatus playerStatus = entry.getValue();
                if (playerInfo == null) {
                    WatutMod.dbg("remove playerstatus for no longer existing player: " + entry.getKey());
                    playerStatus.reset();
                    it.remove();
                }

                //reset players particles when dimension travelled / not in current world (avoid stuck particles), keep their other data though
                if (level.getPlayerByUUID(entry.getKey()) == null) {
                    if (playerStatus.getParticle() != null || playerStatus.getParticleIdle() != null) {
                        WatutMod.dbg("remove player particles for player outside dimension: " + entry.getKey());
                        playerStatus.resetParticles();
                    }
                }
            }
        }

        //reset all particles when self dimension travelled
        if (lastLevel != level) {
            WatutMod.dbg("resetting player status");
            for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
                WatutMod.dbg("reset player particles for " + entry.getKey() + " hash: " + entry.getValue());
                entry.getValue().resetParticles();
            }
            selfPlayerStatus.reset();
            selfPlayerStatusPrev.reset();
            selfPlayerStatus.getScreenData().setGameTicksSinceLastScreenSend(0);
        }
        lastLevel = level;

        long gameTime = 0;
        if (Minecraft.getInstance().level != null) {
            gameTime = Minecraft.getInstance().level.getGameTime();
        }

        if (/*!selfPlayerStatus.isIdle()*/selfPlayerStatus.getTicksSinceLastAction() < (20 * 5)) {
            if (selfPlayerStatus.getScreenData().getGameTicksSinceLastScreenSend() + ConfigServerControlledSyncedToClient.dynamicGuiTickSendRateOfGUIUpdates < gameTime) {
                if (Minecraft.getInstance().screen != null && selfPlayerStatus.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE && selfPlayerStatus.getPlayerGuiState() != PlayerStatus.PlayerGuiState.CHAT_SCREEN) {
                    //System.out.println("? " + selfPlayerStatus.getScreenData().getLastScreen());
                    if (!ConfigServerControlledSyncedToClient.dynamicGuiDontSendConstantGUIUpdates || selfPlayerStatus.getScreenData().getLastScreen() != Minecraft.getInstance().screen) {
                        selfPlayerStatus.getScreenData().setGameTicksSinceLastScreenSend(gameTime);
                        selfPlayerStatus.getScreenData().setNeedsNewRenderToPixelData(true);

                    }
                }
                selfPlayerStatus.getScreenData().setLastScreen(Minecraft.getInstance().screen);
            }

            if (Minecraft.getInstance().screen == null) {
                selfPlayerStatus.getScreenData().setGameTicksSinceLastScreenSend(0);
            }
        }

        /*if (JSONLoader.RELOAD_LIVE_OFTEN && level != null && level.getGameTime() % 100 == 0) {
            JSONLoader.getInstance().loadFiles();
        }*/
    }

    public void tickPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player.getUUID().equals(player.getUUID())) {
            tickLocalPlayerClient(player);
        }

        tickOtherPlayerClient(player);
        getStatus(player.getUUID()).tick();

        PlayerStatus status = getStatus(player);
        float adjRateTyping = 0.1F;
        if (status.getTypingAmplifierSmooth() < status.getTypingAmplifier() - adjRateTyping) {
            status.setTypingAmplifierSmooth(status.getTypingAmplifierSmooth() + adjRateTyping);
        } else if (status.getTypingAmplifierSmooth() > status.getTypingAmplifier() + adjRateTyping) {
            status.setTypingAmplifierSmooth(status.getTypingAmplifierSmooth() - adjRateTyping);
        }
    }

    public void tickLocalPlayerClient(Player player) {
        Minecraft mc = Minecraft.getInstance();
        PlayerStatus statusLocal = getStatusLocal();
        PlayerStatus statusPrevLocal = getStatusPrevLocal();

        statusPrevLocal.setPlayerGuiState(statusLocal.getPlayerGuiState());

        if (ConfigClient.sendActiveGui && !statusLocal.isIdle()) {
            if (mc.screen instanceof ChatScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.CHAT_SCREEN);
                //sendGuiStatus(PlayerStatus.PlayerGuiState.ENCHANTING_TABLE);
            } else if (mc.screen instanceof EffectRenderingInventoryScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.INVENTORY);
            } else if (mc.screen instanceof CraftingScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.CRAFTING);
            } else if (mc.screen instanceof PauseScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.ESCAPE);
            } else if (mc.screen instanceof BookEditScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.EDIT_BOOK);
            } else if (mc.screen instanceof AbstractSignEditScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.EDIT_SIGN);
            } else if (mc.screen instanceof ContainerScreen || mc.screen instanceof ShulkerBoxScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.CHEST);
            } else if (mc.screen instanceof EnchantmentScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.ENCHANTING_TABLE);
            } else if (mc.screen instanceof AnvilScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.ANVIL);
            } else if (mc.screen instanceof BeaconScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.BEACON);
            } else if (mc.screen instanceof BrewingStandScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.BREWING_STAND);
            } else if (mc.screen instanceof DispenserScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.DISPENSER);
            } else if (mc.screen instanceof AbstractFurnaceScreen<?>) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.FURNACE);
            } else if (mc.screen instanceof GrindstoneScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.GRINDSTONE);
            } else if (mc.screen instanceof HopperScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.HOPPER);
            } else if (mc.screen instanceof HorseInventoryScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.HORSE);
            } else if (mc.screen instanceof LoomScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.LOOM);
            } else if (mc.screen instanceof MerchantScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.VILLAGER);
            } else if (mc.screen instanceof AbstractCommandBlockEditScreen) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.COMMAND_BLOCK);
            } else if (mc.screen != null && !(mc.screen instanceof DeathScreen)) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.MISC);
            } else if (mc.screen == null) {
                sendGuiStatus(PlayerStatus.PlayerGuiState.NONE);
                //Watut.dbg(mc.gui);
            }
        } else {
            sendGuiStatus(PlayerStatus.PlayerGuiState.NONE);
        }

        //update typing state
        String chatText = "";
        if (mc.screen instanceof ChatScreen chatScreen) {
            chatText = chatScreen.input.getValue();
        } else if (mc.screen instanceof BookEditScreen bookEditScreen) {
            chatText = bookEditScreen.pageEdit.getMessageFn.get();
        } else if (mc.screen instanceof AbstractSignEditScreen abstractSignEditScreen) {
            chatText = abstractSignEditScreen.signField.getMessageFn.get();
        } else if (mc.screen instanceof AbstractCommandBlockEditScreen abstractCommandBlockEditScreen) {
            //note, the first tick this is open, chatText is blank, next tick contains the correct data
            chatText = abstractCommandBlockEditScreen.commandEdit.getValue();
        }

        if (checkIfTyping(chatText, player)) {
            sendChatStatus(PlayerStatus.PlayerChatState.CHAT_TYPING);
        } else if (isGuiFocusedOnTextBox(mc.screen)) {
            sendChatStatus(PlayerStatus.PlayerChatState.CHAT_FOCUSED);
        } else {
            sendChatStatus(PlayerStatus.PlayerChatState.NONE);
        }

        if (ConfigClient.sendMouseInfo && mc.screen != null && mc.level.getGameTime() % (armMouseTickRate) == 0) {
            PlayerStatus.PlayerGuiState playerGuiState = statusLocal.getPlayerGuiState();
            //this wont trigger if theyre already idle, might be a good thing, just prevent idle, dont bring back from idle
            if (PlayerStatus.PlayerGuiState.canPreventIdleInGui(playerGuiState)) {
                Pair<Float, Float> pos = getMousePos();
                if (pos.first != statusLocal.getScreenPosPercentX() || pos.second != statusLocal.getScreenPosPercentY()) {
                    onAction();
                }
                sendMouse(getMousePos(), statusLocal.isPressing());
            }
        }

        if (statusPrevLocal.getTicksSinceLastAction() != statusLocal.getTicksSinceLastAction()) {
            statusPrevLocal.setTicksSinceLastAction(statusLocal.getTicksSinceLastAction());
        }

        if (ConfigClient.sendIdleState) {
            statusLocal.setTicksSinceLastAction(statusLocal.getTicksSinceLastAction()+1);
            //tickSyncing mostly handles this, but if they JUST went idle, send the packet right away
            if (statusLocal.getTicksSinceLastAction() > statusLocal.getTicksToMarkPlayerIdleSyncedForClient()) {
                //System.out.println("receive idle ticks from server: " + ticksIdle + " for " + player.getUUID());
                if (statusLocal.isIdle() != statusPrevLocal.isIdle()) {
                    WatutMod.dbg("send idle getTicksSinceLastAction: " + statusLocal.getTicksSinceLastAction() + " - " + statusPrevLocal.getTicksSinceLastAction());
                    sendIdle(statusLocal);
                }
            }
        } else {
            statusLocal.setTicksSinceLastAction(0);
        }

        //System.out.println("wasMousePressed " + wasMousePressed);

        if (!wasMousePressed && mousePressedCountdown > 0) {
            //System.out.println("mousePressedCountdown: " + mousePressedCountdown);
            mousePressedCountdown--;
            if (mousePressedCountdown == 0) {
                sendMouse(getMousePos(), false);
            }
        }

        tickSyncing(player);


    }

    public boolean isGuiFocusedOnTextBox(Screen screen) {
        if (isGuiForceTypeFocused(screen)) return true;
        //TODO: screen support with optional text boxes eg creative gui, JEI, ????
        return false;
    }

    public boolean isGuiForceTypeFocused(Screen screen) {
        if (screen instanceof ChatScreen
                || screen instanceof AbstractSignEditScreen
                || screen instanceof BookEditScreen
                || screen instanceof AbstractCommandBlockEditScreen) {
            return true;
        }
        return false;
    }

    /**
     * Occasional syncing, force send state so server refreshes their side to other clients too
     *
     * this is to solve the reported problems of idle and typing states getting stuck on clients
     */
    public void tickSyncing(Player player) {
        if (steadyTickCounter % forcedSyncRate == 0) {
            PlayerStatus playerStatusLocal = getStatusLocal();
            sendIdle(playerStatusLocal);
            sendGuiStatus(playerStatusLocal.getPlayerGuiState(), true);
            sendTyping(playerStatusLocal);
        }
    }

    public void onMouse(boolean pressedAnything) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.player.level() != null) {
            PlayerStatus.PlayerGuiState playerGuiState = getStatus(mc.player).getPlayerGuiState();
            if (ConfigClient.sendMouseInfo) {
                if (PlayerStatus.PlayerGuiState.canPreventIdleInGui(playerGuiState)) {
                    if (pressedAnything) {
                        mousePressedCountdown = 3;
                        wasMousePressed = true;
                    } else {
                        wasMousePressed = false;
                    }
                    sendMouse(getMousePos(), mousePressedCountdown > 0);
                }
            }

            //this wont trigger if theyre already idle, might be a good thing, just prevent idle, dont bring back from idle
            if (mc.screen == null || (pressedAnything && (PlayerStatus.PlayerGuiState.canPreventIdleInGui(playerGuiState)))) {
                onAction();
            }
        }
    }

    public void onKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.player.level() != null) {
            if (mc.screen == null || isGuiFocusedOnTextBox(mc.screen)) {
                onAction();
            }
        }
    }

    public void onAction() {
        if (!ConfigClient.sendIdleState) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.player.level() != null) {
            PlayerStatus statusLocal = getStatusLocal();
            if (statusLocal.isIdle()) {
                statusLocal.setTicksSinceLastAction(0);
                WatutMod.dbg("send idle: " + 0);
                sendIdle(statusLocal);
            } else {
                statusLocal.setTicksSinceLastAction(0);
            }
        }
    }

    public Pair<Float, Float> getMousePos() {
        Minecraft mc = Minecraft.getInstance();
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        //not actual max, just doing some guestimation work
        double guiScaleMax = 4;
        double xPercent = (mc.mouseHandler.xpos() / mc.getWindow().getScreenWidth()) - 0.5;
        double yPercent = (mc.mouseHandler.ypos() / mc.getWindow().getScreenHeight()) - 0.5;

        //TODO: factor in clients gui scale, aka a ratio of gui covering screen, adjust hand move scale accordingly
        //i wanted to do this, but its not possible without other comprimises, eg:
        //journeymap ignores guiscale
        //if i used a baseline of a typical gui windows with a 176 width, and factored in gui scale, i could maybe pull it off
        //but then JEI is still off the side where id have capped the mouse movement because on gui scale one there is SO much extra room
        //maybe just do a lazy gui scale 4 / actual gui scale and hope thats good enough
        //yep seems good enough

        //emphasize the movements
        //double emphasis = 1.5;
        //double emphasis = 1.0;
        double emphasis = guiScaleMax / guiScale;
        double edgeLimit = 0.75;
        double edgeLimitYLower = 0.2;
        xPercent *= emphasis;
        yPercent *= emphasis;
        xPercent = Math.max(Math.min(xPercent, edgeLimit), -edgeLimit);
        yPercent = Math.max(Math.min(yPercent, edgeLimit), -edgeLimit);
        //prevent hand in pants
        //yPercent = Math.min(yPercent, edgeLimitYLower);
        return Pair.of((float) xPercent, (float) yPercent);
    }

    public boolean checkIfTyping(String input, Player player) {
        PlayerStatus statusLocal = getStatusLocal();
        PlayerStatus statusPrevLocal = getStatusPrevLocal();

        //prevent instant typing when opening books, signs, etc
        //note, for command blocks this doesnt work, because the first tick chatText is blank, next tick contains the correct data
        if (statusPrevLocal.getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE &&
                statusLocal.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE) {
            statusLocal.setLastTypeString(input);
            statusLocal.setLastTypeStringForAmp(input);
            statusLocal.setTypingAmplifier(0);
            statusLocal.setLastTypeDiff(0);
            statusLocal.setLastTypeTime(0);
            /*sendChatStatus(PlayerStatus.PlayerChatState.CHAT_FOCUSED);
            sendTyping(statusLocal);
            System.out.println("reset type: " + input);*/
        }

        typeRatePollCounter++;
        if (input.length() > 0) {
            //System.out.println(input + " - " + statusLocal.getTypingAmplifier());
            if (!input.startsWith("/")) {
                if (!input.equals(statusLocal.getLastTypeString())) {
                    statusLocal.setLastTypeString(input);
                    statusLocal.setLastTypeTime(player.level().getGameTime());
                }

                if (typeRatePollCounter >= 10) {
                    typeRatePollCounter = 0;
                    int lengthPrev = statusLocal.getLastTypeStringForAmp().length();
                    if (!input.equals(statusLocal.getLastTypeStringForAmp())) {
                        statusLocal.setLastTypeStringForAmp(input);
                        statusLocal.setLastTypeTimeForAmp(player.level().getGameTime());
                        int length = input.length();
                        int newDiff = length - lengthPrev;
                        //cap amp to 8
                        float amp = Math.max(0, Math.min(8, (newDiff / (float)6) * 2F));
                        if (ConfigClient.sendTypingSpeed) {
                            statusLocal.setTypingAmplifier(amp);
                        } else {
                            statusLocal.setTypingAmplifier(1F);
                        }
                        sendTyping(statusLocal);
                    } else {
                        if (ConfigClient.sendTypingSpeed) statusLocal.setTypingAmplifier(0);
                    }
                }

            }
        } else {
            statusLocal.setLastTypeString(input);
            statusLocal.setLastTypeDiff(0);
            return false;
        }
        if (statusLocal.getLastTypeTime() + typingIdleTimeout >= player.level().getGameTime()) {
            return true;
        }
        return false;
    }

    public void onGuiRender() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChatScreen && mc.getConnection() != null && ConfigClient.screenTypingVisible && ConfigServerControlledSyncedToClient.screenTypingVisible) {
            ChatScreen chat = (ChatScreen) mc.screen;
            GuiGraphics guigraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            int height = chat.height + 26;
            guigraphics.drawString(mc.font, WatutMod.getPlayerStatusManagerClient().getTypingPlayers(), 2 + ConfigClient.screenTypingRelativePosition_X, height - 50 + ConfigClient.screenTypingRelativePosition_Y, 16777215);
            guigraphics.flush();
        }
    }

    public String getTypingPlayers() {
        Minecraft mc = Minecraft.getInstance();
        String str = "";

        for (Map.Entry<UUID, PlayerStatus> entry : lookupPlayerToStatus.entrySet()) {
            if (entry.getValue().getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_SCREEN
                    && entry.getValue().getPlayerChatState() == PlayerStatus.PlayerChatState.CHAT_TYPING) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(entry.getKey());
                if (info != null) {
                    GameProfile profile = info.getProfile();
                    if (profile != null) {
                        str += profile.getName() + ", ";
                    }
                }
            }
        }

        int playersLengthStr = str.length();
        String anim = "";
        int animRate = 6;
        long time = mc.level.getGameTime() % (animRate*4);
        while (time > animRate) {
            time -= animRate;
            anim += ".";
        }

        if (playersLengthStr > ConfigClient.screenTypingCharacterLimit) {
            str = ConfigClient.screenTypingMultiplePlayersText + anim;
        } else if (str.length() > 2) {
            str = str.substring(0, str.length() - 2) + ConfigClient.screenTypingText + anim;
        }
        return str;
    }

    public boolean shouldAnimate(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return player != mc.player || !mc.options.getCameraType().isFirstPerson();
    }

    public void tickOtherPlayerClient(Player player) {
        PlayerStatus playerStatus = getStatus(player);
        PlayerStatus playerStatusPrev = getStatusPrev(player);

        long stableTime = steadyTickCounter;
        float sin = (float) Math.sin((stableTime / 30F) % 360);
        float cos = (float) Math.cos((stableTime / 30F) % 360);
        float idleY = (float) (2.6 + (cos * 0.03F));

        boolean idleParticleChangeOrGone = playerStatus.isIdle() != playerStatusPrev.isIdle() || playerStatus.getParticleIdle() == null;
        boolean statusParticleChangeOrGone = playerStatus.getPlayerGuiState() != playerStatusPrev.getPlayerGuiState() || playerStatus.getParticle() == null;
        boolean statusChatParticleChangeOrGone = playerStatus.getPlayerChatState() != playerStatusPrev.getPlayerChatState() && playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHAT_SCREEN;

        if (idleParticleChangeOrGone || !playerStatus.isIdle()) {
            if (playerStatus.getParticleIdle() != null) {
                playerStatus.getParticleIdle().remove();
                playerStatus.setParticleIdle(null);
            }
        }
        if (statusParticleChangeOrGone || statusChatParticleChangeOrGone || playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
            if (playerStatus.getParticle() != null) {
                playerStatus.getParticle().remove();
                playerStatus.setParticle(null);
            }
        }

        if (playerStatus.getParticle() != null && !playerStatus.getParticle().isAlive()) {
            playerStatus.getParticle().remove();
            playerStatus.setParticle(null);
        }

        if (playerStatus.getParticleIdle() != null && !playerStatus.getParticleIdle().isAlive()) {
            playerStatus.getParticleIdle().remove();
            playerStatus.setParticleIdle(null);
        }

        double quadSize = 0.3F + Math.sin((stableTime / 10F) % 360) * 0.01F;

        if (shouldAnimate(player) && !player.isInvisible()) {
            if (idleParticleChangeOrGone) {
                if (ConfigClient.showIdleStatesInPlayerAboveHead && ConfigServerControlledSyncedToClient.showIdleStatesInPlayerAboveHead && playerStatus.isIdle()) {
                    ParticleRotating particle = new ParticleStatic((ClientLevel) player.level(), player.position().x, player.position().y + idleY, player.position().z, ParticleRegistry.idle.getSprite());
                    if (particle != null) {
                        playerStatus.setParticleIdle(particle);
                        //if (particle instanceof ParticleDynamic) {
                            getParticleEngine().add(particle);
                        /*} else {
                            Minecraft.getInstance().particleEngine.add(particle);
                        }*/
                        particle.setQuadSize((float) quadSize);

                        WatutMod.dbg("spawning idle particle for " + player.getUUID());
                    }
                }
            }
            if (statusParticleChangeOrGone || statusChatParticleChangeOrGone) {
                ParticleRotating particle = null;
                Vec3 posParticle = getParticlePosition(player);

                boolean useChatIdleForTestingOtherGUIs = true;
                boolean newRender = RenderHelper.useDynamicGUISystem() && !playerStatus.isPlayerGuiDontSendDetailedGUIInfo();

                if (ConfigClient.showPlayerActiveChatGui && ConfigServerControlledSyncedToClient.showPlayerActiveChatGui) {
                    if (PlayerStatus.PlayerGuiState.isTypingGui(this.getStatus(player).getPlayerGuiState())) {
                        if (this.getStatus(player).getPlayerChatState() == PlayerStatus.PlayerChatState.CHAT_FOCUSED) {
                            particle = new ParticleAnimated((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chat_idle.getSpriteSet());
                            if (useChatIdleForTestingOtherGUIs) {
                                if (newRender && playerStatus.getScreenData().getParticleRenderType() != null) {
                                    particle = new ParticleDynamic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, playerStatus.getScreenData().getParticleRenderType(), 0.7F);
                                } else {
                                    particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.inventory.getSpriteSet());
                                }
                            }
                        } else if (this.getStatus(player).getPlayerChatState() == PlayerStatus.PlayerChatState.CHAT_TYPING) {
                            particle = new ParticleAnimated((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chat_typing.getSpriteSet());
                        }
                    }
                }
                if (ConfigClient.showPlayerActiveNonChatGui && ConfigServerControlledSyncedToClient.showPlayerActiveNonChatGui) {
                    TextureAtlasSprite sprite = null;
                    float brightness = 0.7F;
                    int subSizeX = 0;
                    int subSizeY = 0;
                    if (newRender) {
                        if (this.getStatus(player).getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE && this.getStatus(player).getPlayerGuiState() != PlayerStatus.PlayerGuiState.CHAT_SCREEN) {
                            if (playerStatus.getScreenData().getParticleRenderType() != null) {
                                particle = new ParticleDynamic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, playerStatus.getScreenData().getParticleRenderType(), 0.7F);
                            }
                        }
                    } else {
                        if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.INVENTORY) {
                            particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.inventory.getSpriteSet());
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.CRAFTING) {
                            particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.crafting.getSpriteSet());
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.ESCAPE) {
                            particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.escape.getSpriteSet());
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.CHEST) {
                            particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chest.getSpriteSet());
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.EDIT_BOOK) {
                            //particle = new ParticleStatic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.book.getSprite(), 0.7F);
                            if (playerStatus.getScreenData().getParticleRenderType() != null) {
                                particle = new ParticleDynamic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, playerStatus.getScreenData().getParticleRenderType(), 0.7F);
                            }
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.EDIT_SIGN) {
                            particle = new ParticleStatic((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.sign.getSprite(), 0.7F);
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.ENCHANTING_TABLE) {
                            sprite = ParticleRegistry.enchanting_table.getSprite();
                            subSizeX = 176;
                            subSizeY = 166;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.ANVIL) {
                            sprite = ParticleRegistry.anvil.getSprite();
                            subSizeX = 176;
                            subSizeY = 166;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.BEACON) {
                            sprite = ParticleRegistry.beacon.getSprite();
                            subSizeX = 231;
                            subSizeY = 219;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.BREWING_STAND) {
                            sprite = ParticleRegistry.brewing_stand.getSprite();
                            subSizeX = 176;
                            subSizeY = 166;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.DISPENSER) {
                            sprite = ParticleRegistry.dispenser.getSprite();
                            subSizeX = 176;
                            subSizeY = 166;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.FURNACE) {
                            sprite = ParticleRegistry.furnace.getSprite();
                            subSizeX = 176;
                            subSizeY = 166;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.GRINDSTONE) {
                            sprite = ParticleRegistry.grindstone.getSprite();
                            subSizeX = 176;
                            subSizeY = 166;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.HOPPER) {
                            sprite = ParticleRegistry.hopper.getSprite();
                            subSizeX = 176;
                            subSizeY = 134;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.HORSE) {
                            sprite = ParticleRegistry.horse.getSprite();
                            subSizeX = 176;
                            subSizeY = 166;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.LOOM) {
                            sprite = ParticleRegistry.loom.getSprite();
                            subSizeX = 176;
                            subSizeY = 166;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.VILLAGER) {
                            sprite = ParticleRegistry.villager.getSprite();
                            subSizeX = 277;
                            subSizeY = 167;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.COMMAND_BLOCK) {
                            sprite = ParticleRegistry.command_block.getSprite();
                            subSizeX = 308;
                            subSizeY = 213;
                        } else if (this.getStatus(player).getPlayerGuiState() == PlayerStatus.PlayerGuiState.MISC && ConfigClient.showPlayerActiveGuiIfNotExactMatch) {
                            particle = new ParticleStaticLoD((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, ParticleRegistry.chest.getSpriteSet());
                        }
                    }

                    if (sprite != null) {
                        particle = new ParticleStaticPartial((ClientLevel) player.level(), posParticle.x, posParticle.y, posParticle.z, sprite, brightness, subSizeX, subSizeY);
                    }
                }
                if (particle != null) {
                    playerStatus.setParticle(particle);
                    //if (particle instanceof ParticleDynamic) {
                        getParticleEngine().add(particle);
                    /*} else {
                        Minecraft.getInstance().particleEngine.add(particle);
                    }*/
                }
            } else {

            }
        }

        if (playerStatus.getParticleIdle() != null) {
            ParticleStatic particle = (ParticleStatic)playerStatus.getParticleIdle();
            if (particle.isAlive()) {
                particle.keepAlive();
                particle.setPos(player.position().x, player.position().y + idleY, player.position().z);
                particle.setPosPrev(player.position().x, player.position().y + idleY, player.position().z);
                particle.setParticleSpeed(0, 0, 0);
                particle.rotationYaw = -player.yBodyRot + 180;
                particle.prevRotationYaw = particle.rotationYaw;
                particle.rotationRoll = cos * 5;
                particle.prevRotationRoll = particle.rotationRoll;
                particle.setQuadSize(0.15F + sin * 0.03F);
                particle.setAlpha(0.5F);
            }
        }

        if (playerStatus.getParticle() != null && playerStatus.getParticle() instanceof ParticleRotating) {
            ParticleRotating particle = (ParticleRotating)playerStatus.getParticle();
            if (particle.isAlive()) {
                particle.keepAlive();
                Vec3 posParticle = getParticlePosition(player);
                particle.setPos(posParticle.x, posParticle.y, posParticle.z);
                particle.setParticleSpeed(0, 0, 0);

                if (!(playerStatus.getParticle() instanceof ParticleAnimated)) {
                    particle.setQuadSize((float) quadSize);

                    if (Minecraft.getInstance().cameraEntity != null) {
                        double distToCamera = Minecraft.getInstance().cameraEntity.distanceTo(player);
                        double distToCameraCapped = Math.max(3F, Math.min(10F, distToCamera));
                        double distToCameraCapped2 = Math.max(3F, Math.min(6F, distToCamera));
                        //Watut.dbg(distToCamera);
                        //float alpha = (float) Math.max(0.35F, 1F - (distToCameraCapped / 10F));
                        float alpha = (float) Math.max(0.35F, 1F - (distToCameraCapped / 10F)) + 0.15F;
                        float brightness = (float) Math.max(0.55F, 1F - (distToCameraCapped / 10F)) + 0.15F;
                        float huh = (float) Math.max(0.0F, 1F - (distToCameraCapped2 / 6F));
                        quadSize = 0.3F + ((Math.sin((stableTime / 10F) % 360) * 0.01F) * huh);
                        particle.setAlpha(alpha);
                        particle.setBrightness(brightness);
                        particle.setQuadSize((float) quadSize);
                        //particle.setBrightness(1);
                        //particle.setAlpha(1);

                        if (particle instanceof ParticleStaticLoD) {
                            ((ParticleStaticLoD) particle).setParticleFromDistanceToCamera((float) distToCamera);
                        }

                        //particle.setAlpha(0.99F);
                        //particle.setBrightness(1F);
                    } else {
                        particle.setAlpha(0.5F);
                    }
                }

                particle.rotationYaw = -player.yBodyRot;
                particle.prevRotationYaw = particle.rotationYaw;
                particle.rotationPitch = 20;
                particle.prevRotationPitch = particle.rotationPitch;
            }
        }

        playerStatusPrev.setPlayerGuiState(playerStatus.getPlayerGuiState());
        playerStatusPrev.setPlayerChatState(playerStatus.getPlayerChatState());
        if (playerStatusPrev.getTicksSinceLastAction() != playerStatus.getTicksSinceLastAction()) {
            playerStatusPrev.setTicksSinceLastAction(playerStatus.getTicksSinceLastAction());
        }
    }

    public boolean renderPingIconHook(PlayerTabOverlay playerTabOverlay, GuiGraphics pGuiGraphics, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo) {
        if (Minecraft.getInstance().particleEngine == null || pPlayerInfo == null || pPlayerInfo.getProfile() == null || !ConfigClient.showIdleStatesInPlayerList || !ConfigServerControlledSyncedToClient.showIdleStatesInPlayerList) return false;
        PlayerStatus playerStatus = getStatus(pPlayerInfo.getProfile().getId());
        if (playerStatus.isIdle()) {
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0F, 0.0F, 101F);
            TextureAtlasSprite sprite = ParticleRegistry.idle.getSprite();
            int x = (int) (Minecraft.getInstance().particleEngine.textureAtlas.width * sprite.getU0());
            int y = (int) (Minecraft.getInstance().particleEngine.textureAtlas.height * sprite.getV0());
            pGuiGraphics.blit(sprite.atlasLocation(), p_282801_ + p_281809_ - 11, pY, x, y, 10, 8, Minecraft.getInstance().particleEngine.textureAtlas.width, Minecraft.getInstance().particleEngine.textureAtlas.height);
            pGuiGraphics.pose().popPose();
            return true;
        }
        return false;
    }

    public void setupRotationsHook(EntityModel model, Entity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        if (!ConfigClient.showPlayerAnimations || !ConfigServerControlledSyncedToClient.showPlayerAnimations) return;
        Minecraft mc = Minecraft.getInstance();
        boolean inOwnInventory = pEntity == mc.player && (mc.screen instanceof EffectRenderingInventoryScreen) && pEntity.isAlive();
        //boolean isRealPlayer = pEntity.tickCount > 10;
        boolean isRealPlayer = pEntity.level().players().contains(pEntity);
        if (model instanceof PlayerModel playerModel && pEntity instanceof Player player && isRealPlayer && ((!inOwnInventory && shouldAnimate((Player) pEntity)) || singleplayerTesting)) {
            PlayerStatus playerStatus = getStatus(player);
            //try to filter out paper model, could use a better context clue, this is using a quirk of rotation not getting wrapped
            boolean contextIsInventoryPaperDoll = playerModel.head.yRot > Math.PI;
            if (!contextIsInventoryPaperDoll) {
                if (playerStatus.getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                    playerStatus.yRotHeadBeforeOverriding = playerModel.head.yRot;
                    playerStatus.xRotHeadBeforeOverriding = playerModel.head.xRot;
                    if (player.level().getGameTime() % 5 == 0) {
                        //Watut.dbg("setting head data for " + playerStatus.yRotHeadBeforeOverriding);
                    }
                } else {
                    if (playerModel.head.yRot <= Math.PI) {
                        playerStatus.yRotHeadWhileOverriding = playerModel.head.yRot;
                        playerStatus.xRotHeadWhileOverriding = playerModel.head.xRot;
                    }
                }
            }

            if (playerStatus.isLerping() || playerStatus.getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE || playerStatus.isIdle()) {
                float partialTick = pAgeInTicks - ((int)pAgeInTicks);
                playerStatus.lastPartialTick = partialTick;

                Vector3f adjRightArm;
                Vector3f adjLeftArm;
                HumanoidArm mainArm = player.getMainArm();
                if (mainArm == HumanoidArm.RIGHT) {
                    adjRightArm = CustomArmCorrections.getAdjustmentForArm(player.getItemBySlot(EquipmentSlot.MAINHAND), player.getItemBySlot(EquipmentSlot.OFFHAND), EquipmentSlot.MAINHAND);
                    adjLeftArm = CustomArmCorrections.getAdjustmentForArm(player.getItemBySlot(EquipmentSlot.OFFHAND), player.getItemBySlot(EquipmentSlot.MAINHAND), EquipmentSlot.OFFHAND);
                } else {
                    adjLeftArm = CustomArmCorrections.getAdjustmentForArm(player.getItemBySlot(EquipmentSlot.OFFHAND), player.getItemBySlot(EquipmentSlot.MAINHAND), EquipmentSlot.OFFHAND);
                    adjRightArm = CustomArmCorrections.getAdjustmentForArm(player.getItemBySlot(EquipmentSlot.MAINHAND), player.getItemBySlot(EquipmentSlot.OFFHAND), EquipmentSlot.MAINHAND);
                }

                //Float.MAX_VALUE is number code we use for "disabled"
                if (adjRightArm.y != Float.MAX_VALUE) playerModel.rightArm.yRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.yRot, playerStatus.getLerpTarget().rightArm.yRot);
                if (adjRightArm.x != Float.MAX_VALUE) playerModel.rightArm.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.xRot, playerStatus.getLerpTarget().rightArm.xRot);
                playerModel.rightArm.x += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.x, playerStatus.getLerpTarget().rightArm.x);
                playerModel.rightArm.y += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.y, playerStatus.getLerpTarget().rightArm.y);
                playerModel.rightArm.z += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().rightArm.z, playerStatus.getLerpTarget().rightArm.z);

                if (adjLeftArm.y != Float.MAX_VALUE) playerModel.leftArm.yRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().leftArm.yRot, playerStatus.getLerpTarget().leftArm.yRot);
                if (adjLeftArm.x != Float.MAX_VALUE) playerModel.leftArm.xRot += Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().leftArm.xRot, playerStatus.getLerpTarget().leftArm.xRot);

                //TODO: workaround to a weird issue of just the y rotation in creative mode being super out of wack
                // likely because of paper doll in inventory screen, couldnt fix by removing Math.PI until within range
                // still happening even for non creative, wat
                // well we still need to find the optimal direction to rotate and fix the target rotation value to that so it doesnt invert and spin i guess
                float yRotDiff = playerStatus.getLerpTarget().head.yRot - playerStatus.getLerpPrev().head.yRot;
                if (Math.abs(yRotDiff) < Math.PI / 2) {
                    playerModel.head.yRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.yRot, playerStatus.getLerpTarget().head.yRot);
                    if (player.level().getGameTime() % 5 == 0) {
                        //Watut.dbg("yRot: " + playerModel.head.yRot);
                    }
                }

                playerModel.head.xRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.xRot, playerStatus.getLerpTarget().head.xRot);
                playerModel.head.zRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), playerStatus.getLerpPrev().head.zRot, playerStatus.getLerpTarget().head.zRot);

                playerModel.rightSleeve.yRot = playerModel.rightArm.yRot;
                playerModel.rightSleeve.xRot = playerModel.rightArm.xRot;
                playerModel.rightSleeve.x = playerModel.rightArm.x;
                playerModel.rightSleeve.y = playerModel.rightArm.y;
                playerModel.rightSleeve.z = playerModel.rightArm.z;

                playerModel.leftSleeve.yRot = playerModel.leftArm.yRot;
                playerModel.leftSleeve.xRot = playerModel.leftArm.xRot;

                playerModel.hat.xRot = playerModel.head.xRot;
                playerModel.hat.yRot = playerModel.head.yRot;

                if (ConfigClient.showPlayerAnimation_Typing && ConfigServerControlledSyncedToClient.showPlayerAnimation_Typing && playerStatus.getPlayerChatState() == PlayerStatus.PlayerChatState.CHAT_TYPING) {
                    float amp = playerStatus.getTypingAmplifierSmooth();
                    float typeAngle = (float) ((Math.toRadians(Math.sin((pAgeInTicks * 1F) % 360) * 15 * amp)));
                    float typeAngle2 = (float) ((Math.toRadians(-Math.sin((pAgeInTicks * 1F) % 360) * 15 * amp)));
                    if (adjRightArm.x != Float.MAX_VALUE) playerModel.rightArm.xRot -= typeAngle;
                    if (adjRightArm.x != Float.MAX_VALUE) playerModel.rightSleeve.xRot -= typeAngle;
                    if (adjLeftArm.x != Float.MAX_VALUE) playerModel.leftArm.xRot -= typeAngle2;
                    if (adjLeftArm.x != Float.MAX_VALUE) playerModel.leftSleeve.xRot -= typeAngle2;
                }

                if (adjRightArm.x != Float.MAX_VALUE) playerModel.rightArm.xRot -= adjRightArm.x;
                if (adjRightArm.x != Float.MAX_VALUE) playerModel.rightSleeve.xRot -= adjRightArm.x;
                if (adjLeftArm.x != Float.MAX_VALUE) playerModel.leftArm.xRot -= adjLeftArm.x;
                if (adjLeftArm.x != Float.MAX_VALUE) playerModel.leftSleeve.xRot -= adjLeftArm.x;

                if (adjRightArm.y != Float.MAX_VALUE) playerModel.rightArm.yRot -= adjRightArm.y;
                if (adjRightArm.y != Float.MAX_VALUE) playerModel.rightSleeve.yRot -= adjRightArm.y;
                if (adjLeftArm.y != Float.MAX_VALUE) playerModel.leftArm.yRot -= adjLeftArm.y;
                if (adjLeftArm.y != Float.MAX_VALUE) playerModel.leftSleeve.yRot -= adjLeftArm.y;

                if (adjRightArm.z != Float.MAX_VALUE) playerModel.rightArm.zRot -= adjRightArm.z;
                if (adjRightArm.z != Float.MAX_VALUE) playerModel.rightSleeve.zRot -= adjRightArm.z;
                if (adjLeftArm.z != Float.MAX_VALUE) playerModel.leftArm.zRot -= adjLeftArm.z;
                if (adjLeftArm.z != Float.MAX_VALUE) playerModel.leftSleeve.zRot -= adjLeftArm.z;

                if (ConfigClient.showPlayerAnimation_Idle && ConfigServerControlledSyncedToClient.showPlayerAnimation_Idle && playerStatus.isIdle()) {
                    float angle = (float) ((Math.toRadians(Math.sin((pAgeInTicks * 0.05F) % 360) * 15)));
                    float angle2 = (float) ((Math.toRadians(Math.cos((pAgeInTicks * 0.05F) % 360) * 7)));
                    playerModel.head.xRot += angle2;
                    playerModel.head.zRot += angle;
                }
            }
            playerModel.hat.xRot = playerModel.head.xRot;
            playerModel.hat.yRot = playerModel.head.yRot;
            playerModel.hat.zRot = playerModel.head.zRot;
        }
    }

    public void setPoseTarget(UUID uuid, boolean becauseMousePress) {
        //Watut.dbg("setPoseTarget");
        PlayerStatus playerStatus = getStatus(uuid);

        playerStatus.getLerpPrev().rightArm = playerStatus.getLerpTarget().rightArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().rightArm, playerStatus.lastPartialTick);
        playerStatus.getLerpPrev().leftArm = playerStatus.getLerpTarget().leftArm.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().leftArm, playerStatus.lastPartialTick);
        playerStatus.getLerpPrev().head = playerStatus.getLerpTarget().head.copyPartialLerp(playerStatus, playerStatus.getLerpPrev().head, playerStatus.lastPartialTick);

        //rightArm can become NaN for some reason???????
        if (Float.isNaN(playerStatus.getLerpPrev().rightArm.yRot)) {
            playerStatus.getLerpPrev().rightArm.yRot = 0;
        }
        if (Float.isNaN(playerStatus.getLerpPrev().rightArm.xRot)) {
            playerStatus.getLerpPrev().rightArm.xRot = 0;
        }

        boolean pointing = PlayerStatus.PlayerGuiState.isPointingGui(playerStatus.getPlayerGuiState());
        boolean typing = playerStatus.getPlayerChatState() == PlayerStatus.PlayerChatState.CHAT_TYPING;
        boolean idle = playerStatus.isIdle();

        if (!ConfigClient.showPlayerAnimation_Gui || !ConfigServerControlledSyncedToClient.showPlayerAnimation_Gui) pointing = false;
        if (!ConfigClient.showPlayerAnimation_Typing || !ConfigServerControlledSyncedToClient.showPlayerAnimation_Typing) typing = false;
        if (!ConfigClient.showPlayerAnimation_Idle || !ConfigServerControlledSyncedToClient.showPlayerAnimation_Idle) idle = false;

        if (becauseMousePress) {
            playerStatus.setNewLerp(armMouseTickRate * 0.5F);
        } else {
            playerStatus.setNewLerp(armMouseTickRate * 1F);
        }

        if (pointing || typing) {
            playerStatus.getLerpTarget().head.xRot = (float) Math.toRadians(15);
            playerStatus.getLerpTarget().head.yRot = 0;
        }

        if (pointing) {
            double xPercent = playerStatus.getScreenPosPercentX();
            double yPercent = playerStatus.getScreenPosPercentY();
            double x = Math.toRadians(90) - Math.toRadians(22.5) - yPercent;
            double y = -Math.toRadians(15) + xPercent;
            double xHead = Math.toRadians(22.5) + yPercent;
            double yHead = xPercent;

            playerStatus.getLerpTarget().rightArm.yRot = (float) y;
            playerStatus.getLerpTarget().rightArm.xRot = (float) -x;

            playerStatus.getLerpTarget().head.yRot = (float) yHead * 0.5F;
            //playerStatus.getLerpTarget().head.xRot = (float) ((-x * 0.5F) + Math.toRadians(90));
            playerStatus.getLerpTarget().head.xRot = (float) xHead * 0.5F;

            if (playerStatus.isPressing()) {
                Vec3 vec = calculateViewVector((float) Math.toDegrees(y), (float) Math.toDegrees(x));
                float press = 1;
                playerStatus.getLerpTarget().rightArm.x = (float) (press * vec.y);
                playerStatus.getLerpTarget().rightArm.y = (float) (press * vec.z);
                playerStatus.getLerpTarget().rightArm.z = (float) (press * vec.x);
            } else {
                playerStatus.getLerpTarget().rightArm.x = (float) 0;
                playerStatus.getLerpTarget().rightArm.z = (float) 0;
                playerStatus.getLerpTarget().rightArm.y = (float) 0;
            }

            playerStatus.getLerpTarget().leftArm.xRot = (float) -Math.toRadians(70);
            playerStatus.getLerpTarget().leftArm.yRot = (float) Math.toRadians(25);


        } else if (typing) {
            double x = Math.toRadians(90) - Math.toRadians(22.5);
            playerStatus.getLerpTarget().rightArm.xRot = (float) -x;
            playerStatus.getLerpTarget().leftArm.xRot = (float) -x;

            double tiltIn = Math.toRadians(20);
            playerStatus.getLerpTarget().rightArm.yRot = (float) -tiltIn;
            playerStatus.getLerpTarget().leftArm.yRot = (float) tiltIn;
        }

        //reset to neutral
        if (!pointing && !typing && !idle) {
            playerStatus.setLerpTarget(new Lerpables());
            playerStatus.getLerpTarget().head.xRot = playerStatus.xRotHeadBeforeOverriding;
            playerStatus.getLerpTarget().head.yRot = playerStatus.yRotHeadBeforeOverriding;
        }

        if (idle) {
            playerStatus.getLerpTarget().head.xRot = (float) Math.toRadians(70);
            playerStatus.setNewLerp(40);
        }

        //setup head lerp from to be where our head was before overrides started
        if (getStatusPrev(uuid).getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE && getStatus(uuid).getPlayerGuiState() != PlayerStatus.PlayerGuiState.NONE) {
            playerStatus.getLerpPrev().head.xRot = playerStatus.xRotHeadBeforeOverriding;
            playerStatus.getLerpPrev().head.yRot = playerStatus.yRotHeadBeforeOverriding;
        }


    }

    public Vec3 getParticlePosition(Player player) {
        Vec3 pos = player.position();
        //float distFromFace = 0.75F;
        float distFromFace = 0.85F;
        //float distFromFace = -3.5F;
        Vec3 lookVec = getBodyAngle(player).scale(distFromFace);
        return new Vec3(pos.x + lookVec.x, pos.y + 1.2D, pos.z + lookVec.z);
        //return new Vec3(pos.x + lookVec.x - 2, pos.y + 1.2D, pos.z + lookVec.z);
    }

    public PlayerStatus getStatusLocal() {
        return selfPlayerStatus;
    }

    public PlayerStatus getStatusPrevLocal() {
        return selfPlayerStatusPrev;
    }

    public PlayerStatus getStatusPrev(Player player) {
        return getStatusPrev(player.getUUID());
    }

    public PlayerStatus getStatusPrev(UUID uuid) {
        return getStatusPrev(uuid, false);
    }

    public PlayerStatus getStatusPrev(UUID uuid, boolean local) {
        if (local) return getStatusPrevLocal();
        checkPrev(uuid);
        return lookupPlayerToStatusPrev.get(uuid);
    }

    public void checkPrev(UUID uuid) {
        if (!lookupPlayerToStatusPrev.containsKey(uuid)) {
            lookupPlayerToStatusPrev.put(uuid, new PlayerStatus(PlayerStatus.PlayerGuiState.NONE));
        }
    }

    public void sendGuiStatus(PlayerStatus.PlayerGuiState playerStatus) {
        sendGuiStatus(playerStatus, false);
    }

    public void sendGuiStatus(PlayerStatus.PlayerGuiState playerStatus, boolean force) {
        if (getStatusLocal().getPlayerGuiState() != playerStatus || force) {
            CompoundTag data = new CompoundTag();
            data.putInt(WatutNetworking.NBTDataPlayerGuiStatus, playerStatus.ordinal());
            data.putBoolean(WatutNetworking.NBTDataPlayerGuiDontSendDetailedGUIInfo, ConfigClient.dontSendDetailedGUIInfo);
            data.putBoolean(WatutNetworking.NBTDataPlayerGuiDontSendItemInfo, ConfigClient.dontSendItemInfo);
            //CULog.dbg("sending status from client: " + playerStatus + " for " + Minecraft.getInstance().player.getUUID());
            //CULog.dbg("data: " + data);
            WatutNetworking.instance().clientSendToServer(data);
        }
        getStatusLocal().setPlayerGuiState(playerStatus);
    }

    public void sendChatStatus(PlayerStatus.PlayerChatState playerStatus) {
        sendChatStatus(playerStatus, false);
    }

    public void sendChatStatus(PlayerStatus.PlayerChatState playerStatus, boolean force) {
        if (getStatusLocal().getPlayerChatState() != playerStatus || force) {
            CompoundTag data = new CompoundTag();
            data.putInt(WatutNetworking.NBTDataPlayerChatStatus, playerStatus.ordinal());
            //CULog.dbg("sending chat status from client: " + playerStatus + " for " + Minecraft.getInstance().player.getUUID());
            //CULog.dbg("data: " + data);
            WatutNetworking.instance().clientSendToServer(data);
        }
        getStatusLocal().setPlayerChatState(playerStatus);
    }

    public void sendMouse(Pair<Float, Float> pos, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        float x = pos.first;
        float y = pos.second;

        boolean testSingleplayer = false;

        if (testSingleplayer || mc.level.getNearestPlayer(mc.player.getX(), mc.player.getY(), mc.player.getZ(), ConfigServerControlledSyncedToClient.distanceRequiredToShowGUIInfo, (entity) -> entity != mc.player) != null) {
            if (getStatusLocal().getScreenPosPercentX() != x || getStatusLocal().getScreenPosPercentY() != y || getStatusLocal().isPressing() != pressed) {
                CompoundTag data = new CompoundTag();
                data.putFloat(WatutNetworking.NBTDataPlayerMouseX, x);
                data.putFloat(WatutNetworking.NBTDataPlayerMouseY, y);
                data.putBoolean(WatutNetworking.NBTDataPlayerMousePressed, pressed);

                //data.putInt("testIndex", i);


                //CULog.dbg("sending mouse status from client for " + Minecraft.getInstance().player.getUUID());
                //CULog.dbg("data: " + data);
                WatutNetworking.instance().clientSendToServer(data);

            }
        }
        getStatusLocal().setScreenPosPercentX(x);
        getStatusLocal().setScreenPosPercentY(y);
        getStatusLocal().setPressing(pressed);
    }

    public void sendScreenRenderData(PlayerStatus status) {
        CompoundTag data = new CompoundTag();

        int packetSizeLimit = 31000;
        int sizeByteCount = status.getScreenData().getTexturePixelData().remaining();
        int sizeByteCountLimit = status.getScreenData().getTexturePixelData().limit();

        if (status.getScreenData().getTexturePixelData() != null) {
            byte[] inputBytes = new byte[sizeByteCount];
            status.getScreenData().getTexturePixelData().get(inputBytes);
            if (sizeByteCountLimit < packetSizeLimit) {
                data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataSize, status.getScreenData().getTexturePixelData().capacity());
                data.putInt(WatutNetworking.NBTDataPlayerScreenWidth, ScreenParticleRenderer.getInstance().widthScaledDown);
                data.putInt(WatutNetworking.NBTDataPlayerScreenHeight, ScreenParticleRenderer.getInstance().heightScaledDown);
                data.putByteArray(WatutNetworking.NBTDataPlayerScreenCompressedPixelData, inputBytes);

                data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount, 1);
                data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex, 1);

                WatutNetworking.instance().clientSendToServer(data);
            } else {
                int packetCount = Mth.ceil((float)sizeByteCount / (float)packetSizeLimit);

                int packetBytesIndex = 0;

                for (int i = 0; i < packetCount; i++) {
                    byte[] inputBytesPartial;
                    if (packetBytesIndex + packetSizeLimit < sizeByteCount) {
                        inputBytesPartial = Arrays.copyOfRange(inputBytes, packetBytesIndex, packetBytesIndex + packetSizeLimit);
                    } else {
                        inputBytesPartial = Arrays.copyOfRange(inputBytes, packetBytesIndex, sizeByteCount);
                    }
                    packetBytesIndex += inputBytesPartial.length;

                    //WE MUST USE NEW NBT FOR EACH PACKET OR WE ARE CHANGING THE ONE ALREADY ABOUT TO GET SEND AND THATS BREAKS THINGS - this cost me 5 hours of debugging
                    data = new CompoundTag();
                    data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataSize, status.getScreenData().getTexturePixelData().capacity());
                    data.putInt(WatutNetworking.NBTDataPlayerScreenWidth, ScreenParticleRenderer.getInstance().widthScaledDown);
                    data.putInt(WatutNetworking.NBTDataPlayerScreenHeight, ScreenParticleRenderer.getInstance().heightScaledDown);

                    data.putByteArray(WatutNetworking.NBTDataPlayerScreenCompressedPixelData, inputBytesPartial);

                    data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount, packetCount);
                    data.putInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex, i);

                    WatutNetworking.instance().clientSendToServer(data);

                }
            }
        }


    }

    public void sendTyping(PlayerStatus status) {
        CompoundTag data = new CompoundTag();
        data.putFloat(WatutNetworking.NBTDataPlayerTypingAmp, status.getTypingAmplifier());

        //CULog.dbg("sending typing amp from client for " + Minecraft.getInstance().player.getUUID());
        //CULog.dbg("data: " + data);
        WatutNetworking.instance().clientSendToServer(data);
    }

    public void sendIdle(PlayerStatus status) {
        CompoundTag data = new CompoundTag();
        data.putInt(WatutNetworking.NBTDataPlayerIdleTicks, status.getTicksSinceLastAction());

        //CULog.dbg("sending idle ticks from client for " + Minecraft.getInstance().player.getUUID());
        //CULog.dbg("data: " + data);
        WatutNetworking.instance().clientSendToServer(data);
    }

    public void receiveAny(UUID uuid, CompoundTag data) {
        PlayerStatus status = getStatus(uuid);
        PlayerStatus statusPrev = getStatusPrev(uuid);

        if (data.contains(WatutNetworking.NBTDataPlayerTypingAmp)) {
            status.setTypingAmplifier(data.getFloat(WatutNetworking.NBTDataPlayerTypingAmp));
        }

        if (data.contains(WatutNetworking.NBTDataPlayerMouseX)) {
            float x = data.getFloat(WatutNetworking.NBTDataPlayerMouseX);
            float y = data.getFloat(WatutNetworking.NBTDataPlayerMouseY);
            boolean pressed = data.getBoolean(WatutNetworking.NBTDataPlayerMousePressed);
            boolean differentPress = status.isPressing() != pressed;
            setMouse(uuid, x, y, pressed);
            setPoseTarget(uuid, differentPress);
            if (pressed && differentPress) {
                Player player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
                if (player != null && ConfigClient.playMouseClickSounds && ConfigServerControlledSyncedToClient.playMouseClickSounds && player != Minecraft.getInstance().player) {
                    WatutMod.dbg("play sound for " + uuid + " name " + player.getDisplayName().getString());
                    player.level().playLocalSound(player.getOnPos(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.05F, 0.1F, false);
                }
            }
        }

        if (data.contains(WatutNetworking.NBTDataPlayerGuiStatus)) {
            PlayerStatus.PlayerGuiState playerGuiState = PlayerStatus.PlayerGuiState.get(data.getInt(WatutNetworking.NBTDataPlayerGuiStatus));
            status.setPlayerGuiState(playerGuiState);
            if (data.contains(WatutNetworking.NBTDataPlayerGuiDontSendDetailedGUIInfo)) status.setPlayerGuiDontSendDetailedGUIInfo(data.getBoolean(WatutNetworking.NBTDataPlayerGuiDontSendDetailedGUIInfo));
            if (data.contains(WatutNetworking.NBTDataPlayerGuiDontSendItemInfo)) status.setPlayerGuiDontSendItemInfo(data.getBoolean(WatutNetworking.NBTDataPlayerGuiDontSendItemInfo));
            if (status.getPlayerGuiState() != statusPrev.getPlayerGuiState()) {
                WatutMod.dbg("New gui player state and new pose target set relating to: " + status.getPlayerGuiState() + " for " + uuid);
                if (statusPrev.getPlayerGuiState() == PlayerStatus.PlayerGuiState.NONE) {
                    status.setLerpTarget(new Lerpables());
                }
                setPoseTarget(uuid, false);
                Player player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
                if (player != null && ConfigClient.playScreenOpenSounds && ConfigServerControlledSyncedToClient.playScreenOpenSounds && player != Minecraft.getInstance().player) {
                    PlayerStatus.PlayerGuiState playerGuiStatePrev = statusPrev.getPlayerGuiState();
                    if (PlayerStatus.PlayerGuiState.isSoundMakerGui(playerGuiState) || PlayerStatus.PlayerGuiState.isSoundMakerGui(playerGuiStatePrev) || playerGuiState == PlayerStatus.PlayerGuiState.INVENTORY || playerGuiState == PlayerStatus.PlayerGuiState.CRAFTING || playerGuiState == PlayerStatus.PlayerGuiState.MISC ||
                            playerGuiStatePrev == PlayerStatus.PlayerGuiState.INVENTORY || playerGuiStatePrev == PlayerStatus.PlayerGuiState.CRAFTING || playerGuiStatePrev == PlayerStatus.PlayerGuiState.MISC) {
                        player.level().playLocalSound(player.getOnPos(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.9F, 1F, false);
                    }
                }
            }
        }

        

        if (data.contains(WatutNetworking.NBTDataPlayerChatStatus)) {
            PlayerStatus.PlayerChatState state = PlayerStatus.PlayerChatState.get(data.getInt(WatutNetworking.NBTDataPlayerChatStatus));
            status.setPlayerChatState(state);
            if (status.getPlayerChatState() != statusPrev.getPlayerChatState()) {
                WatutMod.dbg("New chat player state and new pose target set relating to: " + status.getPlayerChatState() + " for " + uuid);
                if (statusPrev.getPlayerChatState() == PlayerStatus.PlayerChatState.NONE) {
                    status.setLerpTarget(new Lerpables());
                }
                if (status.getPlayerChatState() == PlayerStatus.PlayerChatState.CHAT_FOCUSED) {
                    status.setTypingAmplifier(1F);
                    status.setTypingAmplifierSmooth(1F);
                }
                setPoseTarget(uuid, false);
            }
        }

        if (data.contains(WatutNetworking.NBTDataPlayerIdleTicks)) {
            //Watut.dbg("receive idle ticks from server: " + data.getInt(WatutNetworking.NBTDataPlayerIdleTicks) + " for " + uuid + " playerStatus hash: " + status);
            status.setTicksSinceLastAction(data.getInt(WatutNetworking.NBTDataPlayerIdleTicks));
            status.setTicksToMarkPlayerIdleSyncedForClient(data.getInt(WatutNetworking.NBTDataPlayerTicksToGoIdle));
            statusPrev.setTicksToMarkPlayerIdleSyncedForClient(data.getInt(WatutNetworking.NBTDataPlayerTicksToGoIdle));
            getStatusLocal().setTicksToMarkPlayerIdleSyncedForClient(data.getInt(WatutNetworking.NBTDataPlayerTicksToGoIdle));
            getStatusPrevLocal().setTicksToMarkPlayerIdleSyncedForClient(data.getInt(WatutNetworking.NBTDataPlayerTicksToGoIdle));
            if (statusPrev.isIdle() != status.isIdle()) {
                WatutMod.dbg("New idle player state and new pose target set relating to idle state: " + status.isIdle() + " for " + uuid);
                setPoseTarget(uuid, false);
            }
        }

        //CULog.dbg("client receive packet index " + data.getInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex) + " of " + data.getInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount));

        //if (true) return;

        if (data.contains(WatutNetworking.NBTDataPlayerScreenCompressedPixelData)) {
            byte[] pixelData = data.getByteArray(WatutNetworking.NBTDataPlayerScreenCompressedPixelData);
            int decompressedSize = data.getInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataSize);
            int packetCount = data.getInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount);
            int packetIndex = data.getInt(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex);
            status.getScreenData().setWidth(data.getInt(WatutNetworking.NBTDataPlayerScreenWidth));
            status.getScreenData().setHeight(data.getInt(WatutNetworking.NBTDataPlayerScreenHeight));
            long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
            int timeout = 10;

            //CULog.dbg("compressed pixel data size in bytes: " + pixelData.length + " - " + packetIndex + " of " + packetCount + " roughly total " + (pixelData.length * packetCount));

            if (packetCount > 1) {
                if (packetIndex == 0) {
                    status.getScreenData().setGameTicksSinceFirstPacket(gameTime);
                    status.getScreenData().setLastIndexReceived(0);
                    status.getScreenData().setTexturePixelDataPartial(pixelData);
                } else {
                    if (packetIndex != status.getScreenData().getLastIndexReceived() + 1) {
                        CULog.dbg("ERROR: packet index incorrect, aborting packet processing");
                    } else {
                        status.getScreenData().setLastIndexReceived(packetIndex);
                        if (gameTime > status.getScreenData().getGameTicksSinceFirstPacket() + timeout) {
                            //packet timeout waiting for other pieces, abort and reset state
                            //actually dont think i have to do anything, if a new packetIndex 0 comes in it forces a fresh set
                            //CULog.dbg("watut packet took too long to come in, stop waiting and reset");
                        } else {
                            if (pixelData.length > 0) {
                                byte[] dataBytes = status.getScreenData().getTexturePixelDataPartial();
                                byte[] combined = new byte[dataBytes.length + pixelData.length];
                                System.arraycopy(dataBytes, 0, combined, 0, dataBytes.length);
                                System.arraycopy(pixelData, 0, combined, dataBytes.length, pixelData.length);
                                status.getScreenData().setTexturePixelDataPartial(combined);

                                if (packetIndex == packetCount-1) {
                                    try {
                                        if (status.getScreenData().getTexturePixelDataPartial() != null) {
                                            status.getScreenData().setTexturePixelData(RenderHelper.decompress(status.getScreenData(), ByteBuffer.wrap(status.getScreenData().getTexturePixelDataPartial()), decompressedSize));
                                            status.getScreenData().markNeedsNewRenderFromPixelData(true);
                                            status.getScreenData().getIsBufferReady().set(true);
                                        } else {
                                            CULog.dbg("getTexturePixelDataPartial() null!");
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                        }
                    }

                }
            } else {
                try {
                    status.getScreenData().setTexturePixelData(RenderHelper.decompress(status.getScreenData(), ByteBuffer.wrap(pixelData), decompressedSize));
                    status.getScreenData().markNeedsNewRenderFromPixelData(true);
                    status.getScreenData().getIsBufferReady().set(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void receiveItemMove(CompoundTag data) {
        if (data.contains(WatutNetworking.NBTDataItemTransferItemStack)) {

            ItemStack itemStack = ItemStack.parseOptional(Minecraft.getInstance().level.registryAccess(), data.getCompound(WatutNetworking.NBTDataItemTransferItemStack));
            ParticleItem particleItem = new ParticleItem(Minecraft.getInstance().level, 1, itemStack,
                    Minecraft.getInstance().renderBuffers(),
                    Minecraft.getInstance().getEntityRenderDispatcher(),
                    data.getFloat(WatutNetworking.NBTDataItemTransferFromX),
                    data.getFloat(WatutNetworking.NBTDataItemTransferFromY),
                    data.getFloat(WatutNetworking.NBTDataItemTransferFromZ),
                    data.getFloat(WatutNetworking.NBTDataItemTransferToX),
                    data.getFloat(WatutNetworking.NBTDataItemTransferToY),
                    data.getFloat(WatutNetworking.NBTDataItemTransferToZ));
            //Minecraft.getInstance().particleEngine.add(particleItem);
            getParticleEngine().add(particleItem);

        }
    }

    public void receiveServerConfig(CompoundTag nbt) {

        CULog.dbg("receiving server config sync");
        ConfigServerSyncHelper.getInstance().updateSyncableConfigOnClient(nbt);

        //update buffers carefully if needed
        ScreenParticleRenderer.getInstance().resizeScaledDown(ScreenParticleRenderer.getInstance().width, ScreenParticleRenderer.getInstance().height);

    }

}
