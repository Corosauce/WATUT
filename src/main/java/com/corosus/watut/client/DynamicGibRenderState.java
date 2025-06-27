//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.corosus.watut.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DynamicGibRenderState extends LivingEntityRenderState {
    public float rollAmount;
    public boolean hasStinger = true;
    public boolean isOnGround;
    public boolean isAngry;
    public boolean hasNectar;

    public DynamicGibRenderState() {
    }
}
