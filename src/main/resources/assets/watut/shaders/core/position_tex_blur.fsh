#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform vec2 resolution;
uniform float radius;

in vec2 texCoord0;

out vec4 fragColor;

const float weight[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a == 0.0) {
        discard;
    }

    /*vec2 Resolution2 = vec2(256, 256);

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
    }*/

    float r = radius;
    float x,y,xx,yy,rr=r*r,dx,dy,w,w0;
    float xs = resolution[0];
    float ys = resolution[1];
    xs = 256;
    ys = 256;
    //xs = 176;
    //ys = 222;
    w0=0.3780/pow(r,1.975);
    vec2 p;
    vec2 pos = texCoord0;
    vec4 col=vec4(0.0,0.0,0.0,0.0);
    for (dx=1.0/xs, x=-r, p.x=(pos.x)+(x*dx); x<=r; x++, p.x+=dx) {
        xx=x*x;
        for (dy=1.0/ys, y=-r, p.y=(pos.y)+(y*dy); y<=r; y++, p.y+=dy) {
            yy=y*y;
            if (xx+yy<=rr) {
                w=w0*exp((-xx-yy)/(2.0*rr));
                col+=texture2D(Sampler0,p)*w;
            }
        }
    }

    if (r == 0) {
        col=texture(Sampler0, texCoord0);
    }

    //5 offsets for 10 pixel sampling!
    /*float offset[5] = float[](-4.0f, -2.0f, 0.0f, 2.0f, 4.0f);
    //int[5] weight = [1, 4, 6, 4, 1]; //sum = 16
    float weightInverse[5] = float[](0.0625f, 0.25f, 0.375, 0.25f, 0.0625f);

    vec4 finalColor = texture(Sampler0, texCoord0);

    for(int i = 0; i < 5; i++) {
        finalColor += texture2D(Sampler0, vec2(offset[i], 0.5f)) * weightInverse[i];
    }*/



    //gl_FragColor=col;

    //fragColor = vec4(result, 1.0) * ColorModulator;
    fragColor = col;
    //fragColor = finalColor;

    //color.r = 1;
    //fragColor = color * ColorModulator;
}
