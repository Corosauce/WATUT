package com.corosus.watut;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ShaderProgramBlur {

    private ShaderProgram program;

    //TODO: not all the shaders have these, between having to refresh them and this issue, might be worth just using "compiledShaderProgram.safeGetUniform("resolution");" when using shader program
    //but the lookup each time might be costly, so maybe just a dynamic cached map we invalidate when shaders are reloaded to make sure it gets the new uniform address?
    public AbstractUniform RESOLUTION;
    public AbstractUniform RADIUS;
    public AbstractUniform BLUR_LEVEL;

    public boolean needsUniformInit = true;

    public ShaderProgramBlur(String resourcePath) {

        program = new ShaderProgram(
                ResourceLocation.fromNamespaceAndPath(WatutMod.MODID, resourcePath),
                DefaultVertexFormat.POSITION_TEX, ShaderDefines.EMPTY);

    }

    public ShaderProgram getProgram() {
        return getProgram(true);
    }

    public ShaderProgram getProgram(boolean setupUniforms) {
        if (needsUniformInit && setupUniforms) {
            setupUniforms();
        }
        return program;
    }

    public void markUniformsNeedUpdate() {
        needsUniformInit = true;
    }

    public void setupUniforms() {
        //avoids trying to get uniforms too early, runs at shader use time instead
        if (Minecraft.getInstance().getShaderManager() != null) {
            CompiledShaderProgram compiledShaderProgram = Minecraft.getInstance().getShaderManager().getProgram(program);
            RESOLUTION = compiledShaderProgram.safeGetUniform("resolution");
            RADIUS = compiledShaderProgram.safeGetUniform("radius");
            BLUR_LEVEL = compiledShaderProgram.safeGetUniform("blurLevel");
            needsUniformInit = false;
        }
    }
}
