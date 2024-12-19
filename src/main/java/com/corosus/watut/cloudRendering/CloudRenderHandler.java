package com.corosus.watut.cloudRendering;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.WatutMod;
import com.corosus.watut.cloudRendering.threading.ThreadedCloudBuilder;
import com.corosus.watut.cloudRendering.threading.vanillaThreaded.ThreadedVertexBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CloudRenderHandler {

    private Random rand = new Random();

    private boolean mode_triangles = true;
    public Frustum cullingFrustum;
    private int skyChunkRenderRadius = 1;

    private Vec3 vecCam = Vec3.ZERO;
    private ThreadedCloudBuilder threadedCloudBuilder = new ThreadedCloudBuilder();

    private int lastScale = 4;

    private Vec3 posCloudOffset = new Vec3(0, 0, 0);

    public Vec3 getPosCloudOffset() {
        return posCloudOffset;
    }

    public void setPosCloudOffset(Vec3 posCloudOffset) {
        this.posCloudOffset = posCloudOffset;
    }

    public CloudRenderHandler() {
        threadedCloudBuilder.start();
    }

    private ClientLevel getLevel() {
        return Minecraft.getInstance().level;
    }

    private int getTicks() {
        if (getLevel() == null) return 0;
        return (int) getLevel().getGameTime();
    }

    public ThreadedCloudBuilder getThreadedCloudBuilder() {
        return threadedCloudBuilder;
    }

    public synchronized void updateCameraPosition(double camX, double camY, double camZ) {
        vecCam = new Vec3(camX, camY, camZ);
    }

    public void tickClient() {
        long time = getTicks();
        if (time % (20 * 60 * 5) == 0) {
            posCloudOffset = new Vec3(0, 0, 0);
        }

        posCloudOffset = posCloudOffset.add(0.03F, 0, 0);
    }

    public void renderClouds(PoseStack p_254145_, Matrix4f p_254537_, float p_254364_, double camX, double camY, double camZ) {
        if (true) return;
        if (WatutMod.cloudShader == null) return;
        if (getLevel().effects().renderClouds(getLevel(), getTicks(), p_254364_, p_254145_, camX, camY, camZ, p_254537_))
            return;

        updateCameraPosition(camX, camY, camZ);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        int columns = 5;
        threadedCloudBuilder.setCloudCount(columns * columns);
        threadedCloudBuilder.setCloudCount(1);
        threadedCloudBuilder.setColumns(columns);
        //threadedCloudBuilder.setCloudCount(5 * 5);
        //threadedCloudBuilder.setCloudCount(1);

        threadedCloudBuilder.setSizeX(40);
        threadedCloudBuilder.setSizeY(30);
        threadedCloudBuilder.setSizeZ(40);

        threadedCloudBuilder.setScale(4);
        //for live testing, this can still crash thread if changed live
        if (threadedCloudBuilder.getScale() != lastScale) {
            SkyChunkManager.instance().getSkyChunks().clear();
            lastScale = threadedCloudBuilder.getScale();
        }
        //threadedCloudBuilder.setScale(1);
        threadedCloudBuilder.setCloudsY(256);
        //skyChunkRenderRadius = Minecraft.getInstance().options.renderDistance().get() / 4;
        //skyChunkRenderRadius = Math.max(512 / (threadedCloudBuilder.getScale() * SkyChunk.size), 1) + 1;
        skyChunkRenderRadius = Math.max(768 / (threadedCloudBuilder.getScale() * SkyChunk.size), 1) + 1;
        //skyChunkRenderRadius = 1;

        if (threadedCloudBuilder.getSyncState() == ThreadedCloudBuilder.SyncState.IDLE) {
            ConcurrentHashMap<Long, SkyChunk> map = threadedCloudBuilder.getQueueWaitingForUploadSkyChunks();
            int processCount = 0;
            threadedCloudBuilder.setSyncState(ThreadedCloudBuilder.SyncState.MAINTHREADUPLOADINGVBO);
            boolean info = false;
            if (map.size() > 0) {
                info = true;
                //CULog.log("size " + map.size());
            }
            int maxPerTick = 2;
            if (map.size() > 200) {
                maxPerTick = map.size() / 10;
            }
            for (Iterator<Map.Entry<Long, SkyChunk>> it = map.entrySet().iterator(); processCount < maxPerTick && it.hasNext(); ) {

                Map.Entry<Long, SkyChunk> entry = it.next();
                SkyChunk skyChunk = entry.getValue();

                RenderableData renderableData = skyChunk.getRenderableData();

                //TODO: for now we might not need this, but to fix the thread conflict from using upload, it might be needed
                //renderableData.swapBuffers();
                renderableData.getActiveRenderingVertexBuffer().bind();
                long time = System.currentTimeMillis();
                renderableData.getActiveRenderingVertexBuffer().upload(renderableData.getVbo());
                ThreadedVertexBuffer.unbind();

                renderableData.getVertexBufferAddedPoints().bind();
                renderableData.getVertexBufferAddedPoints().upload(renderableData.getVboAddedPoints());
                ThreadedVertexBuffer.unbind();

                renderableData.getVertexBufferRemovedPoints().bind();
                renderableData.getVertexBufferRemovedPoints().upload(renderableData.getVboRemovedPoints());
                ThreadedVertexBuffer.unbind();

                //CULog.log("upload time: " + (System.currentTimeMillis() - time) + " - " + skyChunk.getPointsOffThread().size() + " points");
                skyChunk.setInitialized(true);
                //skyChunk.setCameraPosForRender(skyChunk.getCameraPosDuringBuild());
                skyChunk.pushNewOffThreadDataToMainThread();
                skyChunk.setLastUploadTime(getTicks());
                //skyChunk.setBeingBuilt(false);

                //remove from upload queue
                it.remove();
                skyChunk.setWaitingToUploadData(false);
                processCount++;

            }
            threadedCloudBuilder.setSyncState(ThreadedCloudBuilder.SyncState.IDLE);

            if (info) {
                //CULog.log("size " + map.size());
                //CULog.log("getLastNextElementByte " + WatutMod.threadedBufferBuilder.getLastNextElementByte());
            }
        }

        RenderSystem.setShader(() -> WatutMod.cloudShader);
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

        Vec3 vecCloudColor = getLevel().getCloudColor(1F);

        if (WatutMod.cloudShader.CLOUD_COLOR != null) {

            //WatutMod.cloudShader.LIGHT0_DIRECTION2.set(new Vector3f(x, 1, z));
            //WatutMod.cloudShader.LIGHT1_DIRECTION2.set(new Vector3f(0, x, 0));
            /*WatutMod.cloudShader.LIGHT0_DIRECTION2.set(new Vector3f(1, 0, 1));
            WatutMod.cloudShader.LIGHT1_DIRECTION2.set(new Vector3f(x, 0, z));*/


        }
        if (WatutMod.cloudShader.FOG_START != null) {
            WatutMod.cloudShader.FOG_START.set(0F);
            WatutMod.cloudShader.FOG_END.set(50F);
        }

        RenderSystem.setShaderTexture(0, ParticleRegistry.idle.getSprite().atlasLocation());
        p_254145_.pushPose();
        p_254145_.translate(posCloudOffset.x, posCloudOffset.y, posCloudOffset.z);

        boolean renderClouds = true;

        int renderCount = 0;
        if (renderClouds) {
            Random rand3 = new Random();
            //List<SkyChunk> asd = getListOfSkyChunksForRender();
            for (SkyChunk skyChunk : getListOfSkyChunksForRender()) {
                RenderableData renderableData = skyChunk.getRenderableData();

                Frustum frustum = Minecraft.getInstance().levelRenderer.getFrustum();
                int scale = threadedCloudBuilder.getScale();
                BlockPos pos = skyChunk.getWorldPos().multiply(scale);
                AABB aabb = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + (SkyChunk.size * scale), pos.getY() + (SkyChunk.size * scale), pos.getZ() + (SkyChunk.size * scale));
                //TODO: fix buggy frustum check, probably my AABB is bad
                if (frustum.isVisible(aabb) || true) {
                    renderCount++;
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

                        //TEMP
                        //float fade = (float) Math.sin((float)ticksSinceVBOUpdate * 0.2F) * 0.5F + 0.49F;//Mth.clamp(0, 9999, );
                        long timeSinceUpload = time - skyChunk.getLastUploadTime();
                        int fadeTicks = 20 * 2;
                        //fadeTicks = 1;
                        float fade = timeSinceUpload * (1F / fadeTicks);
                        if (timeSinceUpload > fadeTicks) {
                            fade = 1F;
                        }
                        //fade = 1F;
                        //WatutMod.cloudShader.LIGHTNING_POS.set(new Vector3f(1F, 0, 0));
                        WatutMod.cloudShader.CLOUD_COLOR.set(new Vector4f((float) vecCloudColor.x, (float) vecCloudColor.y, (float) vecCloudColor.z, 1F));

                        ShaderInstance shaderinstance = RenderSystem.getShader();
                        renderableData.getActiveRenderingVertexBuffer().drawWithShader(p_254145_.last().pose(), p_254537_, shaderinstance);
                        ThreadedVertexBuffer.unbind();

                        //WatutMod.cloudShader.LIGHTNING_POS.set(new Vector3f(fade, 0, 0));
                        WatutMod.cloudShader.CLOUD_COLOR.set(new Vector4f((float) vecCloudColor.x, (float) vecCloudColor.y, (float) vecCloudColor.z, fade));

                        renderableData.getVertexBufferAddedPoints().bind();
                        renderableData.getVertexBufferAddedPoints().drawWithShader(p_254145_.last().pose(), p_254537_, shaderinstance);
                        ThreadedVertexBuffer.unbind();

                        //currently bugged, turned off the inverting part
                        if (fade < 1F) {
                            //WatutMod.cloudShader.LIGHTNING_POS.set(new Vector3f(1 - fade, 1, 0));
                            //put into negatives to shader knows to use inverted dithering
                            //WatutMod.cloudShader.CLOUD_COLOR.set(new Vector4f((float) vecCloudColor.x, (float) vecCloudColor.y, (float) vecCloudColor.z, 1 - fade - 1));
                            //WatutMod.cloudShader.CLOUD_COLOR.set(new Vector4f((float) vecCloudColor.x, (float) vecCloudColor.y, (float) vecCloudColor.z, 0.99F));
                            WatutMod.cloudShader.CLOUD_COLOR.set(new Vector4f((float) vecCloudColor.x, (float) vecCloudColor.y, (float) vecCloudColor.z, 1 - fade));

                            renderableData.getVertexBufferRemovedPoints().bind();
                            renderableData.getVertexBufferRemovedPoints().drawWithShader(p_254145_.last().pose(), p_254537_, shaderinstance);
                            ThreadedVertexBuffer.unbind();
                        }

                        RenderSystem.enableCull();
                    }
                }
            }

            //if (time % 10 == 0)
                //CULog.log("renderCount " + renderCount);
        }

        p_254145_.popPose();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public List<SkyChunk> getListOfSkyChunksForBuilding() {
        BlockPos cameraSkyChunk = SkyChunk.worldPosToChunkPos((vecCam.x / threadedCloudBuilder.getScale()) + 0, vecCam.y / threadedCloudBuilder.getScale(), vecCam.z / threadedCloudBuilder.getScale());
        return getListOfSkyChunks(true, new Vec3i(skyChunkRenderRadius, skyChunkRenderRadius, skyChunkRenderRadius), cameraSkyChunk);
    }

    public List<SkyChunk> getListOfSkyChunksForAlgoClouds() {
        BlockPos cameraSkyChunkAtCloudHeight = SkyChunk.worldPosToChunkPos((vecCam.x / threadedCloudBuilder.getScale()) + 0, threadedCloudBuilder.getCloudsY() / threadedCloudBuilder.getScale(), vecCam.z / threadedCloudBuilder.getScale());
        //int cloudHeightChunk = threadedCloudBuilder.getCloudsY() / (SkyChunk.size * threadedCloudBuilder.getScale());
        return getListOfSkyChunks(true, new Vec3i(skyChunkRenderRadius, 0, skyChunkRenderRadius), cameraSkyChunkAtCloudHeight);
    }

    public List<SkyChunk> getListOfSkyChunksForRender() {
        BlockPos cameraSkyChunk = SkyChunk.worldPosToChunkPos((vecCam.x / threadedCloudBuilder.getScale()) + 0, vecCam.y / threadedCloudBuilder.getScale(), vecCam.z / threadedCloudBuilder.getScale());
        return getListOfSkyChunks(false, new Vec3i(skyChunkRenderRadius, skyChunkRenderRadius, skyChunkRenderRadius), cameraSkyChunk);
    }

    public List<SkyChunk> getListOfSkyChunks(boolean createIfMissing, Vec3i size, BlockPos cameraSkyChunk) {
        List<SkyChunk> skyChunkList = new ArrayList<>();
        if (vecCam == Vec3.ZERO) return skyChunkList;
        for (int xx = -size.getX(); xx <= size.getX(); xx++) {
            for (int yy = -size.getY(); yy <= size.getY(); yy++) {
                for (int zz = -size.getZ(); zz <= size.getZ(); zz++) {
                    //float dist = Vector3f.distance(xx * SkyChunk.size, yy * SkyChunk.size, zz * SkyChunk.size, SkyChunk.size / 2, SkyChunk.size / 2, SkyChunk.size / 2);
                    float dist = Vector3f.distance(xx, yy, zz, 0, 0, 0);
                    //if (dist <= skyChunkRenderRadius) {
                        SkyChunk skyChunk;
                        if (createIfMissing) {
                            skyChunk = SkyChunkManager.instance().getSkyChunk(cameraSkyChunk.getX() + xx, cameraSkyChunk.getY() + yy, cameraSkyChunk.getZ() + zz);
                        } else {
                            skyChunk = SkyChunkManager.instance().getSkyChunkIfExists(cameraSkyChunk.getX() + xx, cameraSkyChunk.getY() + yy, cameraSkyChunk.getZ() + zz);
                        }

                        if (skyChunk != null) {
                            skyChunkList.add(skyChunk);
                        }
                    //}
                }
            }
        }
        return skyChunkList;
    }
}
