package com.corosus.watut.math;

import com.corosus.watut.PlayerStatus;
import net.minecraft.util.Mth;

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

    public ModelPartData copyPartialLerp(PlayerStatus playerStatus, ModelPartData modelPartDataPrev, float partialTick) {
        //float partialTick = 0;
        ModelPartData modelPartData = new ModelPartData();

        modelPartData.x = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.x, this.x);
        modelPartData.y = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.y, this.y);
        modelPartData.z = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.z, this.z);
        modelPartData.xRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.xRot, this.xRot);
        modelPartData.yRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.yRot, this.yRot);
        modelPartData.zRot = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.zRot, this.zRot);
        modelPartData.xScale = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.xScale, this.xScale);
        modelPartData.yScale = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.yScale, this.yScale);
        modelPartData.zScale = Mth.lerp(playerStatus.getPartialLerp(partialTick), modelPartDataPrev.zScale, this.zScale);
        return modelPartData;
    }

    public ModelPartData copyOld() {
        ModelPartData modelPartData = new ModelPartData();

        modelPartData.x = this.x;
        modelPartData.y = this.y;
        modelPartData.z = this.z;
        modelPartData.xRot = this.xRot;
        modelPartData.yRot = this.yRot;
        modelPartData.zRot = this.zRot;
        modelPartData.xScale = this.xScale;
        modelPartData.yScale = this.yScale;
        modelPartData.zScale = this.zScale;
        return modelPartData;
    }

    /**
     * Replaces NaN/infinite values with neutral ones, a model part with a non finite transform isnt rendered
     *
     * @return true if anything had to be replaced
     */
    public boolean sanitize() {
        boolean changed = false;
        if (!Float.isFinite(x)) { x = 0; changed = true; }
        if (!Float.isFinite(y)) { y = 0; changed = true; }
        if (!Float.isFinite(z)) { z = 0; changed = true; }
        if (!Float.isFinite(xRot)) { xRot = 0; changed = true; }
        if (!Float.isFinite(yRot)) { yRot = 0; changed = true; }
        if (!Float.isFinite(zRot)) { zRot = 0; changed = true; }
        if (!Float.isFinite(xScale)) { xScale = 1; changed = true; }
        if (!Float.isFinite(yScale)) { yScale = 1; changed = true; }
        if (!Float.isFinite(zScale)) { zScale = 1; changed = true; }
        return changed;
    }

}
