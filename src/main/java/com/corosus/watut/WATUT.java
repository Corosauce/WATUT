package com.corosus.watut;

import com.corosus.modconfig.ConfigMod;
import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigCommon;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.HashMap;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Watut.MODID)
public class Watut
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "watut";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    private static PlayerStatusManagerClient playerStatusManagerClient = null;
    private static PlayerStatusManagerServer playerStatusManagerServer = null;

    public static PlayerStatusManagerClient getPlayerStatusManagerClient() {
        if (playerStatusManagerClient == null) playerStatusManagerClient = new PlayerStatusManagerClient();
        return playerStatusManagerClient;
    }

    public static PlayerStatusManagerServer getPlayerStatusManagerServer() {
        if (playerStatusManagerServer == null) playerStatusManagerServer = new PlayerStatusManagerServer();
        return playerStatusManagerServer;
    }

    public Watut()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::gatherData);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EventHandlerForge());
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(ParticleRegistry::getRegisteredParticles);
        }
        ConfigMod.addConfigFile(MODID, new ConfigCommon());
        ConfigMod.addConfigFile(MODID, new ConfigClient());
    }

    private void setup(final FMLCommonSetupEvent event) {
        WatutNetworking.register();
    }

    public static void dbg(Object obj) {
        System.out.println("" + obj);
    }

    /**
     *
     * run runData for me
     *
     * @param event
     */
    private void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        if (event.includeServer()) {

        }
        if (event.includeClient()) {
            gatherClientData(event);
        }
    }

    /**
     *
     * run runData for me
     *
     * @param event
     */
    @OnlyIn(Dist.CLIENT)
    private void gatherClientData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput packOutput = gen.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        gen.addProvider(event.includeClient(), new ParticleRegistry(packOutput, existingFileHelper));
    }
}
