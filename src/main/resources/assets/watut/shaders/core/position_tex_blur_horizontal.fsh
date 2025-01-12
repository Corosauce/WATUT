#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 tex_offset = 1.0 / textureSize(Sampler0, 0); // size of a single texel
    //vec3 result = vec3(0.0);
    vec4 result = texture(Sampler0, texCoord0);
    result.rgb = vec3(0);
    float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);
    int blurRange = 4;
    /*float weights[3] = float[](0.294117, 0.235294, 0.117647);
    int blurRange = 2;*/
    for (int i = -blurRange; i <= blurRange; ++i) {
        //result += texture(Sampler0, texCoord0 + vec2(0.0, tex_offset.y * float(i))).rgb * weights[abs(i)];
        //result.rgb += texture(Sampler0, texCoord0 + vec2(0.0, tex_offset.y * float(i))).rgb * weights[abs(i)];
        result.rgb += texture(Sampler0, texCoord0 + vec2(tex_offset.x * float(i), 0.0)).rgb * weights[abs(i)];
    }
    //fragColor = vec4(result, 1.0);
    fragColor = result;
}
