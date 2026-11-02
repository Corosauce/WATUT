package com.corosus.watut.loader.neoforge;

import com.corosus.coroutil.util.CULog;
import com.corosus.watut.WatutRenderPipelines;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

public class IrisCompat {

    public static void assignPipelines() {
        try {
            IrisApi api = IrisApi.getInstance();
            if (api.getMinorApiRevision() < 3) {
                CULog.log("WATUT: Iris API revision " + api.getMinorApiRevision() + " has no assignPipeline, particles may render wrong with shaders");
                return;
            }
            api.assignPipeline(WatutRenderPipelines.TRANSLUCENT_PARTICLE_NO_CULL, IrisProgram.PARTICLES_TRANSLUCENT);
            CULog.log("WATUT: assigned particle pipelines to Iris programs");
        } catch (Throwable e) {
            CULog.err("WATUT: failed to assign pipelines to Iris: " + e);
        }
    }
}
