#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec2 Resolution;

out vec4 fragColor;

const float weight[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a == 0.0) {
        discard;
    }

    vec2 Resolution2 = vec2(256, 256);

    vec2 texOffset = 1.0 / Resolution2; // Size of a texel
    vec3 result = texture(Sampler0, texCoord0).rgb * weight[0];

    // Combine horizontal and vertical blur in one loop
    for (int i = 1; i < 4; ++i) {
        // Horizontal and vertical samples
        vec2 offset = vec2(texOffset.x * i, texOffset.y * i);

        // Accumulate samples
        result += texture(Sampler0, texCoord0 + vec2(offset.x, 0.0)).rgb * weight[i];
        //result += texture(Sampler0, texCoord0 - vec2(offset.x, 0.0)).rgb * weight[i];
        result += texture(Sampler0, texCoord0 + vec2(0.0, offset.y)).rgb * weight[i];
        //result += texture(Sampler0, texCoord0 - vec2(0.0, offset.y)).rgb * weight[i];
    }

    fragColor = vec4(result, 1.0) * ColorModulator;

    //color.r = 1;
    //fragColor = color * ColorModulator;
}
