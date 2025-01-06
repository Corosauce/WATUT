package com.corosus.watut;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import javax.annotation.Nullable;
import java.io.IOException;

public class ShaderInstanceBlur extends ShaderInstance {

    @Nullable
    public final Uniform RESOLUTION;

    @Nullable
    public final Uniform RADIUS;

    public ShaderInstanceBlur(ResourceProvider p_173336_, ResourceLocation shaderLocation, VertexFormat p_173338_) throws IOException {
        super(p_173336_, shaderLocation, p_173338_);
        this.RESOLUTION = this.getUniform("resolution");
        this.RADIUS = this.getUniform("radius");
    }
}
