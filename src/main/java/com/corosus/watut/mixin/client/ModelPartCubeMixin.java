package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ModelPart.Cube.class)
public abstract class ModelPartCubeMixin {


    @Redirect(method = "compile",
            at = @At(value = "INVOKE",
                    target = "Lorg/joml/Matrix4f;transformPosition(FFFLorg/joml/Vector3f;)Lorg/joml/Vector3f;"))
    public Vector3f transformPosition(Matrix4f instance, float x, float y, float z, Vector3f dest) {

        return WatutMod.getPlayerStatusManagerClient().transformPosition(((ModelPart.Cube)(Object)this), instance, x, y, z, dest);

        /*if (WatutMod.getPlayerStatusManagerClient().transformPosition(instance, x, y, z, dest)) {
            instance.transformPosition(x, y, z, dest);
        }


        return dest;*/
    }

}