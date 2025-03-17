package com.corosus.watut;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ShaderReloader extends SimplePreparableReloadListener {

    @Override
    protected Object prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return null;
    }

    @Override
    protected void apply(Object o, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        ShaderRegistry.reloadShaders();
    }

}
