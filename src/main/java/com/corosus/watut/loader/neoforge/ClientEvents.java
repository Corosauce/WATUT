package com.corosus.watut.loader.neoforge;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.PlayerStatusManagerClient;
import com.corosus.watut.ShaderReloader;
import com.corosus.watut.WatutMod;
import com.corosus.watut.command.CommandWatutReloadJSON;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class ClientEvents {

    //public static HashMap<EntityRenderState, ExtraRenderInfo> lookupEntityToData = new HashMap<>();
    //public static HashMap<EntityRenderState, ExtraRenderInfo> lookupEntityToData2 = new HashMap<>();
    public static HashMap<Entity, ExtraEntityInfo> lookupEntityToData = new HashMap<>();

    public static class ExtraEntityInfo {
        public int hurtTime;
        public int hurtTimeLast;
        //TODO: tracking each tick for client, maybe a temp design and will go more event based with packets in future
        public float healthLastTick;
        public float healthCur;
        public float healthMax;
        //tracking during on hurt
        public float healthLastChange;
        public int randSeed = 0;
        public boolean recalcModelPieces = false;
        public HashSet<ModelPart> partsToHide = new HashSet<>();
    }

    public void onTickEntity(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity livingEntity) {
            ExtraEntityInfo extraEntityInfo = getExtraInfo(event.getEntity());
            if (extraEntityInfo.hurtTime == 0 && extraEntityInfo.hurtTimeLast == 1) {
                extraEntityInfo.randSeed = (new Random()).nextInt(1000);
            }
            extraEntityInfo.hurtTimeLast = extraEntityInfo.hurtTime;
            extraEntityInfo.hurtTime = livingEntity.hurtTime;

            if (livingEntity.hurtTime > 0) {
                //they detatched entity from rendering via all relevant info in render state, so im using my own lookup
                //CULog.dbg("h " + livingEntity.hurtTime);
                int what = 0;

                if (extraEntityInfo.healthLastChange != livingEntity.getHealth()) {
                    //float healthPercent = livingEntity.getHealth() / livingEntity.getMaxHealth();
                    extraEntityInfo.healthLastChange = livingEntity.getHealth();

                    extraEntityInfo.healthCur = livingEntity.getHealth();
                    extraEntityInfo.healthMax = livingEntity.getMaxHealth();
                    extraEntityInfo.recalcModelPieces = true;

                    //blacklist names with: leg, body, root

                }
            }

        }
    }

    public void onHurtEntity(LivingDamageEvent.Post event) {
        CULog.dbg("client hurt " + event.getEntity().getScoreboardName());
        //event.getEntity().setDeltaMovement(0, 0, 0);
    }

    public void onHealthChange(LivingEntity entity) {
        ExtraEntityInfo extraEntityInfo = getExtraInfo(entity);

    }

    public static ExtraEntityInfo getExtraInfo(Entity entity) {
        ExtraEntityInfo extraEntityInfo = ClientEvents.lookupEntityToData.get(entity);
        if (extraEntityInfo == null) {
            //CULog.dbg("new info");
            extraEntityInfo = new ExtraEntityInfo();
            ClientEvents.lookupEntityToData.put(entity, extraEntityInfo);
        }
        return extraEntityInfo;
    }

    public void getRegisteredParticles(TextureAtlasStitchedEvent event) {
        ParticleRegistry.textureAtlasUpload(event.getAtlas());
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
