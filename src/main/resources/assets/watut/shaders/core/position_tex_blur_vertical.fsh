#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform vec2 resolution;
uniform float radius;
uniform float blurLevel;

in vec2 texCoord0;

out vec4 fragColor;

void main() {

    float xs = resolution[0];
    float ys = resolution[1];
    float xmid = xs / 2;
    float ymid = ys / 2;

    vec2 tex_offset = 1.0 / textureSize(Sampler0, 0); // size of a single texel
    vec4 result = texture(Sampler0, texCoord0);
    if (blurLevel != 0) {
        result.rgb = vec3(0);
        float weights[5];
        if (blurLevel == 1) {
            weights = float[](0.45, 0.1, 0.1, 0.05, 0.02);
        } else {
            weights = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);
        }
        int blurRange = 4;
        /*float weights[3] = float[](0.294117, 0.235294, 0.117647);
        int blurRange = 2;*/
        for (int i = -blurRange; i <= blurRange; ++i) {
            result.rgb += texture(Sampler0, texCoord0 + vec2(0.0, tex_offset.y * float(i))).rgb * weights[abs(i)];
        }
    }

    vec2 pixelCoord = gl_FragCoord.xy;
    //col.a = texCoord0.y;
    float cutoff = radius;
    //cutoff = 32;
    int cutoff2 = 32;
    float dist = distance(pixelCoord, vec2(xmid, ymid));
    //vec4 color = vec4(result, 1.0);
    if (dist > cutoff) {
        //TODO: there might be a problem with my strat of using the same texture to render back onto itself, try enabling discard below to see the weirdness outside of the circle
        //if i discard, the texture data remains because its already in the texture
        //for now its ok cause im forcing all other pixels to be alpha 0
        //discard;
        //color.a = min(color.a, 1 - ((dist - cutoff) / cutoff2));
        //avoid doing the alpha calculation where theres no point, saves on performance a lot
        if (dist > cutoff + cutoff2) {
            result.a = 0;
        } else {
            result.a = min(result.a, 1 - ((dist - cutoff) / cutoff2));
        }
    }
    /*if (result.a <= 0.0) {
        discard;
    }*/
    //col.a = 0.32;
    /*if (fragColor.a <= 0.0) {
        discard;
    }*/
    //fragColor = color;
    fragColor = result;
}
