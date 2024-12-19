package com.corosus.watut.physics;

import com.corosus.watut.loader.forge.WatutModForge;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class RayonExampleModClient {

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        EntityRenderers.register((EntityType<StoneBlockEntity>) WatutModForge.STONE_BLOCK_ENTITY.get(), (context) -> new StoneBlockEntityRenderer(context, new StoneBlockEntityModel(12, 4, 12)));
    }

}
