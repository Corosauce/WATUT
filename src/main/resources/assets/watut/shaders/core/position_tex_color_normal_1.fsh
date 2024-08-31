#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
//uniform vec3 Lightning_Pos;

in vec2 texCoord0;
in float vertexDistance;
in vec4 vertexColor;
in vec4 normal;

out vec4 fragColor;

vec4 testMethod() {
    return vec4(1.0);
}

float test2() {
    return 0.0;
}

vec4 mainImage(vec2 fragCoord)
{
    float Pi = 6.28318530718; // Pi*2

    // GAUSSIAN BLUR SETTINGS {{{
    float Directions = 16.0; // BLUR DIRECTIONS (Default 16.0 - More is better but slower)
    float Quality = 8.0; // BLUR QUALITY (Default 4.0 - More is better but slower)
    float Size = 20.0; // BLUR SIZE (Radius)
    // GAUSSIAN BLUR SETTINGS }}}

    vec2 iResolution = vec2(1920, 1080);

    vec2 Radius = Size/iResolution.xy;

    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/iResolution.xy;
    // Pixel colour
    //vec4 Color = texture(iChannel0, uv);
    vec4 Color = vec4(0.0, 0.0, 0.0, 0.0);

    // Blur calculations
    for( float d=0.0; d<Pi; d+=Pi/Directions)
    {
        for(float i=1.0/Quality; i<=1.0; i+=1.0/Quality)
        {
            //Color += texture( iChannel0, uv+vec2(cos(d),sin(d))*Radius*i);
        }
    }

    // Output to screen
    Color /= Quality * Directions + 1.0;
    //return Color;
    return vec4(0.0);
}

vec4 mainImageNew( vec4 fragColor, vec2 fragCoord )
{
    float Pi = 6.28318530718; // Pi*2

    // GAUSSIAN BLUR SETTINGS {{{
    float Directions = 16.0; // BLUR DIRECTIONS (Default 16.0 - More is better but slower)
    float Quality = 3.0; // BLUR QUALITY (Default 4.0 - More is better but slower)
    float Size = 8.0; // BLUR SIZE (Radius)
    // GAUSSIAN BLUR SETTINGS }}}

    vec2 iResolution = vec2(1920, 1080);

    vec2 Radius = Size/iResolution.xy;

    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/iResolution.xy;
    // Pixel colour
    //vec4 Color = texture(iChannel0, uv);

    // Blur calculations
    for( float d=0.0; d<Pi; d+=Pi/Directions)
    {
        for(float i=1.0/Quality; i<=1.0; i+=1.0/Quality)
        {
            fragColor.rgb += vec3(0.1);//texture( iChannel0, uv+vec2(cos(d),sin(d))*Radius*i);
        }
    }

    // Output to screen
    fragColor /= Quality * Directions - 15.0;
    //fragColor =  Color;
    return fragColor;
}

float rand(vec2 co){
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

// 8x8 Bayer matrix (values 0 to 63 normalized to 0.0 to 1.0)
const float ditherMatrix[8][8] = float[8][8](
float[8](0.0/64.0, 32.0/64.0, 8.0/64.0, 40.0/64.0, 2.0/64.0, 34.0/64.0, 10.0/64.0, 42.0/64.0),
float[8](48.0/64.0, 16.0/64.0, 56.0/64.0, 24.0/64.0, 50.0/64.0, 18.0/64.0, 58.0/64.0, 26.0/64.0),
float[8](12.0/64.0, 44.0/64.0, 4.0/64.0, 36.0/64.0, 14.0/64.0, 46.0/64.0, 6.0/64.0, 38.0/64.0),
float[8](60.0/64.0, 28.0/64.0, 52.0/64.0, 20.0/64.0, 62.0/64.0, 30.0/64.0, 54.0/64.0, 22.0/64.0),
float[8](3.0/64.0, 35.0/64.0, 11.0/64.0, 43.0/64.0, 1.0/64.0, 33.0/64.0, 9.0/64.0, 41.0/64.0),
float[8](51.0/64.0, 19.0/64.0, 59.0/64.0, 27.0/64.0, 49.0/64.0, 17.0/64.0, 57.0/64.0, 25.0/64.0),
float[8](15.0/64.0, 47.0/64.0, 7.0/64.0, 39.0/64.0, 13.0/64.0, 45.0/64.0, 5.0/64.0, 37.0/64.0),
float[8](63.0/64.0, 31.0/64.0, 55.0/64.0, 23.0/64.0, 61.0/64.0, 29.0/64.0, 53.0/64.0, 21.0/64.0)
);

const float ditherMatrixInverted[8][8] = float[8][8](
float[8](1.0 - 0.0/64.0, 1.0 - 32.0/64.0, 1.0 - 8.0/64.0, 1.0 - 40.0/64.0, 1.0 - 2.0/64.0, 1.0 - 34.0/64.0, 1.0 - 10.0/64.0, 1.0 - 42.0/64.0),
float[8](1.0 - 48.0/64.0, 1.0 - 16.0/64.0, 1.0 - 56.0/64.0, 1.0 - 24.0/64.0, 1.0 - 50.0/64.0, 1.0 - 18.0/64.0, 1.0 - 58.0/64.0, 1.0 - 26.0/64.0),
float[8](1.0 - 12.0/64.0, 1.0 - 44.0/64.0, 1.0 - 4.0/64.0, 1.0 - 36.0/64.0, 1.0 - 14.0/64.0, 1.0 - 46.0/64.0, 1.0 - 6.0/64.0, 1.0 - 38.0/64.0),
float[8](1.0 - 60.0/64.0, 1.0 - 28.0/64.0, 1.0 - 52.0/64.0, 1.0 - 20.0/64.0, 1.0 - 62.0/64.0, 1.0 - 30.0/64.0, 1.0 - 54.0/64.0, 1.0 - 22.0/64.0),
float[8](1.0 - 3.0/64.0, 1.0 - 35.0/64.0, 1.0 - 11.0/64.0, 1.0 - 43.0/64.0, 1.0 - 1.0/64.0, 1.0 - 33.0/64.0, 1.0 - 9.0/64.0, 1.0 - 41.0/64.0),
float[8](1.0 - 51.0/64.0, 1.0 - 19.0/64.0, 1.0 - 59.0/64.0, 1.0 - 27.0/64.0, 1.0 - 49.0/64.0, 1.0 - 17.0/64.0, 1.0 - 57.0/64.0, 1.0 - 25.0/64.0),
float[8](1.0 - 15.0/64.0, 1.0 - 47.0/64.0, 1.0 - 7.0/64.0, 1.0 - 39.0/64.0, 1.0 - 13.0/64.0, 1.0 - 45.0/64.0, 1.0 - 5.0/64.0, 1.0 - 37.0/64.0),
float[8](1.0 - 63.0/64.0, 1.0 - 31.0/64.0, 1.0 - 55.0/64.0, 1.0 - 23.0/64.0, 1.0 - 61.0/64.0, 1.0 - 29.0/64.0, 1.0 - 53.0/64.0, 1.0 - 21.0/64.0)
);


void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    /*if (color.a < 0.1) {
        discard;
    }*/
    //color.a = 0.5F;
    /*color.r = 0;
    color.g = 0;
    color.b = 0;*/
    vec2 test = vec2(0.0);
    //vec4 testReturn = testMethod();
    float test2 = test2();

    float rand = rand(texCoord0);

    // Calculate the position within the 8x8 matrix
    ivec2 pos = ivec2(mod(gl_FragCoord.xy, 8.0));

    // Fetch the corresponding dither value
    float ditherValue = ditherMatrix[pos.y][pos.x];
    float ditherValueInv = ditherMatrixInverted[pos.y][pos.x];

    //a bit of sky blue
    //color = mix(color, vec4(0.5, 0.65, 1, 1), 0.4);
    //mix in some of the fog, for sunset
    //color = mix(color, FogColor, 0.3);

    //fragColor = color;//linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    //fragColor = linear_fog(color, vertexDistance, 50, 512, FogColor);
    // Apply dithering based on the transparency level
    bool invert = vertexColor.a < 0;
    if (!invert) {
        if (vertexColor.a > ditherValue) {
            fragColor = linear_fog(color, vertexDistance, 250, 1024, FogColor);
        } else {
            discard;// Discard the fragment to create transparency
        }
    } else {
        //currently bugged
        if ((vertexColor.a + 1) > ditherValueInv) {
            fragColor = linear_fog(color, vertexDistance, 250, 1024, FogColor);
        } else {
            discard;  // Discard the fragment to create transparency
        }
    }
    //fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    //fragColor.w *= rand;
    //fragColor = testReturn;
    //vec4 test2 = mainImage(test);
    //fragColor = mainImageNew(fragColor, texCoord0);
}
