package com.corosus.watut.cloudRendering.threading;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.PerlinNoiseHelper;
import com.corosus.watut.WatutMod;
import com.corosus.watut.cloudRendering.Cloud;
import com.corosus.watut.cloudRendering.SkyChunk;
import com.corosus.watut.cloudRendering.SkyChunkManager;
import com.corosus.watut.cloudRendering.threading.vanillaThreaded.ThreadedBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadedCloudBuilder {

    public enum SyncState {
        IDLE,
        OFFTHREADBUILDINGVBO,
        MAINTHREADUPLOADINGVBO
    }

    private Random rand = new Random();
    private Random rand2 = new Random();

    private int cloudCount = 150;

    private int quadCount = 0;
    private int pointCount = 0;

    private long timeOffset = 0;

    private int sizeX = 30;
    private int sizeY = 20;
    private int sizeZ = 30;

    private int cloudsY = 140;
    private int scale = 4;
    private int columns = 10;

    private Cloud cloudShape = new Cloud(sizeX, sizeY, sizeZ);
    private Cloud cloudShape2 = new Cloud(sizeX, sizeY, sizeZ);
    private boolean cloudShapeNeedsPrecalc = true;
    private boolean cloudShape2NeedsPrecalc = true;
    private SyncState syncState = SyncState.IDLE;
    private long lastBuildTime = 0;
    private int rebuildFrequency = 20*5;

    //private boolean isRunning = false;

    private ThreadedCloudBuilderJob threadedCloudBuilderJob;

    //if i split vbo building into multiple threads this needs to change
    Vec3 camVec = Vec3.ZERO;

    public boolean testOnce = false;

    //private ConcurrentHashMap<Long, SkyChunk> queueUpdateSkyChunks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, SkyChunk> queueWaitingForUploadSkyChunks = new ConcurrentHashMap<>();

    public synchronized SyncState getSyncState() {
        return syncState;
    }

    public synchronized void setSyncState(SyncState syncState) {
        this.syncState = syncState;
    }

    public Vec3 getCamVec() {
        return camVec;
    }

    public void setCamVec(Vec3 camVec) {
        this.camVec = camVec;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }/*

    public ConcurrentHashMap<Long, SkyChunk> getQueueUpdateSkyChunks() {
        return queueUpdateSkyChunks;
    }*/

    public ConcurrentHashMap<Long, SkyChunk> getQueueWaitingForUploadSkyChunks() {
        return queueWaitingForUploadSkyChunks;
    }

    public int getCloudCount() {
        return cloudCount;
    }

    public void setCloudCount(int cloudCount) {
        this.cloudCount = cloudCount;
    }

    public int getSizeX() {
        return sizeX;
    }

    public void setSizeX(int sizeX) {
        this.sizeX = sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public void setSizeY(int sizeY) {
        this.sizeY = sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public void setSizeZ(int sizeZ) {
        this.sizeZ = sizeZ;
    }

    public int getCloudsY() {
        return cloudsY;
    }

    public void setCloudsY(int cloudsY) {
        this.cloudsY = cloudsY;
    }

    public synchronized void start() {
        threadedCloudBuilderJob = new ThreadedCloudBuilderJob(this);
        threadedCloudBuilderJob.start();
    }

    public boolean tickThreaded() {
        if (true) return false;
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().cameraEntity == null) {
            return false;
        }

        //dont touch vbos until we've finished uploading them, prevents buffer infinite growth issue
        if (syncState != SyncState.IDLE || getQueueWaitingForUploadSkyChunks().size() != 0) {
            return false;
        }

        boolean buildSlowClouds = lastBuildTime <= Minecraft.getInstance().level.getGameTime();
        boolean buildAnyClouds = buildSlowClouds;
        if (buildSlowClouds) {
            rebuildFrequency = 20*1;
            rebuildFrequency = 20 * 10;
            rebuildFrequency = 20 * 5;
            //rebuildFrequency = 40;
            lastBuildTime = Minecraft.getInstance().level.getGameTime() + rebuildFrequency;
        }

        if (!buildAnyClouds) {
            return false;
        }

        Vec3 vec3 = new Vec3(0, 0, 0);

        this.setSyncState(SyncState.OFFTHREADBUILDINGVBO);

        //algo cloud layer
        if (buildSlowClouds) {
            for (SkyChunk skyChunk : WatutMod.cloudRenderHandler.getListOfSkyChunksForAlgoClouds()) {
                if (skyChunk.isWaitingToUploadData()) continue;

                generateAlgoCloudv2(skyChunk.getX(), skyChunk.getZ(), 60 / scale, getCloudsY() / scale);
            }
        }

        //SkyChunkManager.instance().addPoint(false, 0, 100, 0);

        //TODO: entire rework needed here, needs to be instanced ala weather2 weather objects
        /*for (int ii = 0; ii < cloudCount; ii++) {
            RenderableData renderableData = null;

        }*/

        //test lenticular cloud
        this.performPrecalc();
        this.buildCloud(50 / scale, 130 / scale, 110 / scale, scale);


        //for (Iterator<Map.Entry<Long, SkyChunk>> it = getQueueUpdateSkyChunks().entrySet().iterator(); it.hasNext(); ) {
        for (Iterator<Map.Entry<Long, SkyChunk>> it = SkyChunkManager.instance().getSkyChunks().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, SkyChunk> entry = it.next();
            SkyChunk skyChunk = entry.getValue();

            //until we find a nice way to clean up chunks that were unused, just flag everything for new data which will do this for us
            skyChunk.markDirty();

            if (skyChunk.isWaitingToUploadData() || !skyChunk.isDirty()) continue;

            Vec3 vecCam = Minecraft.getInstance().cameraEntity.position();
            this.setCamVec(vecCam);

            skyChunk.setCameraPosDuringBuild(new Vec3(vecCam.x, vecCam.y, vecCam.z));

            skyChunk.populateToBeRemovedPoints();

            calculateNormalizedDistanceToOutside(skyChunk);
            skyChunk.getRenderableData().setVbo(pointsToVBO(WatutMod.threadedBufferBuilder, skyChunk, skyChunk.getLookupPointsOffThreadAlreadyExisting(), vec3, scale));
            skyChunk.getRenderableData().setVboAddedPoints(pointsToVBO(WatutMod.threadedBufferBuilder, skyChunk, skyChunk.getLookupPointsOffThreadBeingAdded(), vec3, scale));
            skyChunk.getRenderableData().setVboRemovedPoints(pointsToVBO(WatutMod.threadedBufferBuilder, skyChunk, skyChunk.getLookupPointsOffThreadBeingRemoved(), vec3, scale));

            //CULog.log("skyChunk.getPointsOffThreadPrevUpdate() " + skyChunk.getPointsOffThreadPrevUpdate().size());
                /*CULog.log("skyChunk.getLookupPointsOffThreadA() " + skyChunk.getLookupPointsOffThreadA().size());
                CULog.log("skyChunk.getLookupPointsOffThreadB() " + skyChunk.getLookupPointsOffThreadB().size());
                CULog.log("skyChunk.getLookupPointsOffThreadAlreadyExisting() " + skyChunk.getLookupPointsOffThreadAlreadyExisting().size());
                CULog.log("skyChunk.getLookupPointsOffThreadBeingAdded() " + skyChunk.getLookupPointsOffThreadBeingAdded().size());
                CULog.log("skyChunk.getLookupPointsOffThreadBeingRemoved() " + skyChunk.getLookupPointsOffThreadBeingRemoved().size());*/

            skyChunk.setLastBuildTime(getTicksVolatile());

            //add to upload queue
            skyChunk.setWaitingToUploadData(true);
            //skyChunk.setNeedsReinit(true);
            skyChunk.markDirty(false);
            getQueueWaitingForUploadSkyChunks().put(entry.getKey(), skyChunk);
        }

        setSyncState(ThreadedCloudBuilder.SyncState.IDLE);

        return true;
    }

    private ThreadedBufferBuilder.RenderedBuffer pointsToVBO(ThreadedBufferBuilder bufferIn, SkyChunk skyChunk, HashMap<Long, SkyChunk.SkyChunkPoint> skyChunkPoints, Vec3 cloudsColor, float scale) {

        bufferIn.begin(VertexFormat.Mode.QUADS, WatutMod.POSITION_TEX_COLOR_NORMAL_VEC3);

        for (SkyChunk.SkyChunkPoint entry : skyChunkPoints.values()) {
            List<Direction> listRenderables = entry.getRenderableSides(skyChunkPoints);

            renderCloudCube(bufferIn, cloudsColor,
                    new Vector3f((skyChunk.getX() * SkyChunk.size) + entry.getX(), (skyChunk.getY() * SkyChunk.size) + entry.getY(), (skyChunk.getZ() * SkyChunk.size) + entry.getZ())
                    , scale, listRenderables, entry);
        }

        return bufferIn.end();
    }

    //TODO: this will have more issues since skychunk changed to 32 size
    //TODO: couldnt we redesign this so that the lookups are populated as were adding points to the skychunk?
    private void calculateNormalizedDistanceToOutside(SkyChunk skyChunk) {

        //move existing points to new and old points as if theres a change
        for (Iterator<Map.Entry<Long, SkyChunk.SkyChunkPoint>> it = skyChunk.getLookupPointsOffThreadAlreadyExisting().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, SkyChunk.SkyChunkPoint> entry = it.next();
            SkyChunk.SkyChunkPoint skyChunkPointNew = entry.getValue();
            long hash = entry.getKey();

            float dist = skyChunkPointNew.calculateNormalizedDistanceToOutside();
            skyChunkPointNew.setNormalizedDistanceToOutside(dist);

            SkyChunk.SkyChunkPoint skyChunkPointOld = skyChunk.getPointsOffThreadPrevUpdate().get(hash);
            if (skyChunkPointOld != null) {
                float oldDist = skyChunkPointOld.getNormalizedDistanceToOutside();
                if (oldDist != dist) {
                    //move to a transition data set
                    skyChunk.getLookupPointsOffThreadBeingAdded().put(hash, skyChunkPointNew);
                    skyChunk.getLookupPointsOffThreadBeingRemoved().put(hash, skyChunkPointOld);

                    //remove from existing data set
                    it.remove();
                }
            }
        }

        //calculate for new points
        for (Iterator<Map.Entry<Long, SkyChunk.SkyChunkPoint>> it = skyChunk.getLookupPointsOffThreadBeingAdded().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, SkyChunk.SkyChunkPoint> entry = it.next();
            SkyChunk.SkyChunkPoint skyChunkPointNew = entry.getValue();

            if (skyChunkPointNew.getNormalizedDistanceToOutside() == 1F) {
                float dist = skyChunkPointNew.calculateNormalizedDistanceToOutside();
                skyChunkPointNew.setNormalizedDistanceToOutside(dist);
            }
        }
    }

    private ClientLevel getLevelVolatile() {
        return Minecraft.getInstance().level;
    }

    private int getTicksVolatile() {
        return (int) getLevelVolatile().getGameTime();
    }

    private void generateAlgoCloudv2(int skyChunkPosX, int skyChunkPosZ, int sizeY, int posY) {

        int worldPosX = skyChunkPosX * SkyChunk.size;
        int worldPosZ = skyChunkPosZ * SkyChunk.size;

        int size = 800;
        size = SkyChunk.size;
        //size = 200;
        //Cloud cloudLarge = new Cloud(size, sizeY, size);

        PerlinNoise perlinNoise = PerlinNoiseHelper.get().getPerlinNoise();
        if (Minecraft.getInstance().level == null) return;

        //TODO: use time value set at start of job, not this
        long time = (long) (Minecraft.getInstance().level.getGameTime() * 0.1F);
        //time = (long) (Minecraft.getInstance().level.getGameTime() * 0.2F);
        time = (long) (Minecraft.getInstance().level.getGameTime() * 0.05F * 0.2F);
        //time = (long) (Minecraft.getInstance().level.getGameTime() * 0.05F * 2F);
        //System.out.println(time);
        //time = 202985;
        //time = 0;

        //BlockPos skyChunkWorldPos = skyChunk.getWorldPos();
        Vec3 vec = WatutMod.cloudRenderHandler.getPosCloudOffset();
        //skyChunkWorldPos = skyChunkWorldPos.subtract(new BlockPos(Mth.floor(vec.x / SkyChunk.size), Mth.floor(vec.y / SkyChunk.size), Mth.floor(vec.z / SkyChunk.size)));
        //skyChunkWorldPos = skyChunkWorldPos.offset(new BlockPos(Mth.floor(vec.x), Mth.floor(vec.y), Mth.floor(vec.z)));

        for (int x = 0; x < SkyChunk.size; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < SkyChunk.size; z++) {

                    int indexX = worldPosX + x;
                    int indexY = posY + y;
                    int indexZ = worldPosZ + z;

                    float distFromCenterY = Vector3f.distance(0, sizeY/2, 0, 0, y, 0);
                    float vecY = distFromCenterY / (sizeY);

                    //double scaleP = 10;
                    double scaleP = 2 * scale;
                    //double scaleP = 10 / (scale / 2);
                    double noiseVal = perlinNoise.getValue(((indexX) * scaleP) + time, ((indexY) * scaleP) + time,((indexZ) * scaleP) + time)/* + 0.2F*/;

                    float noiseThreshAdj = (float) (vecY + (Math.sin(time * 0.2F) * 0.4F) + 0.1F);
                    if (Math.abs(noiseVal) > noiseThreshAdj) {
                        //skyChunk.addPoint(false, x, y + offsetY, z);
                        SkyChunkManager.instance().addPoint(false, indexX, indexY, indexZ);
                    }

                }
            }
        }
    }

    //for huge overcast area
    private void generateAlgoCloud(SkyChunk skyChunk, int sizeY, int offsetY) {

        int size = 800;
        size = SkyChunk.size;
        //size = 200;
        Cloud cloudLarge = new Cloud(size, sizeY, size);

        PerlinNoise perlinNoise = PerlinNoiseHelper.get().getPerlinNoise();
        if (Minecraft.getInstance().level == null) return;

        //TODO: use time value set at start of job, not this
        long time = (long) (Minecraft.getInstance().level.getGameTime() * 0.1F);
        //time = (long) (Minecraft.getInstance().level.getGameTime() * 0.2F);
        time = (long) (Minecraft.getInstance().level.getGameTime() * 0.05F * 0.2F);
        //time = (long) (Minecraft.getInstance().level.getGameTime() * 0.05F * 2F);
        //System.out.println(time);
        //time = 202985;
        //time = 0;

        BlockPos skyChunkWorldPos = skyChunk.getWorldPos();
        Vec3 vec = WatutMod.cloudRenderHandler.getPosCloudOffset();
        //skyChunkWorldPos = skyChunkWorldPos.subtract(new BlockPos(Mth.floor(vec.x / SkyChunk.size), Mth.floor(vec.y / SkyChunk.size), Mth.floor(vec.z / SkyChunk.size)));
        //skyChunkWorldPos = skyChunkWorldPos.offset(new BlockPos(Mth.floor(vec.x), Mth.floor(vec.y), Mth.floor(vec.z)));

        for (int x = 0; x < cloudLarge.getSizeX(); x++) {
            for (int y = 0; y < cloudLarge.getSizeY(); y++) {
                for (int z = 0; z < cloudLarge.getSizeZ(); z++) {

                    int indexX = skyChunkWorldPos.getX() + x;
                    int indexY = skyChunkWorldPos.getY() + y + offsetY;
                    int indexZ = skyChunkWorldPos.getZ() + z;

                    double scaleP = 10;
                    double noiseVal = perlinNoise.getValue(((indexX) * scaleP) + time, ((indexY) * scaleP) + time,((indexZ) * scaleP) + time)/* + 0.2F*/;

                    float noiseThreshAdj = (float) (0.2F + (Math.sin(time * 0.1F) * 0.1F));
                    if (Math.abs(noiseVal) > 0.0 + noiseThreshAdj) {
                        skyChunk.addPoint(false, x, y + offsetY, z);
                        //SkyChunkManager.instance().addPoint(false, indexX, indexY, indexZ);
                    }

                }
            }
        }
    }

    private void performPrecalc() {

        if (cloudShapeNeedsPrecalc) {
            CULog.log("performing one time cloud shape precalc");
            cloudShapeNeedsPrecalc = false;

            cloudShape = new Cloud(sizeX, sizeY, sizeZ);

            for (int x = 0; x < cloudShape.getSizeX(); x++) {
                for (int y = 0; y < cloudShape.getSizeY(); y++) {
                    for (int z = 0; z < cloudShape.getSizeZ(); z++) {

                        /*if ((x == cloudShape.getSizeX() / 2 && y == cloudShape.getSizeY() / 4 * 3 && z > 3 && z < cloudShape.getSizeZ() - 3)
                                || (x == cloudShape.getSizeX() / 2 && z == cloudShape.getSizeZ() / 2 && y > 3 && y < cloudShape.getSizeY() - 3)) {
                            //forceShapeAdj = 1;
                            float noiseThreshAdj = 1;
                            cloudShape.addPoint(x, y, z, noiseThreshAdj);

                        }*/

                        float xzRatio = Mth.clamp(1F - ((float)y / (float)(cloudShape.getSizeY() - 30)), 0.3F, 0.7F);

                        if ((x > (xzRatio * cloudShape.getSizeX()) && x < cloudShape.getSizeX() - (xzRatio * cloudShape.getSizeX()))
                                && (z > (xzRatio * cloudShape.getSizeZ()) && z < cloudShape.getSizeZ() - (xzRatio * cloudShape.getSizeZ()))) {

                            float noiseThreshAdj = 1;
                            cloudShape.addPoint(x, y, z, noiseThreshAdj);
                        }

                        /*if ((x == cloudShape.getSizeX() / 2 && y == cloudShape.getSizeY() / 4 * 3 && z > 3 && z < cloudShape.getSizeZ() - 3)
                                || (x == cloudShape.getSizeX() / 2 && z == cloudShape.getSizeZ() / 2 && y > 3 && y < cloudShape.getSizeY() - 3)) {
                            //forceShapeAdj = 1;
                            float noiseThreshAdj = 1;
                            cloudShape.addPoint(x, y, z, noiseThreshAdj);

                        }*/

                        /*float noiseThreshAdj = 1;
                        cloudShape.addPoint(x, y, z, noiseThreshAdj);*/

                    }
                }
            }

            cloudShape2 = new Cloud(sizeX, sizeY, sizeZ);

            for (int x = 0; x < cloudShape2.getSizeX(); x++) {
                for (int y = 0; y < cloudShape2.getSizeY(); y++) {
                    for (int z = 0; z < cloudShape2.getSizeZ(); z++) {

                        if ((x == cloudShape2.getSizeX() / 2 && y == cloudShape2.getSizeY() / 4 * 1 && z > 3 && z < cloudShape2.getSizeZ() - 3)
                                || (z == cloudShape2.getSizeZ() / 2 && y == cloudShape2.getSizeY() / 4 * 1 && x > 3 && x < cloudShape2.getSizeX() - 3)) {
                            //forceShapeAdj = 1;
                            float noiseThreshAdj = 1;
                            cloudShape2.addPoint(x, y, z, noiseThreshAdj);

                        }

                        /*if ((x == 0 || x == cloudShape2.getSizeX() - 1) && (z == 0 || z == cloudShape2.getSizeZ() - 1)) {
                            //forceShapeAdj = 1;
                            float noiseThreshAdj = 1;
                            cloudShape2.addPoint(x, y, z, noiseThreshAdj);
                        }*/

                        /*float noiseThreshAdj = 1;
                        cloudShape2.addPoint(x, y, z, noiseThreshAdj);*/

                    }
                }
            }

            //how far from a point in the shape to have no threshold adjustment at all
            int maxDistToZeroAdjust = 6;

            buildShapeThresholds(cloudShape, maxDistToZeroAdjust);
            buildShapeThresholds(cloudShape2, maxDistToZeroAdjust);

            CULog.log("finished one time cloud shape precalc");
        }

    }

    private void buildShapeThresholds(Cloud cloudShape, int maxDistToZeroAdjust) {
        //TODO: should only ever run once (in dev lifetime), as it should be stored/baked to data for use at runtime
        for (int x = 0; x < cloudShape.getSizeX(); x++) {
            for (int y = 0; y < cloudShape.getSizeY(); y++) {
                for (int z = 0; z < cloudShape.getSizeZ(); z++) {

                    float maxDist = 99999;

                    for (int xt = -maxDistToZeroAdjust; xt <= maxDistToZeroAdjust; xt++) {
                        for (int yt = -maxDistToZeroAdjust; yt <= maxDistToZeroAdjust; yt++) {
                            for (int zt = -maxDistToZeroAdjust; zt <= maxDistToZeroAdjust; zt++) {

                                int checkX = x + xt;
                                int checkY = y + yt;
                                int checkZ = z + zt;
                                if (xt > 0) {
                                    int what = 0;
                                }
                                //CULog.dbg(checkX + " - " + checkY + " - " + checkZ);
                                //System.out.println();
                                if (checkX < 0 || checkX >= cloudShape.getSizeX()) continue;
                                if (checkY < 0 || checkY >= cloudShape.getSizeY()) continue;
                                if (checkZ < 0 || checkZ >= cloudShape.getSizeZ()) continue;

                                Cloud.CloudPoint cloudPointAvoidChanging = cloudShape.getPoint(x, y, z);
                                if (cloudPointAvoidChanging == null || cloudPointAvoidChanging.getShapeAdjustThreshold() != 1) {
                                    Cloud.CloudPoint cloudPointCheck = cloudShape.getPoint(checkX, checkY, checkZ);

                                    //TODO: for now assuming threshold of 1 = pure shape point, not a distance adjusted one, maybe change this behavior, flag them correctly
                                    if (cloudPointCheck != null && cloudPointCheck.getShapeAdjustThreshold() == 1) {

                                        float dist = Vector3f.distance(x, y, z, checkX, checkY, checkZ);

                                        if (dist <= maxDist && dist <= maxDistToZeroAdjust) {
                                            maxDist = dist;
                                            float distFraction = Mth.clamp(1 - (dist / maxDistToZeroAdjust), 0, 0.99F);
                                            //distFraction = 0.5F;
                                            //distFraction = rand.nextFloat() * 0.5F;
                                            //Cloud.CloudPoint cloudPoint = cloudShape.getPoint(checkX, checkY, checkZ);
                                            cloudShape.addPoint(x, y, z, distFraction);
                                        }

                                    }
                                }

                            }
                        }
                    }

                }
            }
        }
    }

    private void buildCloud(double cloudsX, double cloudsY, double cloudsZ, float scale) {
        Vector3f cubePos = new Vector3f(Mth.floor(cloudsX), Mth.floor(cloudsY), Mth.floor(cloudsZ));

        //TODO: skychunk relocate me
        //renderableCloud.setLightningPos(vec);

        Cloud cloud = new Cloud(sizeX, sizeY, sizeZ);
        cloud.setCloudShape(cloudShape);

        PerlinNoise perlinNoise = PerlinNoiseHelper.get().getPerlinNoise();
        if (Minecraft.getInstance().level == null) return;
        //TODO: use time value set at start of job, not this
        long time = (long) (Minecraft.getInstance().level.getGameTime() * 0.1F);
        //time = (long) (Minecraft.getInstance().level.getGameTime() * 1F);
        time = (long) (Minecraft.getInstance().level.getGameTime() * 0.05F * 0.05F);
        //time = (long) (Minecraft.getInstance().level.getGameTime() * 0.8F);
        //time += (cloudIndex * 25);
        //time = 0;

        float timeShortAdj1 = (time) * 1F;
        float timeShortAdj2 = (time) * 1F;
        float timeShortAdj3 = (time) * 5F;

        Vec3 vec = WatutMod.cloudRenderHandler.getPosCloudOffset();

        for (int x = 0; x < cloud.getSizeX(); x++) {
            for (int y = 0; y < cloud.getSizeY(); y++) {
                for (int z = 0; z < cloud.getSizeZ(); z++) {
                    float distFromCenterXZ = Vector3f.distance(cloud.getSizeX()/2, 0, cloud.getSizeZ()/2, x, 0, z);
                    float distFromCenterY = Vector3f.distance(0, cloud.getSizeY()/2, 0, 0, y, 0);
                    float vecXZ = distFromCenterXZ / (cloud.getSizeX() - 3);
                    float vecY = distFromCenterY / (cloud.getSizeY() - 3);
                    //require more strict threshold as it gets further from center of cloud
                    //float noiseThreshAdj = (vecXZ + vecY) / 2F * 0.9F;
                    float noiseThreshAdj = (vecXZ + vecY) / 2F * 1.8F;
                    //float noiseThreshAdj = (vecXZ + vecY) / 2F * 1.2F;
                    //float noiseThreshAdj = 0.5F;//(vecXZ + vecY) / 2F * 1.2F;
                    //float noiseThreshAdj = (vecXZ + vecY) / 2F * 0.8F;
                    int indexX = (int) Math.floor(x + cubePos.x - (vec.x / scale));
                    int indexY = (int) Math.floor(y + cubePos.y - (vec.y / scale));
                    int indexZ = (int) Math.floor(z + cubePos.z - (vec.z / scale));

                    //TEMP
                    //noiseThreshAdj = 0.8F;
                    //noiseThreshAdj = 0.3F;

                    float uh = (float) ((Math.sin(timeShortAdj3 * 0.005F) + 1)) * 0.1F;
                    float uh2 = (float) ((-Math.sin(timeShortAdj3 * 0.005F)));

                    //double forceShapeAdj = 0;
                    //forced shape testing, a cross shape ( t )
                    float shapeAdjThreshold = 0F;
                    float more = 0.8F;
                    Cloud.CloudPoint cloudPoint = cloudShape.getPoint(x, y, z);
                    if (cloudPoint != null) {
                        //noiseThreshAdj -= (Math.sin(timeShortAdj2 * 0.01F) * 0.5F * cloudPoint.getShapeAdjustThreshold());
                        shapeAdjThreshold -= cloudPoint.getShapeAdjustThreshold() * uh * more;
                        //noiseThreshAdj = 0;
                    }
                    Cloud.CloudPoint cloudPoint2 = cloudShape2.getPoint(x, y, z);
                    if (cloudPoint2 != null) {
                        //noiseThreshAdj -= (Math.sin(timeShortAdj2 * 0.01F) * 0.5F * cloudPoint.getShapeAdjustThreshold());
                        shapeAdjThreshold -= cloudPoint2.getShapeAdjustThreshold() * uh2 * more;
                        //noiseThreshAdj = 0;
                    }
                    noiseThreshAdj += shapeAdjThreshold;
                    if ((x == cloud.getSizeX() / 2 && y == cloud.getSizeY() / 4 * 3)
                            || (x == cloud.getSizeX() / 2 && z == cloud.getSizeZ() / 2)) {
                        //forceShapeAdj = 1;
                        //noiseThreshAdj -= (0.4 * Math.sin(timeShortAdj2 * 0.01F) * 1.2F);
                    }

                    float mixedThreshold = shapeAdjThreshold;

                    //noiseThreshAdj -= 0.2F;

                    //noiseThreshAdj += Math.sin(timeShortAdj1 * 0.015F) * 0.15F;
                    noiseThreshAdj += Math.sin(timeShortAdj1 * 0.015F) * 0.22F;
                    //noiseThreshAdj += Math.sin(timeShortAdj1 * 0.015F) * 0.55F;

                    double scaleP = 10;
                    double noiseVal = perlinNoise.getValue(((indexX) * scaleP) + time, ((indexY) * scaleP) + time,((indexZ) * scaleP) + time)/* + 0.2F*/;
                    //noiseVal = 0.2F;
                    if (Math.abs(noiseVal) > 0.0 + noiseThreshAdj) {
                        //SkyChunkManager.instance().
                        //cloud.addPoint(x, y, z);
                        //if (!testOnce) {
                            //testOnce = true;
                            SkyChunkManager.instance().addPoint(false, indexX, indexY, indexZ);

                            //SkyChunkManager.instance().addPoint(false, 0, getCloudsY(), 0);
                        //}
                    }
                }
            }
        }
    }

    private void renderCloudCube(ThreadedBufferBuilder bufferIn, Vec3 cloudsColor, Vector3f cubePos, float scale, List<Direction> directions, SkyChunk.SkyChunkPoint cloudPoint) {
        pointCount++;
        Quaternionf q2 = new Quaternionf(0, 0, 0, 1);
        Random rand2 = new Random((long) (cubePos.x + cubePos.z));

        boolean randRotate = false;
        Vec3 cloudColor = Minecraft.getInstance().level.getCloudColor(1F);

        float particleAlpha = 0.1F;
        //particleAlpha = (float) Math.random();

        float threshold = 0;
        //TODO: BAD CODE NOW since skychunk change, redesign/relocate
        Cloud.CloudPoint cloudShapePoint = cloudShape2.getPoint(cloudPoint.getX(), cloudPoint.getY(), cloudPoint.getZ());
        if (cloudShapePoint != null) {
            threshold = cloudShapePoint.getShapeAdjustThreshold();
        } else {
            threshold = rand2.nextFloat();
            threshold = rand2.nextFloat();
            threshold = (rand2.nextFloat() * 0.5F);
            threshold = 0.5F;

        }

        for (Direction dir : directions) {
            Quaternionf quaternion = dir.getRotation();

            Vector3f[] avector3f3 = new Vector3f[]{
                    new Vector3f(-1.0F, 0.0F, -1.0F),
                    new Vector3f(-1.0F, 0.0F, 1.0F),
                    new Vector3f(1.0F, 0.0F, 1.0F),
                    new Vector3f(1.0F, 0.0F, -1.0F)};

            Vector3f[] avector3f32 = new Vector3f[]{
                    new Vector3f(-1.0F, 0.0F, -1.0F),
                    new Vector3f(-1.0F, 0.0F, 1.0F),
                    new Vector3f(1.0F, 0.0F, 1.0F),
                    new Vector3f(1.0F, 0.0F, -1.0F)};

            Vector3f normal = new Vector3f(dir.getNormal().getX(), dir.getNormal().getY(), dir.getNormal().getZ());
            //playing around with idea of other sides being brighter too
            if (normal.x == -1) normal.x = 1;
            if (normal.z == -1) normal.z = 1;
            //Vector3f normal = new Vector3f(0F, dir.getNormal().getY(), 0F);
            //normal = new Vector3f(0, 0, 0);
            if (randRotate) normal.rotate(q2);
            float normalRange = 0.1F;
            //normal.mul(normalRange);
            //normal.add(1-normalRange, 1-normalRange, 1-normalRange);
            //float uh = 0.55F;
            //normal.add(uh, uh, uh);

            //normal = new Vector3f((float) Math.random(), (float) Math.random(), (float) Math.random());
            //normal = new Vector3f(1, 0, 0);

            for(int i = 0; i < 4; ++i) {
                Vector3f vector3f = avector3f3[i];
                Vector3f vector3f2 = avector3f32[i];
                vector3f.rotate(quaternion);
                vector3f2.rotate(quaternion);
                vector3f.add(1F, 1F, 1F);
                vector3f.add((float) dir.getStepX(), (float) dir.getStepY(), (float) dir.getStepZ());
                if (randRotate) vector3f.rotate(q2);
                //vector3f.mul(scale * 2);
                vector3f.mul(scale / 2F);
                //vector3f.add((float) cloudsX + 0.5F, (float) cloudsY, (float) cloudsZ + 0.5F);
                //vector3f.add((float) cloudsX + 0.0F, (float) cloudsY, (float) cloudsZ + 0.0F);
                //vector3f.add((float) 0 + 0.5F, (float) cloudsY, (float) 0 + 0.5F);
                //vector3f.add((float) cubePos.x, (float) cubePos.y, (float) cubePos.z);
                //vector3f.add((float) cubePos.x, (float) cubePos.y, (float) cubePos.z);
                vector3f.add((float) cubePos.x * scale, (float) cubePos.y * scale, (float) cubePos.z * scale);

                //position relative to camera
                vector3f.add((float) -camVec.x(), (float) -camVec.y(), (float) -camVec.z());
            }

            //TextureAtlasSprite sprite = ParticleRegistry.cloud_square;
            TextureAtlasSprite sprite = ParticleRegistry.cloud_square.getSprite();

            float f7 = sprite.getU0();
            float f8 = sprite.getU1();
            float f5 = sprite.getV0();
            float f6 = sprite.getV1();

            /*f7 = (float) Math.random();
            f8 = (float) Math.random();
            f5 = (float) Math.random();
            f6 = (float) Math.random();*/

            /*f7 = 0;
            f8 = 1;
            f5 = 0;
            f6 = 1;*/


            float particleRed = (float) cloudsColor.x;
            float particleGreen = (float) cloudsColor.y;
            float particleBlue = (float) cloudsColor.z;

            particleRed = 1F;
            //particleRed = (float) (0.85F + (rand2.nextFloat() * 0.08F));
            /*particleGreen = (float) Math.random();
            particleBlue = (float) Math.random();*/
            particleGreen = threshold;
            //particleBlue = (float) particleRed - threshold;
            particleBlue = 0;

            /*particleGreen = particleRed;
            particleBlue = particleRed;*/

            threshold = 0.7F + (threshold * 0.2F);

            threshold = 0.93F;
            threshold = 0.83F;
            threshold = 0.9F;
            //threshold = 1F;
            //threshold = 0.8F;
            //threshold = 0.2F;

            float colorShift = 0.82F;
            colorShift = 0.99F;
            colorShift = 0.89F;
            /*particleRed = threshold * colorShift;
            particleGreen = threshold * colorShift;
            particleBlue = threshold;*/

            /*particleRed = threshold * (0.5F);
            particleGreen = threshold * (0.65F);
            particleBlue = threshold;*/

            particleRed = threshold * (0.75F);
            particleGreen = threshold * (0.825F);
            particleBlue = threshold;

            particleRed = (float) cloudColor.x;
            particleGreen = (float) cloudColor.y;
            particleBlue = (float) cloudColor.z;

            //TODO: done in shader now
            float impact1 = 0.1F;
            //float impact1 = 0.0F;
            float heightFract = (1F - impact1) + ((cubePos.y / (float)(sizeY)) * impact1);
            //float heightFract = 1F;//1.5F + ((cubePos.y / (float)(sizeY)) * 0.5F);
            particleRed *= heightFract;
            particleGreen *= heightFract;
            particleBlue *= heightFract;

            //float impact2 = 0.1F;
            //float impact2 = 0.15F;
            //float impact2 = 0.0F;
            //moved to shader, keep this 1
            //float impact2 = 0.15F;
            float impact2 = 1F;
            float distToOutsideAdj = (1F - impact2) + cloudPoint.getNormalizedDistanceToOutside() * impact2;//Mth.clamp(1F - cloudPoint.getNormalizedDistanceToOutside(), 0, 1);
            //distToOutsideAdj = cloudPoint.getNormalizedDistanceToOutside();
            if (distToOutsideAdj == -1F) {
                //System.out.println("???????");
            }

            /*particleRed *= distToOutsideAdj;
            particleGreen *= distToOutsideAdj;
            particleBlue *= distToOutsideAdj;*/

            particleRed = Math.min(1F, particleRed);
            particleGreen = Math.min(1F, particleGreen);
            particleBlue = Math.min(1F, particleBlue);

            float maxDistFromCloudClass = 4F;
            float distToOutsideHalfBlockAdj = (0.5F / maxDistFromCloudClass) * impact2;
            //if (distToOutsideAdj != 0) {
                avector3f32[0].y *= distToOutsideHalfBlockAdj;
                avector3f32[1].y *= distToOutsideHalfBlockAdj;
                avector3f32[2].y *= distToOutsideHalfBlockAdj;
                avector3f32[3].y *= distToOutsideHalfBlockAdj;
            //}

            particleRed = 1;
            particleGreen = 1;
            particleBlue = 1;
            particleAlpha = 1;

            /*particleRed = 0;
            particleGreen = 0;
            particleBlue = 0;
            particleAlpha = 1;*/

            bufferIn.vertex(avector3f3[0].x(), avector3f3[0].y(), avector3f3[0].z()).uv(f8, f6).color(particleRed, particleGreen, particleBlue, particleAlpha).normal(normal.x(), normal.y(), normal.z())
                    .vertex(distToOutsideAdj, avector3f32[0].y(), distToOutsideHalfBlockAdj).endVertex();
            bufferIn.vertex(avector3f3[1].x(), avector3f3[1].y(), avector3f3[1].z()).uv(f8, f5).color(particleRed, particleGreen, particleBlue, particleAlpha).normal(normal.x(), normal.y(), normal.z())
                    .vertex(distToOutsideAdj, avector3f32[1].y(), distToOutsideHalfBlockAdj).endVertex();
            bufferIn.vertex(avector3f3[2].x(), avector3f3[2].y(), avector3f3[2].z()).uv(f7, f5).color(particleRed, particleGreen, particleBlue, particleAlpha).normal(normal.x(), normal.y(), normal.z())
                    .vertex(distToOutsideAdj, avector3f32[2].y(), distToOutsideHalfBlockAdj).endVertex();
            bufferIn.vertex(avector3f3[3].x(), avector3f3[3].y(), avector3f3[3].z()).uv(f7, f6).color(particleRed, particleGreen, particleBlue, particleAlpha).normal(normal.x(), normal.y(), normal.z())
                    .vertex(distToOutsideAdj, avector3f32[3].y(), distToOutsideHalfBlockAdj).endVertex();
            quadCount++;

        }

    }
}
