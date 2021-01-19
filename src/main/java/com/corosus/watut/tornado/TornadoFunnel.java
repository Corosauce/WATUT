package com.corosus.watut.tornado;

import com.corosus.watut.EventHandlerForge;
import com.corosus.watut.particles.HeartParticle2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;

import java.util.*;

/**
 * To contain the full funnel, with each component piece
 */
public class TornadoFunnel {

    public Vector3d pos = new Vector3d(0, 0, 0);

    public LinkedList<FunnelPiece> listFunnel = new LinkedList();

    //temp?

    public int amountPerLayer = 30;
    public int particleCount = amountPerLayer * 50;
    public int funnelPieces = 2;

    static class FunnelPiece {

        public List<HeartParticle2> listParticles = new ArrayList<>();

        public Vector3d posStart = new Vector3d(0, 0, 0);
        public Vector3d posEnd = new Vector3d(0, 20, 0);

        //public Vector3d vecDir = new Vector3d(0, 0, 0);
        public float vecDirX = 0;
        public float vecDirZ = 0;

        public boolean needInit = true;

    }

    public TornadoFunnel() {

    }

    public void tickGame() {

        amountPerLayer = 30;
        particleCount = amountPerLayer * 50;
        funnelPieces = 2;



        tickGameTestCreate();
        tickUpdateFunnel();
    }

    private void tickGameTestCreate() {

        PlayerEntity entP = Minecraft.getInstance().player;

        Random rand = new Random();

        while (listFunnel.size() < funnelPieces) {
            addPieceToEnd(new FunnelPiece());
        }

        //for (FunnelPiece piece : listFunnel) {
        for (int i = 0; i < listFunnel.size(); i++) {
            FunnelPiece piece = listFunnel.get(i);

            if (piece.needInit) {
                piece.needInit = false;

                int height = 10;
                //temp
                //TODO: LINK TO PREVIOUS OR NEXT PIECE IF THERE IS ONE
                if (i == 0) {
                    piece.posStart = new Vector3d(entP.getPosX(), entP.getPosY(), entP.getPosZ());
                    piece.posEnd = new Vector3d(entP.getPosX(), entP.getPosY() + height, entP.getPosZ());
                    //piece.posEnd = new Vector3d(entP.posX, entP.posY + entP.getEyeHeight(), entP.posZ);

                } else {
                    Vector3d prev = listFunnel.get(i-1).posEnd;
                    piece.posStart = new Vector3d(prev.x, prev.y, prev.z);
                    piece.posEnd = new Vector3d(piece.posStart.x, piece.posStart.y + height, piece.posStart.z);
                }

                if (i == funnelPieces - 1) {
                    piece.posEnd = new Vector3d(piece.posStart.x, piece.posStart.y + height, piece.posStart.z);
                }

                piece.vecDirX = rand.nextBoolean() ? 1 : -1;
                piece.vecDirZ = rand.nextBoolean() ? 1 : -1;
            }

            double dist = piece.posStart.distanceTo(piece.posEnd);

            double sizeXYParticle = 1;
            double funnelRadius = 3;

            double circumference = funnelRadius * 2D * Math.PI;

            amountPerLayer = (int) (circumference / sizeXYParticle);
            int layers = (int) (dist / sizeXYParticle);

            particleCount = layers * amountPerLayer;

            while (piece.listParticles.size() > particleCount) {
                piece.listParticles.get(piece.listParticles.size() - 1).setExpired();
                piece.listParticles.remove(piece.listParticles.size() - 1);
            }

            while (piece.listParticles.size() < particleCount) {
                BlockPos pos = new BlockPos(piece.posEnd.x, piece.posEnd.y, piece.posEnd.z);

                //if (entP.getDistanceSq(pos) < 10D * 10D) continue;

                //pos = world.getPrecipitationHeight(pos).add(0, 1, 0);
                
                ClientWorld world = (ClientWorld)entP.world;

                HeartParticle2 particleTest = new HeartParticle2(world, pos.getX() + rand.nextFloat(),
                        pos.getY(),
                        pos.getZ() + rand.nextFloat());

                particleTest.setSprite(EventHandlerForge.square16);
                particleTest.setMaxAge(250);
                particleTest.setMotion(0, 0, 0);
                particleTest.setScale(0.1F);
                //particleTest.setColor(0.1F * (particles.size() % particleCountCircle), 0, 0);
                particleTest.setColor(world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat());
                /*if (piece.listParticles.size() < particleCountCircle * 5) {
                    particleTest.setColor(1, 1, 1);
                }*/
                //particleTest.move(0, -0.1, 0);
                Minecraft.getInstance().particles.addEffect(particleTest);

                piece.listParticles.add(particleTest);
            }
        }

        //reset
        /*for (int i = 0; i < listFunnel.size(); i++) {
            FunnelPiece piece = listFunnel.get(i);

            while (piece.listParticles.size() > particleCount) {
                piece.listParticles.get(piece.listParticles.size() - 1).setExpired();
                piece.listParticles.remove(piece.listParticles.size() - 1);
            }
        }*/
        //listFunnel.clear();


    }

    private void tickUpdateFunnel() {

        World world = Minecraft.getInstance().world;
        PlayerEntity player = Minecraft.getInstance().player;

        //for (FunnelPiece piece : listFunnel) {
        for (int ii = 0; ii < listFunnel.size(); ii++) {
            FunnelPiece piece = listFunnel.get(ii);

            /*if (ii == listFunnel.size() - 1) {
                piece.posEnd = new Vector3d(piece.posStart.x, piece.posStart.y + 20, piece.posStart.z);
            }*/

            double rate = 0.5F/* + (ii * 0.1F)*/;
            double distMax = 20;

            Random rand = new Random();

            //piece.posEnd = piece.posEnd.addVector(rate * piece.vecDirX, 0, rate * piece.vecDirZ);
            //piece.posEnd = piece.posEnd.addVector(rate * rand.nextFloat() * piece.vecDirX, 0, rate * rand.nextFloat() * piece.vecDirZ);

            int offset = 360 / listFunnel.size();
            long timeC = (world.getGameTime() * (ii+1) + (offset * ii)) * 1;
            float range = 35F;

            //piece.posEnd = new Vector3d(piece.posStart.x + Math.sin(Math.toRadians(timeC % 360)) * range, piece.posStart.y + 3, piece.posStart.z + Math.cos(Math.toRadians(timeC % 360)) * range);

            //piece.posEnd.

            //piece.posEnd = piece.posEnd.addVector(-1, 0, 0);

            double xx1 = piece.posEnd.x - piece.posStart.x;
            double zz1 = piece.posEnd.z - piece.posStart.z;
            double xzDist2 = (double) MathHelper.sqrt(xx1 * xx1 + zz1 * zz1);

            if (xzDist2 > distMax) {
                if (piece.posEnd.x - piece.posStart.x > 0) {
                    piece.vecDirX = -1;
                }

                if (piece.posEnd.x - piece.posStart.x < 0) {
                    piece.vecDirX = 1;
                }

                if (piece.posEnd.z - piece.posStart.z > 0) {
                    piece.vecDirZ = -1;
                }

                if (piece.posEnd.z - piece.posStart.z < 0) {
                    piece.vecDirZ = 1;
                }
            }

            /*if (Math.abs(piece.posStart.x - piece.posEnd.x) > distMax) {
                piece.vecDirX *= -1;
            }

            if (Math.abs(piece.posStart.z - piece.posEnd.z) > distMax) {
                piece.vecDirZ *= -1;
            }*/

            if (ii > 0) {
                Vector3d prev = listFunnel.get(ii-1).posEnd;
                piece.posStart = new Vector3d(prev.x, prev.y, prev.z);
            }

            double dist = piece.posStart.distanceTo(piece.posEnd);

            double x1 = piece.posEnd.x - piece.posStart.x;
            double y1 = piece.posEnd.y - piece.posStart.y;
            double z1 = piece.posEnd.z - piece.posStart.z;
            Vector3d vec = new Vector3d(x1 / dist, y1 / dist, z1 / dist);

            double sizeXYParticle = 1;
            double funnelRadius = 3;

            double circumference = funnelRadius * 2D * Math.PI;

            amountPerLayer = (int) (circumference / sizeXYParticle);
            int layers = (int) (dist / sizeXYParticle);

            particleCount = layers * amountPerLayer;

            Iterator<HeartParticle2> it = piece.listParticles.iterator();
            int index = 0;
            while (it.hasNext()) {
                HeartParticle2 part = it.next();
                if (!part.isAlive()) {
                    it.remove();
                } else {

                    int particleCountCircle = 20;
                    int particleCountLayers = 40;

                    int yIndex = index / amountPerLayer;
                    int rotIndex = index % amountPerLayer;
                    int yCount = particleCount / amountPerLayer;

                    float x = 0;//((world.getGameTime() * 0.5F) % 360);
                    float y = /*((world.getGameTime() * 3) % 360) + */((index % particleCountCircle) * (360 / particleCountCircle));
                    float y2 = ((world.getGameTime() * 3) % 360) + ((index % particleCountCircle) * (360 / particleCountCircle));
                    float z = 0;//((world.getGameTime() * 0.3F) % 360);


                    int testY = 100;

                    float dist2 = (float)Math.sqrt(player.getDistanceSq(0.5, testY, 0.5));
                    Vector3f vecDiff = new Vector3f(
                            (float)(player.getPosX() - 0.5) / dist2,
                            (float)(player.getPosY() - testY) / dist2,
                            (float)(player.getPosZ() - 0.5) / dist2);
                    Vector3f vecAngles = new Vector3f(
                            (float)Math.atan2(vecDiff.getY(), vecDiff.getZ()),
                            (float)Math.atan2(vecDiff.getZ(), vecDiff.getX()), //invert if needed
                            (float)Math.atan2(vecDiff.getX(), vecDiff.getY())); //invert if needed

                    //convert to degrees
                    vecAngles = new Vector3f((float)Math.toDegrees(vecAngles.getX()), (float)Math.toDegrees(vecAngles.getY()), (float)Math.toDegrees(vecAngles.getZ()));

                    double xx = player.getPosX() - 0.5;
                    double zz = player.getPosZ() - 0.5;
                    double xzDist = Math.sqrt(xx * xx + zz * zz);
                    float pitchAngle = (float)Math.toDegrees(Math.atan2(vecDiff.getY(), xzDist / dist2));

                    pitchAngle += 90;
                    y = vecAngles.getY() - 90;

                    int yDiff = (index / particleCountCircle) - (particleCountLayers / 2);
                    float yDiffDist = 0.1F;

                    Quaternion quaternionY = new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), -y, true);
                    Quaternion quaternionYCircle = new Quaternion(new Vector3f(0.0F, 1.0F, 0.0F), -y2, true);

                    Quaternion quatPitch = new Quaternion(new Vector3f(1.0F, 0.0F, 0.0F), -pitchAngle, true);
                    Vector3f vecNew = new Vector3f(1F, 1 + ((float)yDiff) * yDiffDist, 0);

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

                    part.setPosition(player.getPosX() + rotAroundPosX, player.getPosY() + rotAroundPosY, player.getPosZ() + rotAroundPosZ);
                }

                index++;
            }
        }
    }

    public void addPieceToEnd(FunnelPiece piece) {
        listFunnel.addLast(piece);
    }

}
