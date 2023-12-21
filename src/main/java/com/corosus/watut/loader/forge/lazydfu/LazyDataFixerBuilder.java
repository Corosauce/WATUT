package com.corosus.watut.loader.forge.lazydfu;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;

import java.util.Set;
import java.util.concurrent.Executor;

public class LazyDataFixerBuilder extends DataFixerBuilder {

    private static final Executor NO_OP_EXECUTOR = command -> {};

    public LazyDataFixerBuilder(int dataVersion) {
        super(dataVersion);
    }

    @Override
    public DataFixer buildOptimized(final Set<DSL.TypeReference> requiredTypes, final Executor executor) {
        return super.buildOptimized(requiredTypes, NO_OP_EXECUTOR);
    }
}