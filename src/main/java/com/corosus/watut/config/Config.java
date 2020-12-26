package com.corosus.watut.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static net.minecraftforge.common.ForgeConfigSpec.*;

@EventBusSubscriber
public class Config {

    private static final Builder CLIENT_BUILDER = new Builder();

    public static final CategoryGeneral GENERAL = new CategoryGeneral();

    public static final class CategoryGeneral {

        public final DoubleValue tileEntityRenderRangeMax;
        public final DoubleValue entityRenderRangeMax;

        public final BooleanValue tileEntityRenderLimitModdedOnly;
        public final BooleanValue entityRenderLimitModdedOnly;

        private CategoryGeneral() {
            CLIENT_BUILDER.comment("General mod settings").push("general");

            tileEntityRenderRangeMax = CLIENT_BUILDER
                    .defineInRange("tileEntityRenderRangeMax", 24, 1D, 30000);

            entityRenderRangeMax = CLIENT_BUILDER
                    .defineInRange("entityRenderRangeMax", 64, 1D, 30000);

            tileEntityRenderLimitModdedOnly = CLIENT_BUILDER
                    .define("tileEntityRenderLimitModdedOnly", true);

            entityRenderLimitModdedOnly = CLIENT_BUILDER
                    .define("entityRenderLimitModdedOnly", true);

            CLIENT_BUILDER.pop();
        }
    }
    public static final ForgeConfigSpec CLIENT_CONFIG = CLIENT_BUILDER.build();
}
