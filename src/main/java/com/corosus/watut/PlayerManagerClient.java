package com.corosus.watut;

import com.corosus.watut.network.ToClientPlayerStatusMessage;
import com.corosus.watut.network.ToServerPlayerStatusMessage;
import com.corosus.watut.network.WATUTNetwork;
import com.corosus.watut.particles.HeartParticle2;
import com.corosus.watut.particles.StatusParticle;
import com.corosus.watut.tornado.CubicBezierCurve;
import com.corosus.watut.tornado.TornadoFunnel;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.particle.TexturedParticle;
import net.minecraft.entity.player.PlayerEntity;
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

    CubicBezierCurve bezierCurve;

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

                    float dist = (float)Math.sqrt(playerEntity.getDistanceSq(0.5, testY, 0.5));
                    Vector3f vecDiff = new Vector3f(
                            (float)(playerEntity.getPosX() - 0.5) / dist,
                            (float)(playerEntity.getPosY() - testY) / dist,
                            (float)(playerEntity.getPosZ() - 0.5) / dist);
                    Vector3f vecAngles = new Vector3f(
                            (float)Math.atan2(vecDiff.getY(), vecDiff.getZ()),
                            (float)Math.atan2(vecDiff.getZ(), vecDiff.getX()), //invert if needed
                            (float)Math.atan2(vecDiff.getX(), vecDiff.getY())); //invert if needed

                    //convert to degrees
                    vecAngles = new Vector3f((float)Math.toDegrees(vecAngles.getX()), (float)Math.toDegrees(vecAngles.getY()), (float)Math.toDegrees(vecAngles.getZ()));

                    double xx = playerEntity.getPosX() - 0.5;
                    double zz = playerEntity.getPosZ() - 0.5;
                    double xzDist = Math.sqrt(xx * xx + zz * zz);
                    float pitchAngle = (float)Math.toDegrees(Math.atan2(vecDiff.getY(), xzDist / dist));

                    pitchAngle += 90;

                    if (bezierCurve == null || world.getGameTime() % 40 == 0) {
                        Vector3d[] vecs = new Vector3d[4];
                        for (int i = 0; i < vecs.length; i++) {
                            vecs[i] = new Vector3d(world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat());
                        }
                        bezierCurve = new CubicBezierCurve(vecs);
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
                            float yDiffDist = 0.1F;

                            int curLayer = (index / particleCountCircle);
                            float curvePoint = (float)curLayer / (float)particleCountLayers;
                            float stretchCurveY = 4F;
                            float curveAmp = 2F;

                            Quaternion quaternionY = new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), -y, true);
                            Quaternion quaternionYCircle = new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), -y2, true);

                            Quaternion quatPitch = new Quaternion(new Vector3f(1.0F, 0.0F, 0.0F), -pitchAngle, true);
                            //Vector3f vecNew = new Vector3f(1F, 1 + ((float)yDiff) * yDiffDist, 0);
                            Vector3d vecCurve = bezierCurve.getValue(curvePoint);
                            //System.out.println("curvePoint: " + curvePoint + ", " + vecCurve);
                            Vector3f vecNew = new Vector3f((float)vecCurve.x * curveAmp, (float)vecCurve.y * curveAmp * stretchCurveY * (((float)yDiff) * yDiffDist) + 10F, (float)vecCurve.z * curveAmp);

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

                            particle.setPosition(playerEntity.getPosX() + rotAroundPosX, playerEntity.getPosY() + rotAroundPosY, playerEntity.getPosZ() + rotAroundPosZ);
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

}
