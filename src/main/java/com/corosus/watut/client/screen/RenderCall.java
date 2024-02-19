package com.corosus.watut.client.screen;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class RenderCall {

    private RenderCallType renderCallType;
    private List<Object> listParams = new ArrayList<>();

    public RenderCall(RenderCallType renderCallType) {
        this.renderCallType = renderCallType;
    }

    public void innerBlit(ResourceLocation pAtlasLocation, int pX1, int pX2, int pY1, int pY2, int pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV) {
        this.listParams.add(pAtlasLocation);
        this.listParams.add(pX1);
        this.listParams.add(pX2);
        this.listParams.add(pY1);
        this.listParams.add(pY2);
        this.listParams.add(pBlitOffset);
        this.listParams.add(pMinU);
        this.listParams.add(pMaxU);
        this.listParams.add(pMinV);
        this.listParams.add(pMaxV);
    }

    public void innerBlit(ResourceLocation pAtlasLocation, int pX1, int pX2, int pY1, int pY2, int pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV, float pRed, float pGreen, float pBlue, float pAlpha) {
        this.listParams.add(pAtlasLocation);
        this.listParams.add(pX1);
        this.listParams.add(pX2);
        this.listParams.add(pY1);
        this.listParams.add(pY2);
        this.listParams.add(pBlitOffset);
        this.listParams.add(pMinU);
        this.listParams.add(pMaxU);
        this.listParams.add(pMinV);
        this.listParams.add(pMaxV);
        this.listParams.add(pRed);
        this.listParams.add(pGreen);
        this.listParams.add(pBlue);
        this.listParams.add(pAlpha);
    }

    public RenderCallType getRenderCallType() {
        return renderCallType;
    }

    public void setRenderCallType(RenderCallType renderCallType) {
        this.renderCallType = renderCallType;
    }

    public List<Object> getListParams() {
        return listParams;
    }

    public void setListParams(List<Object> listParams) {
        this.listParams = listParams;
    }
}
