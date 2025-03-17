package com.corosus.watut;

import com.corosus.watut.client.screen.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

import java.util.ArrayList;
import java.util.List;

public class ParticleRegistry {

    public static SpriteInfo inventory;
    public static SpriteInfo chest;
    public static SpriteInfo crafting;
    public static SpriteInfo escape;
    public static SpriteInfo sign;
    public static SpriteInfo book;
    public static SpriteInfo enchanting_table;
    public static SpriteInfo anvil;
    public static SpriteInfo beacon;
    public static SpriteInfo brewing_stand;
    public static SpriteInfo dispenser;
    public static SpriteInfo furnace;
    public static SpriteInfo grindstone;
    public static SpriteInfo hopper;
    public static SpriteInfo horse;
    public static SpriteInfo loom;
    public static SpriteInfo villager;
    public static SpriteInfo command_block;
    public static SpriteInfo chat_idle;
    public static SpriteInfo chat_typing;
    public static SpriteInfo idle;
    public static List<SpriteInfo> particles = new ArrayList<>();

    static {
        inventory = add("inventory_", 3, 0);
        chest = add("chest_", 3, 0);
        crafting = add("crafting_", 3, 0);
        escape = add("escape_menu_", 3, 0);
        sign = add("sign", 0, 0);
        book = add("book", 0, 0);
        enchanting_table = add("enchanting_table", 0, 0);
        anvil = add("anvil", 0, 0);
        beacon = add("beacon", 0, 0);
        brewing_stand = add("brewing_stand", 0, 0);
        dispenser = add("dispenser", 0, 0);
        furnace = add("furnace", 0, 0);
        grindstone = add("grindstone", 0, 0);
        hopper = add("hopper", 0, 0);
        horse = add("horse", 0, 0);
        loom = add("loom", 0, 0);
        villager = add("villager", 0, 0);
        command_block = add("command_block", 0, 0);

        chat_idle = add("chat_idle_", 2, 6);
        chat_typing = add("chat_typing_", 6, 2);
        idle = add("idle");
    }

    public static SpriteInfo add(String name) {
        return add(name, 0, 0);
    }

    public static SpriteInfo add(String name, int frames, int tickDelay) {
        SpriteInfo spriteInfo = new SpriteInfo(name, frames, tickDelay);
        particles.add(spriteInfo);
        return spriteInfo;
    }

    public static void textureAtlasUpload(TextureAtlas textureAtlas) {
        if (!textureAtlas.location().equals(TextureAtlas.LOCATION_PARTICLES)) return;
        for (SpriteInfo info : ParticleRegistry.particles) {
            info.setupSprites(textureAtlas);
        }
    }

    //TESTING - learning render types, shards, new particlerendertypes

    //this is the best one i think TransparencyStateShard, because it actually does nothing and lets me add whatever
    /*public static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
            "no_transparency", () -> RenderSystem.disableBlend(), () -> {
    });

    public static final RenderStateShard.TransparencyStateShard DYNAMIC_TEXTURE = new RenderStateShard.TransparencyStateShard(
            "no_transparency", () -> {
        //oculus breaks our shader for some reason
        if (RenderHelper.isShadersEnabled()) {
            RenderSystem.setShader(CoreShaders.PARTICLE);
        } else {
            RenderSystem.setShader(PlayerStatusManagerClient.particle.getProgram());
        }
        RenderSystem.setShaderTexture(0, getImage().getId());

        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }, () -> {
        RenderSystem.enableCull();
    });*/

    static {

        ResourceLocation test = ResourceLocation.parse("test");

        /**
         * setTransparencyState
         * setShaderState
         * etc
         * these are just templates, they all are run in an order, i should put what relevant bits that i can in correctly eg set shader via setShaderState
         * then just put the leftovers in setTransparencyState maybe, unless its order is an issue
         */

        //TODO: buffer size var? 1536

        /*RenderType TEST_TYPE = RenderType.create("dynamic_texture", DefaultVertexFormat.PARTICLE,
                    VertexFormat.Mode.QUADS, 1536, false, false,
                    RenderType.CompositeState.builder()
                            //our generic state use, for now
                            .setTransparencyState(DYNAMIC_TEXTURE)
                            //.setShaderState(RenderType.PARTICLE_SHADER)
                            //.setTextureState(new RenderStateShard.TextureStateShard(test, TriState.FALSE, false))
                            //.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY).setOutputState(RenderType.PARTICLES_TARGET)
                            //.setLightmapState(RenderType.LIGHTMAP).setWriteMaskState(RenderType.COLOR_DEPTH_WRITE)
                            .createCompositeState(false));

        ParticleRenderType TERRAIN_SHEET = new ParticleRenderType("TERRAIN_SHEET", TEST_TYPE);*/
    }
}
