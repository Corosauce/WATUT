package com.corosus.watut;

import com.corosus.watut.network.ToClientPlayerStatusMessage;
import com.corosus.watut.network.ToServerPlayerStatusMessage;
import com.corosus.watut.network.WATUTNetwork;
import com.corosus.watut.particles.HeartParticle2;
import com.corosus.watut.particles.StatusParticle;
import com.corosus.watut.tornado.CubicBezierCurve;
import com.corosus.watut.tornado.TornadoFunnel;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.java.games.input.Keyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.particle.TexturedParticle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayerManagerClient extends PlayerManager {

    private Class lastScreenClass = null;

    private HeartParticle2 particleTest = null;
    private List<HeartParticle2> particles = new ArrayList<>();

    private TornadoFunnel funnel;

    public CubicBezierCurve bezierCurve;

    public Vector3f[] vecSpeeds = new Vector3f[4];

    public void tick(World world) {
        super.tick(world);
        Minecraft mc = Minecraft.getInstance();
        if (mc.world == null || mc.player == null) return;
        //List<AbstractClientPlayerEntity> listClosePlayers = mc.world.getPlayers().stream().filter((player) -> mc.player.getDistance(player) < 24).collect(Collectors.toList());
        //listClosePlayers.stream().forEach();
        Class curScreenClass = null;
        if (mc.currentScreen != null) {
            curScreenClass = mc.currentScreen.getClass();
        }
        if (curScreenClass != lastScreenClass) {
            if (mc.currentScreen instanceof ChatScreen) {
                triggerScreenChange(PlayerStatus.StatusType.CHAT);
                lastScreenClass = mc.currentScreen.getClass();
            } else if (curScreenClass == null) {
                triggerScreenChange(PlayerStatus.StatusType.NONE);
                lastScreenClass = null;
            }
        }

        if (mc.world.getGameTime() % 1 == 0) {
            for (PlayerEntity playerEntity : mc.world.getPlayers()) {
                if (true || mc.player.getDistance(playerEntity) < 20) {
                    if (WATUT.playerManagerClient.getPlayerStatus(mc.player.getUniqueID()).getStatusType() == PlayerStatus.StatusType.CHAT) {
                        /*StatusParticle particle = new StatusParticle(mc.world, playerEntity.getPosX(), playerEntity.getPosY() + 2.2, playerEntity.getPosZ());
                        particle.setSprite(EventHandlerForge.square16);
                        particle.setMaxAge(5);
                        particle.setSize(0.5F, 0.5F);
                        particle.setScale(0.2F);*/
                        //HeartParticle2 particle = new HeartParticle2(mc.world, playerEntity.getPosX(), playerEntity.getPosY() + 2.2, playerEntity.getPosZ());
                        //particle.setSprite(EventHandlerForge.square16);
                        //particle.setMaxAge(5);
                        //particle.setSize(0.5F, 0.5F);
                        //particle.setScale(0.2F);
                        //mc.particles.addEffect(particle);
                    }

                    int particleCountCircle = 20;
                    int particleCountLayers = 40;

                    while (particles.size() < particleCountCircle * particleCountLayers) {
                        particleTest = new HeartParticle2(mc.world, playerEntity.getPosX(), playerEntity.getPosY() + 2.2, playerEntity.getPosZ());
                        particleTest.setSprite(EventHandlerForge.square16);
                        particleTest.setMaxAge(250);
                        particleTest.setMotion(0, 0, 0);
                        particleTest.setScale(0.1F);
                        //particleTest.setColor(0.1F * (particles.size() % particleCountCircle), 0, 0);
                        particleTest.setColor(world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat());
                        if (particles.size() < particleCountCircle * 5) {
                            particleTest.setColor(1, 1, 1);
                        }
                        //particleTest.move(0, -0.1, 0);
                        mc.particles.addEffect(particleTest);
                        particles.add(particleTest);
                    }

                    int testY = 100;

                    Vector3f pos1 = new Vector3f(0.5F, 150, 0.5F);
                    Vector3f pos2 = new Vector3f(0.5F, 100, 0.5F);

                    /*float dist = (float)Math.sqrt(playerEntity.getDistanceSq(0.5, testY, 0.5));
                    Vector3f vecDiff = new Vector3f(
                            (float)(playerEntity.getPosX() - 0.5) / dist,
                            (float)(playerEntity.getPosY() - testY) / dist,
                            (float)(playerEntity.getPosZ() - 0.5) / dist);
                    Vector3f vecAngles = new Vector3f(
                            (float)Math.atan2(vecDiff.getY(), vecDiff.getZ()),
                            (float)Math.atan2(vecDiff.getZ(), vecDiff.getX()), //invert if needed
                            (float)Math.atan2(vecDiff.getX(), vecDiff.getY())); //invert if needed*/

                    float dist = getDistance(pos1, pos2);
                    Vector3f vecDiff = new Vector3f(
                            (pos1.getX() - pos2.getX()) / dist,
                            (pos1.getY() - pos2.getY()) / dist,
                            (pos1.getZ() - pos2.getZ()) / dist);
                    Vector3f vecAngles = new Vector3f(
                            (float)Math.atan2(vecDiff.getY(), vecDiff.getZ()),
                            (float)Math.atan2(vecDiff.getZ(), vecDiff.getX()), //invert if needed
                            (float)Math.atan2(vecDiff.getX(), vecDiff.getY())); //invert if needed

                    //convert to degrees
                    vecAngles = new Vector3f((float)Math.toDegrees(vecAngles.getX()), (float)Math.toDegrees(vecAngles.getY()), (float)Math.toDegrees(vecAngles.getZ()));

                    double xx = pos1.getX() - pos2.getX();
                    double zz = pos1.getZ() - pos2.getZ();
                    double xzDist = Math.sqrt(xx * xx + zz * zz);
                    float pitchAngle = (float)Math.toDegrees(Math.atan2(vecDiff.getY(), xzDist / dist));

                    pitchAngle += 90;

                    if (bezierCurve == null || /*world.getGameTime() % 200 == 0*/playerEntity.isSprinting()) {
                        Vector3f[] vecs = new Vector3f[4];
                        for (int i = 0; i < vecs.length; i++) {
                            vecs[i] = new Vector3f(world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat());
                        }
                        bezierCurve = new CubicBezierCurve(vecs);
                    }

                    /*if (bezierCurve != null) {
                        float randScale = 0.1F;
                        for (int i = 0; i < bezierCurve.P.length; i++) {
                            bezierCurve.P[i].add((world.rand.nextFloat() - world.rand.nextFloat()) * randScale, (world.rand.nextFloat() - world.rand.nextFloat()) * randScale, (world.rand.nextFloat() - world.rand.nextFloat()) * randScale);
                            bezierCurve.P[i].normalize();
                        }
                    }*/

                    if (bezierCurve != null) {
                        float randScale = 0.1F;
                        for (int i = 0; i < bezierCurve.P.length; i++) {
                            if (vecSpeeds[i] == null) {
                                vecSpeeds[i] = new Vector3f(world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat());
                            }

                            bezierCurve.P[i].add(vecSpeeds[i].getX() * 0.01F, vecSpeeds[i].getY() * 0.01F, vecSpeeds[i].getZ() * 0.01F);

                            float maxY = 0.75F;
                            float minY = 0.25F;

                            if (i == 0) {
                                maxY = 0.25F;
                                minY = 0.0F;
                            } else if (i == 1) {
                                maxY = 0.9F;
                                minY = 0.1F;
                            } else if (i == 2) {
                                maxY = 0.9F;
                                minY = 0.1F;
                            } else if (i == 3) {
                                maxY = 1.0F;
                                minY = 0.75F;
                            }

                            if (bezierCurve.P[i].getX() > 1) {
                                vecSpeeds[i].set(world.rand.nextFloat() * -1, vecSpeeds[i].getY(), vecSpeeds[i].getZ());
                            } else if (bezierCurve.P[i].getX() < 0) {
                                vecSpeeds[i].set(world.rand.nextFloat(), vecSpeeds[i].getY(), vecSpeeds[i].getZ());
                            }
                            if (bezierCurve.P[i].getY() > maxY) {
                                vecSpeeds[i].set(vecSpeeds[i].getX(), world.rand.nextFloat() * -1, vecSpeeds[i].getZ());
                            } else if (bezierCurve.P[i].getY() < minY) {
                                vecSpeeds[i].set(vecSpeeds[i].getX(), world.rand.nextFloat(), vecSpeeds[i].getZ());
                            }
                            if (bezierCurve.P[i].getZ() > 1) {
                                vecSpeeds[i].set(vecSpeeds[i].getX(), vecSpeeds[i].getY(), world.rand.nextFloat() * -1);
                            } else if (bezierCurve.P[i].getZ() < 0) {
                                vecSpeeds[i].set(vecSpeeds[i].getX(), vecSpeeds[i].getY(), world.rand.nextFloat());
                            }
                            //bezierCurve.P[i].normalize();
                        }

                        //bezierCurve.P[0] = new Vector3f(0.5F, 0, 0.5F);
                        bezierCurve.P[3].set(bezierCurve.P[3].getX(), 1, bezierCurve.P[3].getZ());

                        bezierCurve.P[3] = new Vector3f(0.5F, 1, 0.5F);
                    }

                    /*if (mc.world.getGameTime() % 40 == 0 && WATUT.playerManagerClient.getPlayerStatus(mc.player.getUniqueID()).getStatusType() == PlayerStatus.StatusType.CHAT) {
                        System.out.println("x: " + vecAngles.getX());
                        System.out.println("y: " + vecAngles.getY());
                        System.out.println("z: " + vecAngles.getZ());
                        //System.out.println("yDiff: " + (playerEntity.getPosY() - testY));
                        System.out.println("pitchAngle: " + pitchAngle);
                    }*/

                    Iterator<HeartParticle2> it = particles.iterator();
                    int index = 0;
                    while (it.hasNext()) {
                        HeartParticle2 particle = it.next();
                        if (!particle.isAlive()) {
                            it.remove();
                        } else {
                            //(index * (360 / particleCount))
                            float x = 0;//((world.getGameTime() * 0.5F) % 360);
                            float y = /*((world.getGameTime() * 3) % 360) + */((index % particleCountCircle) * (360 / particleCountCircle));
                            float y2 = ((world.getGameTime() * 3) % 360) + ((index % particleCountCircle) * (360 / particleCountCircle));
                            float z = 0;//((world.getGameTime() * 0.3F) % 360);

                            y = vecAngles.getY() - 90;

                            int yDiff = (index / particleCountCircle) - (particleCountLayers / 2);
                            float yDiffDist = 0.01F;

                            int curLayer = (index / particleCountCircle);
                            float curvePoint = (float)curLayer / (float)particleCountLayers;
                            float stretchCurveY = 4F;
                            float curveAmp = 2F;

                            float distFinal = dist / 2F;

                            Vector3f vecCurve1 = bezierCurve.getValue(curvePoint);
                            Vector3f vecCurve2 = bezierCurve.getValue((float)Math.min(1D, (float)(curLayer+1) / (float)particleCountLayers));
                            Vector2f curvePointYawPitch = getYawPitch(vecCurve2, vecCurve1);

                            if ((index % particleCountCircle) == 0) {
                                //System.out.println(curvePointYawPitch.x + " - " + curvePointYawPitch.y);
                            }

                            //Quaternion quaternionY = new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), -y, true);
                            Quaternion quaternionY = new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), -curvePointYawPitch.x - 90, true);
                            Quaternion quaternionYCircle = new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), -y2, true);

                            Quaternion quatPitch = new Quaternion(new Vector3f(1.0F, 0.0F, 0.0F), curvePointYawPitch.y, true);
                            //Vector3f vecNew = new Vector3f(1F, 1 + ((float)yDiff) * yDiffDist, 0);
                            //Vector3d vecCurve = bezierCurve.getValue(curvePoint);
                            Vector3f vecCurve = bezierCurve.getValue(curvePoint);
                            //System.out.println("curvePoint: " + curvePoint + ", " + vecCurve);
                            //Vector3f vecNew = new Vector3f((float)vecCurve.x * curveAmp, (float)vecCurve.y * curveAmp * stretchCurveY * (((float)yDiff) * yDiffDist) + 10F, (float)vecCurve.z * curveAmp);
                            //Vector3f vecNew = new Vector3f((float)vecCurve.getX() * curveAmp - curveAmp/2F, (1F + ((float)yDiff) * yDiffDist * (dist*2F)) - (dist/2F), (float)vecCurve.getZ() * curveAmp - curveAmp/2F);
                            //Vector3f vecNew = new Vector3f(1F * curveAmp, (1F + ((float)yDiff) * yDiffDist * (dist*2F)) - (dist/2F), 0F);
                            //Vector3f vecNew = new Vector3f(1F * curveAmp, (1F + ((float)yDiff) * yDiffDist * (dist*2F)) - (dist/2F), 1F * curveAmp);
                            //Vector3f vecNew = new Vector3f(1F * curveAmp, (((float)yDiff) * distFinal) - (dist/2F), 0);
                            Vector3f vecNew = new Vector3f(0.3F + (curLayer * 0.05F)/* + (curLayer * 0.05F)*/, 0F, 0);

                            float rotAroundPosX = 0;
                            float rotAroundPosY = 0;
                            float rotAroundPosZ = 0;
                            Matrix3f matrix = new Matrix3f();
                            matrix.setIdentity();
                            matrix.mul(quaternionY);
                            matrix.mul(quatPitch);
                            matrix.mul(quaternionYCircle);
                            vecNew.transform(matrix);

                            rotAroundPosX = vecNew.getX();
                            rotAroundPosY = vecNew.getY();
                            rotAroundPosZ = vecNew.getZ();

                            //particle.setPosition(pos1.getX() + rotAroundPosX, pos1.getY() + rotAroundPosY, pos1.getZ() + rotAroundPosZ);
                            particle.setPosition(pos1.getX() + (vecCurve1.getX()*distFinal) + rotAroundPosX, pos1.getY() + (vecCurve1.getY()*distFinal) + rotAroundPosY, pos1.getZ() + (vecCurve1.getZ()*distFinal) + rotAroundPosZ);
                        }
                        index++;
                    }
                }
            }
        }

        lookupPlayerStatus.entrySet().stream().forEach(entrySet -> {
            //WATUT.LOGGER.debug("client:" + world.getGameTime() + " - " + entrySet.getValue().getUuid() + " -> " + entrySet.getValue().getStatusType());
        });

        if (funnel == null) {
            funnel = new TornadoFunnel();
            funnel.pos = new Vector3d(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        }

        //funnel.tickGame();

    }

    private void triggerScreenChange(PlayerStatus.StatusType type) {
        Minecraft mc = Minecraft.getInstance();
        PlayerStatus status = getPlayerStatus(mc.player.getUniqueID());
        status.setStatusType(type);
        ToServerPlayerStatusMessage message = new ToServerPlayerStatusMessage(status.getUuid(), status.getStatusType());
        WATUTNetwork.CHANNEL.sendToServer(message);
        WATUT.LOGGER.debug("syncing state client -> server");
    }

    public float getDistance(Vector3f vec1, Vector3f vec2) {
        float f = (vec1.getX() - vec2.getX());
        float f1 = (vec1.getY() - vec2.getY());
        float f2 = (vec1.getZ() - vec2.getZ());
        return MathHelper.sqrt(f * f + f1 * f1 + f2 * f2);
    }

    /**
     *
     * @param pos2
     * @param pos1
     * @return yaw and pitch in degrees
     */
    public Vector2f getYawPitch(Vector3f pos2, Vector3f pos1) {
        float dist = getDistance(pos1, pos2);
        Vector3f vecDiff = new Vector3f(
                (pos1.getX() - pos2.getX()) / dist,
                (pos1.getY() - pos2.getY()) / dist,
                (pos1.getZ() - pos2.getZ()) / dist);
        Vector3f vecAngles = new Vector3f(
                (float)Math.atan2(vecDiff.getY(), vecDiff.getZ()),
                (float)Math.atan2(vecDiff.getZ(), vecDiff.getX()), //invert if needed
                (float)Math.atan2(vecDiff.getX(), vecDiff.getY())); //invert if needed

        double xx = pos1.getX() - pos2.getX();
        double zz = pos1.getZ() - pos2.getZ();
        double xzDist = Math.sqrt(xx * xx + zz * zz);
        double wat = xzDist / dist;
        float pitchAngle = (float)Math.toDegrees(Math.atan2(vecDiff.getY(), xzDist / dist));

        vecAngles = new Vector3f((float)Math.toDegrees(vecAngles.getX()), (float)Math.toDegrees(vecAngles.getY()), (float)Math.toDegrees(vecAngles.getZ()));

        pitchAngle += 90;

        return new Vector2f(vecAngles.getY(), pitchAngle);
    }

}
