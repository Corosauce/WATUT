package com.corosus.watut.loader.forge.lazydfu.mixin;

import net.minecraft.util.datafix.DataFixers;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DataFixers.class)
public class SchemasMixin {

    /**
     * @author Corosus
     * @reason stop mojangs rediculous and pointless cpu usage on load that no modpack user would ever benefit from
     */
    /*@Overwrite
    private static DataFixer createFixerUpper() {
        LazyDataFixerBuilder datafixerbuilder = new LazyDataFixerBuilder(SharedConstants.WORLD_VERSION);
        DataFixers.addFixers(datafixerbuilder);
        return datafixerbuilder.build(Util.bootstrapExecutor());
    }*/
}