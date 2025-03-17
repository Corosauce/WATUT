package com.corosus.watut.loader.neoforge;


import com.corosus.coroutil.util.MultiLoaderUtil;
import com.corosus.watut.ShaderRegistry;
import com.corosus.watut.ShaderReloader;
import com.corosus.watut.WatutMod;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.players.PlayerList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(WatutMod.MODID)
public class WatutModNeoForge extends WatutMod {

    public WatutModNeoForge(ModContainer container) {
        super();
        new WatutNetworkingNeoForge();

        container.getEventBus().addListener(this::setup);
        container.getEventBus().addListener(this::registerPackets);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);

        if (FMLEnvironment.dist.isClient()) {
            ClientEvents clientEvents = new ClientEvents();
            container.getEventBus().addListener(clientEvents::getRegisteredParticles);
            NeoForge.EVENT_BUS.addListener(clientEvents::onRegisterCommandsClient);
            NeoForge.EVENT_BUS.addListener(clientEvents::onGameTick);
            NeoForge.EVENT_BUS.addListener(clientEvents::onKey);
            NeoForge.EVENT_BUS.addListener(clientEvents::onMouse);
            container.getEventBus().addListener(clientEvents::reload);

            //container.getEventBus().register(new RegisterShadersEvent());
            /*ShaderRegistry.instance = new ShaderRegistry();
            container.getEventBus().addListener(ShaderRegistry::reload);*/
            //container.getEventBus().addListener(ShaderRegistry.instance);

            ShaderRegistry.init();
        }
    }

    private void setup(final FMLCommonSetupEvent event) {



    }

    public void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0");
        WatutNetworkingNeoForge.register(registrar);
    }


    @Override
    public PlayerList getPlayerList() {
        if (ServerLifecycleHooks.getCurrentServer() == null) return null;
        return ServerLifecycleHooks.getCurrentServer().getPlayerList();
    }

    @Override
    public boolean isModInstalled(String modID) {
        return ModList.get().isLoaded(modID);
    }

    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            WatutMod.getPlayerStatusManagerClient().tickPlayer(event.getEntity());
        } else {
            WatutMod.getPlayerStatusManagerServer().tickPlayer(event.getEntity());
        }
    }

    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        WatutMod.getPlayerStatusManagerServer().playerLoggedIn(event.getEntity());
    }
}
