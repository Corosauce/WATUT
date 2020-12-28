package com.corosus.watut;

import com.corosus.watut.command.CommandReloadConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod.EventBusSubscriber(modid = WATUT.MODID)
public class EventHandlerForge {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void clientChat(ClientChatEvent event) {
        String msg = event.getMessage();

        if (msg.equals("/" + CommandReloadConfig.getCommandName() + " client")) {
            Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent("reloading all mods client configurations"));
            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, FMLPaths.CONFIGDIR.get());
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void registerCommands(RegisterCommandsEvent event) {
        CommandReloadConfig.register(event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void tickWorld(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            WATUT.playerManagerServer.tick(event.world);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOW)
    public void tickClient(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (Minecraft.getInstance().world != null) {
                WATUT.playerManagerClient.tick(Minecraft.getInstance().world);
            }
        }
    }


}