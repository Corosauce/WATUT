package com.corosus.watut;

public class ShaderRegistry {

    public static ShaderRegistry instance;

    public static void init() {

        PlayerStatusManagerClient.particle = new ShaderProgramBlur("core/particle");
        PlayerStatusManagerClient.positionTexBlur = new ShaderProgramBlur("core/position_tex_blur");
        PlayerStatusManagerClient.positionTexBlurHorizontal = new ShaderProgramBlur("core/position_tex_blur_horizontal");
        PlayerStatusManagerClient.positionTexBlurVertical = new ShaderProgramBlur("core/position_tex_blur_vertical");

    }

    public static void reloadShaders() {
        PlayerStatusManagerClient.particle.markUniformsNeedUpdate();
        PlayerStatusManagerClient.positionTexBlur.markUniformsNeedUpdate();
        PlayerStatusManagerClient.positionTexBlurHorizontal.markUniformsNeedUpdate();
        PlayerStatusManagerClient.positionTexBlurVertical.markUniformsNeedUpdate();
    }

}
