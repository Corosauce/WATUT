package com.corosus.watut;

import com.corosus.watut.command.CommandReloadConfig;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod.EventBusSubscriber(modid = WATUT.MODID)
public class EventHandlerForge {

    public static TextureAtlasSprite square16;
    public static TextureAtlasSprite square64;

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

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOW)
    public void renderPlayer(RenderPlayerEvent.Post event) {
        PlayerStatusRenderer.renderShadow(event.getMatrixStack(), event.getBuffers(), event.getEntity(), event.getPartialRenderTick(), event.getEntity().world, 1F);
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        //event.getRegistry().registerAll(new Block(...), new Block(...), ...);
    }

    /*@SubscribeEvent
    public static void registerParticles(final RegistryEvent.Register<ParticleType<?>> event) {
        ParticleEnergy.TYPE.setRegistryName(new ResourceLocation(HearthWell.MODID, ParticleEnergy.REG_ID));
        event.getRegistry().register(ParticleEnergy.TYPE);
    }*/

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerParticles(TextureStitchEvent.Pre event) {
        if (!event.getMap().getTextureLocation().equals(AtlasTexture.LOCATION_PARTICLES_TEXTURE)) {
            return;
        }
        //System.out.println("TextureStitchEvent.Pre");
        event.addSprite(new ResourceLocation(WATUT.MODID + ":particle/white16"));
        event.addSprite(new ResourceLocation(WATUT.MODID + ":particle/white64"));
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void getRegisteredParticles(TextureStitchEvent.Post event) {
        if (!event.getMap().getTextureLocation().equals(AtlasTexture.LOCATION_PARTICLES_TEXTURE)) {
            return;
        }
        //System.out.println("TextureStitchEvent.Post");
        square16 = event.getMap().getSprite(new ResourceLocation(WATUT.MODID + ":particle/white16"));
        square64 = event.getMap().getSprite(new ResourceLocation(WATUT.MODID + ":particle/white64"));
        //System.out.println("TextureStitchEvent.Post square16 is: " + square16);
    }

}