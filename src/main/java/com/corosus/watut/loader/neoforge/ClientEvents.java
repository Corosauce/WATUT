package com.corosus.watut.loader.neoforge;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.WatutMod;
import com.corosus.watut.command.CommandWatutReloadJSON;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.*;

public class ClientEvents {

    public void getRegisteredParticles(TextureAtlasStitchedEvent event) {
        ParticleRegistry.textureAtlasUpload(event.getAtlas());
    }

    public void onGameTick(ClientTickEvent.Post event) {
        WatutMod.getPlayerStatusManagerClient().tickGame();
    }

    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WatutMod.getPlayerStatusManagerClient().onDisconnect();
    }

    public void onRegisterCommandsClient(RegisterClientCommandsEvent event) {
        CommandWatutReloadJSON.register(event.getDispatcher());
    }

    public void onMouse(InputEvent.MouseButton.Pre event) {
        WatutMod.getPlayerStatusManagerClient().onMouse(event.getAction() != 0);
    }

    public void onKey(InputEvent.Key event) {
        WatutMod.getPlayerStatusManagerClient().onKey();
    }

    public void reload(AddClientReloadListenersEvent event) {
        //event.addListener(Identifier.parse(WatutMod.MODID + ":shaders"), new ShaderReloader());
    }

}
