package com.corosus.watut.cloudRendering;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.WatutMod;
import com.corosus.watut.cloudRendering.test.BufferDataExample;
import com.corosus.watut.cloudRendering.threading.ThreadedCloudBuilder;
import com.corosus.watut.cloudRendering.threading.vanillaThreaded.ThreadedBufferBuilderPersistentStorage;
import com.corosus.watut.cloudRendering.threading.vanillaThreaded.ThreadedVertexBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL33;

import java.util.*;

public class CloudRenderHandler {

    private Random rand = new Random();

    private boolean mode_triangles = true;
    public Frustum cullingFrustum;
    private int skyChunkRenderRadius = 1;
    private boolean generateClouds = true;

    private Vec3 vecCam = Vec3.ZERO;
    private ThreadedCloudBuilder threadedCloudBuilder = new ThreadedCloudBuilder();

    public CloudRenderHandler() {
        threadedCloudBuilder.start();
    }

    private ClientLevel getLevel() {
        return Minecraft.getInstance().level;
    }

    private int getTicks() {
        return (int) getLevel().getGameTime();
    }

    public ThreadedCloudBuilder getThreadedCloudBuilder() {
        return threadedCloudBuilder;
    }

    public synchronized void updateCameraPosition(double camX, double camY, double camZ) {
        vecCam = new Vec3(camX, camY, camZ);
    }

    public void renderClouds(PoseStack p_254145_, Matrix4f p_254537_, float p_254364_, double camX, double camY, double camZ) {
        //if (true) return;
        if (WatutMod.cloudShader == null) return;
        if (getLevel().effects().renderClouds(getLevel(), getTicks(), p_254364_, p_254145_, camX, camY, camZ, p_254537_))
            return;

        updateCameraPosition(camX, camY, camZ);

        RenderSystem.enableCull();
        //RenderSystem.disableCull();
        //RenderSystem.enableBlend();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        //RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(true);

        /*if (!threadedCloudBuilder.isRunning() && !threadedCloudBuilder.isWaitingToUploadData()) {
            if (getTicks() % (20 * 5) == 0) {
                generateClouds = true;
            }
        }*/

        //if (this.generateClouds) {

            //System.out.println("gen clouds " + getTicks());

            threadedCloudBuilder.setMultiBufferMode(true);
            int columns = 30;
            threadedCloudBuilder.setCloudCount(columns * columns);
            threadedCloudBuilder.setColumns(columns);
            //threadedCloudBuilder.setCloudCount(5 * 5);
            //threadedCloudBuilder.setCloudCount(1);

            threadedCloudBuilder.setSizeX(40);
            threadedCloudBuilder.setSizeY(30);
            threadedCloudBuilder.setSizeZ(40);

            threadedCloudBuilder.setScale(1);
            threadedCloudBuilder.setCloudsY(200 / threadedCloudBuilder.getScale());
            skyChunkRenderRadius = 0;

            //initSkyChunksForGrid();

            this.generateClouds = false;
        //}

        if (threadedCloudBuilder.getSyncState() == ThreadedCloudBuilder.SyncState.IDLE) {
            threadedCloudBuilder.setSyncState(ThreadedCloudBuilder.SyncState.MAINTHREADUPLOADINGVBO);
            for (Iterator<Map.Entry<Long, SkyChunk>> it = threadedCloudBuilder.getQueueWaitingForUploadSkyChunks().entrySet().iterator(); it.hasNext(); ) {

                Map.Entry<Long, SkyChunk> entry = it.next();
                SkyChunk skyChunk = entry.getValue();

                RenderableData renderableData = skyChunk.getRenderableData();

                //TODO: for now we might not need this, but to fix the thread conflict from using upload, it might be needed
                //renderableData.swapBuffers();
                renderableData.getActiveRenderingVertexBuffer().bind();

                if (WatutMod.threadedBufferBuilder == null) {
                    GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, renderableData.getActiveRenderingVertexBuffer().getVertexBufferId());
                    WatutMod.threadedBufferBuilder = new ThreadedBufferBuilderPersistentStorage(2097152 * 10);
                    GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
                }

                if (renderableData.getVbo() != null) {
                    renderableData.getActiveRenderingVertexBuffer().upload(renderableData.getVbo());
                }
                //WatutMod.threadedBufferBuilder.clear();
                //renderableData.getVbo().release();
                skyChunk.setInitialized(true);
                //skyChunk.setCameraPosForRender(skyChunk.getCameraPosDuringBuild());
                skyChunk.pushNewOffThreadDataToMainThread();
                //ThreadedVertexBuffer.unbind();
                ThreadedVertexBuffer.unbind();
                //skyChunk.setBeingBuilt(false);

                //remove from upload queue
                it.remove();

            }
            if (WatutMod.threadedBufferBuilder != null) {
                //CULog.log("getLastNextElementByte " + WatutMod.threadedBufferBuilder.getLastNextElementByte());
                //CULog.log("getRenderedBufferCount " + WatutMod.threadedBufferBuilder.getRenderedBufferCount());
            }
            threadedCloudBuilder.setSyncState(ThreadedCloudBuilder.SyncState.IDLE);
        }

        /*if (!threadedCloudBuilder.isRunning()) {
            if (threadedCloudBuilder.isWaitingToUploadData()) {

                for (SkyChunk skyChunk : SkyChunkManager.instance().getSkyChunks().values()) {

                    RenderableData renderableData = skyChunk.getRenderableData();

                    //TODO: for now we might not need this, but to fix the thread conflict from using upload, it might be needed
                    //renderableData.swapBuffers();
                    renderableData.getActiveRenderingVertexBuffer().bind();
                    renderableData.getActiveRenderingVertexBuffer().upload(renderableData.getVbo());
                    skyChunk.setInitialized(true);
                    //skyChunk.setCameraPosForRender(skyChunk.getCameraPosDuringBuild());
                    skyChunk.pushNewOffThreadDataToMainThread();
                    skyChunk.setBeingBuilt(false);
                    ThreadedVertexBuffer.unbind();

                }

                //System.out.println("=============== upload done");
                threadedCloudBuilder.setWaitingToUploadData(false);
            }
        }*/

            /*if (WatutMod.cloudShader != null) {
                RenderSystem.setShader(() -> WatutMod.cloudShader);
            } else {
                RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
            }*/

        RenderSystem.setShader(() -> WatutMod.cloudShader);
        //RenderSystem.setupShaderLights(WatutMod.cloudShader);
        long time = getTicks();
        float speed = 0.05F;
        float x = (float) -Math.sin(time * speed);
        float z = (float) Math.cos(time * speed);

        if (WatutMod.cloudShader.LIGHT0_DIRECTION2 != null) {

            //WatutMod.cloudShader.LIGHT0_DIRECTION2.set(new Vector3f(x, 1, z));
            //WatutMod.cloudShader.LIGHT1_DIRECTION2.set(new Vector3f(0, x, 0));
                /*WatutMod.cloudShader.LIGHT0_DIRECTION2.set(new Vector3f(1, 0, 1));
                WatutMod.cloudShader.LIGHT1_DIRECTION2.set(new Vector3f(x, 0, z));*/

            WatutMod.cloudShader.LIGHT0_DIRECTION2.set(new Vector3f(1, 0, 1));
            WatutMod.cloudShader.LIGHT1_DIRECTION2.set(new Vector3f(1, 0, 1));
        }
        //RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
        RenderSystem.setShaderTexture(0, ParticleRegistry.idle.getSprite().atlasLocation());
        //FogRenderer.levelFogColor();
        p_254145_.pushPose();
        //p_254145_.scale(3.0F, 3.0F, 3.0F);
        //p_254145_.scale(10.0F, 10.0F, 10.0F);
        //p_254145_.translate(-f3, f4, -f5);

        //TODO: remove use
        //p_254145_.translate(-p_253843_, -p_253663_, -p_253795_);

        float timeShort = (this.getTicks() % (20 * 30)) * 3F;

        //p_254145_.translate(((timeShort + p_254364_)) * 0.03F, 0, 0);

        boolean renderClouds = false;

        if (renderClouds) {
            if (threadedCloudBuilder.isMultiBufferMode()) {

                Random rand3 = new Random();

                List<SkyChunk> skyChunkList = getListOfSkyChunksForRender();
                for (SkyChunk skyChunk : getListOfSkyChunksForRender()) {
                    RenderableData renderableData = skyChunk.getRenderableData();
                    //if (renderableData.getVbo() == null) continue;
                    try {
                        Vec3 vecCamVBO = skyChunk.getCameraPosForRender();
                        WatutMod.cloudShader.VBO_RENDER_POS.set(new Vector3f((float)(vecCamVBO.x - vecCam.x), (float) (vecCamVBO.y - vecCam.y), (float) (vecCamVBO.z - vecCam.z)));

                        if (skyChunk.isInitialized()) {
                            renderableData.getActiveRenderingVertexBuffer().bind();

                            RenderSystem.colorMask(true, true, true, true);
                            if (skyChunk.isClientCameraInCloudForSkyChunk()) {
                                skyChunk.setClientCameraInCloudForSkyChunk(false);
                                RenderSystem.disableCull();
                            }

                            if (rand3.nextFloat() > 0.993F && false) {
                                Vector3f vec = new Vector3f(renderableData.getLightningPos());
                                //TODO: skychunk changes, needs to be reworked, lightning could bleed into another chunk, for now just contain within chunk
                                //vec.add(rand3.nextFloat() * threadedCloudBuilder.getSizeX(), rand3.nextFloat() * threadedCloudBuilder.getSizeY()/* + rand3.nextFloat(80)*/, rand3.nextFloat() * threadedCloudBuilder.getSizeZ());
                                vec.add(rand3.nextFloat() * SkyChunk.size, rand3.nextFloat() * SkyChunk.size, rand3.nextFloat() * SkyChunk.size);
                                WatutMod.cloudShader.LIGHTNING_POS.set(vec);
                            } else {
                                WatutMod.cloudShader.LIGHTNING_POS.set(new Vector3f(0, -999, 0));
                            }

                            ShaderInstance shaderinstance = RenderSystem.getShader();
                            renderableData.getActiveRenderingVertexBuffer().drawWithShader(p_254145_.last().pose(), p_254537_, shaderinstance);
                            VertexBuffer.unbind();

                            RenderSystem.enableCull();
                        }
                    } catch (Exception exception) {

                    }

                }
            }
        }

        p_254145_.popPose();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public List<SkyChunk> getListOfSkyChunksForBuilding() {
        BlockPos cameraSkyChunk = SkyChunk.worldPosToChunkPos(vecCam.x / threadedCloudBuilder.getScale(), 0, vecCam.z / threadedCloudBuilder.getScale());
        return getListOfSkyChunks(true, new Vec3i(skyChunkRenderRadius, 0, skyChunkRenderRadius), cameraSkyChunk);
    }

    public List<SkyChunk> getListOfSkyChunksForRender() {
        BlockPos cameraSkyChunk = SkyChunk.worldPosToChunkPos(vecCam.x / threadedCloudBuilder.getScale(), vecCam.y / threadedCloudBuilder.getScale(), vecCam.z / threadedCloudBuilder.getScale());
        return getListOfSkyChunks(false, new Vec3i(skyChunkRenderRadius, skyChunkRenderRadius, skyChunkRenderRadius), cameraSkyChunk);
    }

    public List<SkyChunk> getListOfSkyChunks(boolean createIfMissing, Vec3i size, BlockPos cameraSkyChunk) {
        List<SkyChunk> skyChunkList = new ArrayList<>();
        if (vecCam == Vec3.ZERO) return skyChunkList;
        for (int xx = -size.getX(); xx <= size.getX(); xx++) {
            for (int yy = -size.getY(); yy <= size.getY(); yy++) {
                for (int zz = -size.getZ(); zz <= size.getZ(); zz++) {
                    SkyChunk skyChunk;
                    if (createIfMissing) {
                        skyChunk = SkyChunkManager.instance().getSkyChunk(cameraSkyChunk.getX() + xx, cameraSkyChunk.getY() + yy, cameraSkyChunk.getZ() + zz);
                    } else {
                        skyChunk = SkyChunkManager.instance().getSkyChunkIfExists(cameraSkyChunk.getX() + xx, cameraSkyChunk.getY() + yy, cameraSkyChunk.getZ() + zz);
                    }

                    if (skyChunk != null) {
                        skyChunkList.add(skyChunk);
                    }
                }
            }
        }
        return skyChunkList;
    }

    //init skychunks so buffers are made on render thread
    //TODO: find a more elegant solution maybe?
    /*public void initSkyChunksForGrid() {
        int columns = 20;
        int startX = 0;
        int startY = threadedCloudBuilder.getCloudsY();
        int startZ = 0;
        int sizeX = columns * threadedCloudBuilder.getSizeX();
        int sizeY = threadedCloudBuilder.getSizeY();
        int sizeZ = columns * threadedCloudBuilder.getSizeZ();
        for (int x = startX; x <= startX + sizeX; x++) {
            for (int z = startZ; z <= startZ + sizeZ; z++) {
                for (int y = startY; y <= startY + sizeY; y++) {
                    SkyChunkManager.instance().getSkyChunk(x >> 4, y >> 4, z >> 4);
                }
            }
        }
    }*/
}
