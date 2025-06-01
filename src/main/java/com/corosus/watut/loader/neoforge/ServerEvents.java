package com.corosus.watut.loader.neoforge;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.ShaderReloader;
import com.corosus.watut.WatutMod;
import com.corosus.watut.command.CommandWatutReloadJSON;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class ServerEvents {

    public void onHurtEntity(LivingDamageEvent.Post event) {
        //client needs to know, not server
        //CULog.dbg("? " + event.getEntity().getScoreboardName());
        //event.getEntity().setDeltaMovement(0, 0, 0);
    }

    public void onKnock(LivingKnockBackEvent event) {
        //client needs to know, not server
        //CULog.dbg("? " + event.getEntity().getScoreboardName());
        event.setStrength(0.2F);
        //event.setCanceled(true);
    }

    public void onTickEntity(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            if (livingEntity.hurtTime > 0) {
                //event.getEntity().setDeltaMovement(0, 0, 0);
                //they detatched entity from rendering via all relevant info in render state, so im using my own lookup
                //CULog.dbg("h " + livingEntity.hurtTime);

            }
            //lookupEntityToData.put(event.getEntity(), livingEntity.hurtTime);
        }
    }

    public void onGameTick(ClientTickEvent.Post event) {
        WatutMod.getPlayerStatusManagerClient().tickGame();
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
        event.addListener(ResourceLocation.parse(WatutMod.MODID + ":particles"), PlayerStatusManagerClient.getParticleEngine());
        event.addListener(ResourceLocation.parse(WatutMod.MODID + ":shaders"), new ShaderReloader());
    }

}
