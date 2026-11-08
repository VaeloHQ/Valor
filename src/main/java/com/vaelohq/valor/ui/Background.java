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
            + " vec3 c=mix(vec3(.016,.024,.07),vec3(.03,.05,.16),uv.y*.8);\n"
            + " float w=0.;\n"
            + " for(int i=0;i<10;i++){float f=float(i), side=i<5?f*.045+.05:ar-.05-(f-5.)*.045;\n"
            + "  float x=side+.06*sin(p.y*3.+t*.5+f*.9)+.02*sin(p.y*7.-t*.8+f);\n"
            + "  float d=abs(p.x-x); w+=smoothstep(.035,0.,d)*.10+smoothstep(.004,0.,d)*.35;}\n"
            + " c+=vec3(.04,.30,1.)*w*.45;\n"
            + " vec2 g=floor(p*45.), q=fract(p*45.)-.5; float r=h(g);\n"
            + " if(r>.986) c+=vec3(.55,.85,1.)*smoothstep(.12,0.,length(q))*(.45+.55*sin(t*1.7+r*80.));\n"
            + " c*=1.-.35*length(uv-.5);\n"
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
