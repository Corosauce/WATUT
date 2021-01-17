package com.corosus.watut;

import com.corosus.watut.network.ToClientPlayerStatusMessage;
import com.corosus.watut.network.ToServerPlayerStatusMessage;
import com.corosus.watut.network.WATUTNetwork;
import com.corosus.watut.particles.HeartParticle2;
import com.corosus.watut.particles.StatusParticle;
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
                if (mc.player.getDistance(playerEntity) < 20) {
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
                        //particleTest.move(0, -0.1, 0);
                        mc.particles.addEffect(particleTest);
                        particles.add(particleTest);
                    }

                    Iterator<HeartParticle2> it = particles.iterator();
                    int index = 0;
                    while (it.hasNext()) {
                        HeartParticle2 particle = it.next();
                        if (!particle.isAlive()) {
                            it.remove();
                        } else {
                            //(index * (360 / particleCount))
                            float x = ((world.getGameTime() * 0.5F) % 360);
                            float y = ((world.getGameTime() * 3) % 360) + ((index % particleCountCircle) * (360 / particleCountCircle));
                            float z = ((world.getGameTime() * 0.3F) % 360);

                            int yDiff = (index / particleCountCircle) - (particleCountLayers / 2);
                            float yDiffDist = 0.1F;

                            //Quaternion q = new Quaternion(new Vector3f(1.0F, 0.0F, 1.0F), 45, true);
                            //Matrix4f m = new Matrix4f();

                            Quaternion quaternionX = new Quaternion(new Vector3f(1.0F, 0.0F, 0.0F), (float)(-x), true);
                            Quaternion quaternionY = new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), (float)(-y), true);
                            Quaternion quaternionZ = new Quaternion(new Vector3f(0.0F, 0.0F, 1.0F), (float)(-z), true);
                            //quaternion.multiply(new Quaternion(new Vector3f(1.0F, 0.0F, 0.0F), (float)(-x), true));
                            //quaternion.multiply(new Quaternion(new Vector3f(0.0F, 0.0F, 1.0F), (float)(-z), true));
                            //Vector3f vec = new Vector3f(1, 1, 1);
                            Vector3f vec = new Vector3f(1F, 0 + ((float)yDiff) * yDiffDist, 0);
                            /*Vector3f vecXZ = new Vector3f(1F, 0, 0);
                            Vector3f vecY = new Vector3f(0, 0.2F, 0);
                            Vector3f vecZ = new Vector3f(0, 0, 1);*/

                            //setup the ring of particles (yaw)
                            vec.transform(quaternionY);
                            //pitch them
                            vec.transform(quaternionX);
                            //roll them
                            vec.transform(quaternionZ);
                            //vecXZ.transform(quaternionZ);
                            //vecX.transform(quaternionX);
                            //vecY.transform(quaternionX);
                            //vecZ.transform(quaternionX);

                            //Vector3f vec = new Vector3f(0, 0, 0);
                            //vec.add(vecX);
                            //vec.add(vec);
                            //vec.add(vecXZ);
                            //vec.add(vecY);
                            //vec.add(vecZ);

                            float rotAroundPosX = 0;
                            float rotAroundPosY = 0;
                            float rotAroundPosZ = 0;
                            /*float rotAroundPosX = quaternion.getX();
                            float rotAroundPosY = quaternion.getY();
                            float rotAroundPosZ = quaternion.getZ();*/
                            //TransformationMatrix transformationMatrix = new TransformationMatrix(vec, quaternion, null, null);
                            //MatrixStack matrixStack = new MatrixStack();
                            /*Matrix4f matrix3f = new Matrix4f();
                            matrix3f.setIdentity();
                            matrix3f.translate(vec);
                            matrix3f.mul(quaternion);
                            float xx = ObfuscationReflectionHelper.getPrivateValue(Matrix4f.class, matrix3f, "m03");
                            float yy = ObfuscationReflectionHelper.getPrivateValue(Matrix4f.class, matrix3f, "m13");
                            float zz = ObfuscationReflectionHelper.getPrivateValue(Matrix4f.class, matrix3f, "m23");*/
                            //vec.transform(matrix3f);

                            rotAroundPosX = vec.getX();
                            rotAroundPosY = vec.getY();
                            rotAroundPosZ = vec.getZ();

                            /*rotAroundPosX = xx;
                            rotAroundPosY = yy;
                            rotAroundPosZ = zz;

                            Vector3f vecOut = transformationMatrix.getTranslation();
                            rotAroundPosX = vecOut.getX();
                            rotAroundPosY = vecOut.getY();
                            rotAroundPosZ = vecOut.getZ();*/

                            particle.setPosition(playerEntity.getPosX() + rotAroundPosX, playerEntity.getPosY() + 2.2 + rotAroundPosY, playerEntity.getPosZ() + rotAroundPosZ);
                        }
                        index++;
                    }

                    /*if (particleTest == null || !particleTest.isAlive()) {
                        particleTest = new HeartParticle2(mc.world, playerEntity.getPosX(), playerEntity.getPosY() + 2.2, playerEntity.getPosZ());
                        particleTest.setSprite(EventHandlerForge.square16);
                        particleTest.setMaxAge(50);
                        //particleTest.move(0, -0.1, 0);
                        mc.particles.addEffect(particleTest);
                    }*/



                }


            }
        }

        lookupPlayerStatus.entrySet().stream().forEach(entrySet -> {
            //WATUT.LOGGER.debug("client:" + world.getGameTime() + " - " + entrySet.getValue().getUuid() + " -> " + entrySet.getValue().getStatusType());
        });

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
