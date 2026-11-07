package com.corosus.watut;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public class WatutModClient {
    public static Player getPlayer()
    {
        return Minecraft.getInstance().player;
    }

    public static boolean isClientPlayerInTropics() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.level().dimension() == WatutMod.TROPICS;
    }

}
