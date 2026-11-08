package com.vaelohq.valor.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

/** Slowly drifting, blurred blue light with a faint dot grid. Falls back to a flat colour without GL 2.0. */
final class Background {
    private static final String FRAG = "#version 120\n"
            + "uniform float t; uniform vec2 res;\n"
            + "float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}\n"
            + "float n(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);"
            + "return mix(mix(h(i),h(i+vec2(1.,0.)),f.x),mix(h(i+vec2(0.,1.)),h(i+vec2(1.,1.)),f.x),f.y);}\n"
            + "float fbm(vec2 p){float v=0.,a=.5;for(int i=0;i<4;i++){v+=a*n(p);p=p*2.03+17.;a*=.5;}return v;}\n"
            + "void main(){\n"
            + " vec2 uv=gl_FragCoord.xy/res; vec2 p=vec2(uv.x*res.x/res.y,uv.y)*1.15;\n"
            + " vec2 q=vec2(fbm(p+t*.03),fbm(p+vec2(5.2,1.3)-t*.025));\n"
            + " float f=fbm(p+2.4*q+t*.02);\n"
            + " vec3 c=mix(vec3(.020,.035,.090),vec3(.045,.13,.38),smoothstep(.28,.62,f));\n"
            + " c=mix(c,vec3(.16,.42,.95),smoothstep(.50,.78,f));\n"
            + " c=mix(c,vec3(.62,.80,1.),smoothstep(.72,.95,f)*.65);\n"
            + " float s=res.y/27.; vec2 g=mod(gl_FragCoord.xy,s)-s*.5;\n"
            + " c+=vec3(.6,.75,1.)*.07*(1.-smoothstep(.5,1.3,length(g)));\n"
            + " c*=1.-.40*pow(length(uv-.5)*1.3,2.);\n"
            + " gl_FragColor=vec4(c,1.);}";
    private static final long START = System.nanoTime();
    private static int program = -1;

    static void draw(float w, float h) {
        if (program == -1) program = Shapes.compile("void main(){gl_Position=ftransform();}", FRAG, "Background");
        if (program == 0) {
            Ui.rect(0, 0, w, h, 0xFF070C1E);
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GL20.glUseProgram(program);
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "t"), 20 + (System.nanoTime() - START) / 1e9f);
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "res"), mc.displayWidth, mc.displayHeight);
        Ui.rect(0, 0, w, h, 0xFFFFFFFF);
        GL20.glUseProgram(0);
    }

    static void release() {
        if (program > 0) GL20.glDeleteProgram(program);
        program = -1;
    }
}
