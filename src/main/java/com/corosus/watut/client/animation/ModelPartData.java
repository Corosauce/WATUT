package com.corosus.watut.client.animation;

import com.corosus.watut.status.PlayerStatus;
import net.minecraft.util.Mth;

/**
 * Contiene i dati di posizione, rotazione e scala di una parte del modello 3D del giocatore.
 */
public class ModelPartData {
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;

    public ModelPartData copyPartialLerp(PlayerStatus playerStatus, ModelPartData prev, float partialTick) {
        ModelPartData target = new ModelPartData();
        float lerp = playerStatus.getPartialLerp(partialTick);

        target.x = Mth.lerp(lerp, prev.x, this.x);
        target.y = Mth.lerp(lerp, prev.y, this.y);
        target.z = Mth.lerp(lerp, prev.z, this.z);
        target.xRot = Mth.lerp(lerp, prev.xRot, this.xRot);
        target.yRot = Mth.lerp(lerp, prev.yRot, this.yRot);
        target.zRot = Mth.lerp(lerp, prev.zRot, this.zRot);
        target.xScale = Mth.lerp(lerp, prev.xScale, this.xScale);
        target.yScale = Mth.lerp(lerp, prev.yScale, this.yScale);
        target.zScale = Mth.lerp(lerp, prev.zScale, this.zScale);

        return target;
    }

    public ModelPartData copy() {
        ModelPartData data = new ModelPartData();
        data.x = this.x;
        data.y = this.y;
        data.z = this.z;
        data.xRot = this.xRot;
        data.yRot = this.yRot;
        data.zRot = this.zRot;
        data.xScale = this.xScale;
        data.yScale = this.yScale;
        data.zScale = this.zScale;
        return data;
    }
}
