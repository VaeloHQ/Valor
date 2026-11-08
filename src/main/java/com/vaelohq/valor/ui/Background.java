package com.vaelohq.valor.ui;

import com.vaelohq.valor.Valor;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/** Animated menu background: soft blue waves and twinkling stars in one fragment shader. Falls back to a flat colour without GL 2.0. */
final class Background {
    private static final String FRAG = "#version 120\n"
            + "uniform float t; uniform vec2 res;\n"
            + "float h(vec2 p){return fract(sin(dot(p,vec2(12.9898,78.233)))*43758.5453);}\n"
            + "void main(){\n"
            + " vec2 uv=gl_FragCoord.xy/res; float ar=res.x/res.y; vec2 p=vec2(uv.x*ar,uv.y);\n"
            + " vec3 c=mix(vec3(.012,.018,.055),vec3(.03,.045,.13),uv.y);\n"
            + " float w=0.;\n"
            + " for(int i=0;i<12;i++){float f=float(i); float side=i<6?f*.035+.02:ar-.02-(f-6.)*.035;\n"
            + "  float x=side+.07*sin(p.y*2.2+t*.35+f*1.3)+.025*sin(p.y*5.3-t*.6+f*2.1);\n"
            + "  float d=abs(p.x-x); w+=exp(-d*d*2600.)*.05+exp(-d*d*90000.)*.10;}\n"
            + " c+=mix(vec3(.10,.22,.60),vec3(.55,.70,1.),.4)*w;\n"
            + " vec2 g=floor(p*42.), q=fract(p*42.)-.5; float r=h(g);\n"
            + " if(r>.985) c+=vec3(.6,.85,1.)*smoothstep(.13,0.,length(q))*(.4+.6*sin(t*1.6+r*90.));\n"
            + " c*=1.-.45*pow(length(uv-vec2(.5,.55)),1.6);\n"
            + " gl_FragColor=vec4(c,1.);}";
    private static int program = -1; // -1 = not built yet, 0 = unsupported
    private static final long START = System.nanoTime();

    static void draw(float w, float h) {
        if (program == -1) program = build();
        if (program == 0) {
            Ui.rect(0, 0, w, h, Ui.BG);
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GL20.glUseProgram(program);
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "t"), (System.nanoTime() - START) / 1e9f);
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "res"), mc.displayWidth, mc.displayHeight);
        Ui.rect(0, 0, w, h, 0xFFFFFFFF);
        GL20.glUseProgram(0);
    }

    static void release() {
        if (program > 0) GL20.glDeleteProgram(program);
        program = -1;
    }

    private static int build() {
        if (!GLContext.getCapabilities().OpenGL20) return 0;
        int p = GL20.glCreateProgram();
        int vs = shader(GL20.GL_VERTEX_SHADER, "void main(){gl_Position=ftransform();}"), fs = shader(GL20.GL_FRAGMENT_SHADER, FRAG);
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            Valor.LOGGER.error("Menu shader failed, using flat background: {}", GL20.glGetProgramInfoLog(p, 4096));
            GL20.glDeleteProgram(p);
            return 0;
        }
        return p;
    }

    private static int shader(int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        return s;
    }
}
