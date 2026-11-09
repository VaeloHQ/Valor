package com.vaelohq.valor.ui;

import com.vaelohq.valor.Valor;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/**
 * Anti-aliased rounded rectangles with a vertical gradient fill, a border and a soft drop shadow,
 * drawn by one signed-distance-field shader. Without GL 2.0 it falls back to a plain rectangle.
 */
public final class Shapes {
    private static final String VERT = "#version 120\n"
            + "uniform vec2 center; varying vec2 p;\n"
            + "void main(){p=gl_Vertex.xy-center; gl_Position=ftransform();}";
    private static final String FRAG = "#version 120\n"
            + "uniform vec2 hs; uniform float radius, bw, blur, px; uniform vec4 fillA, fillB, border, shadow; uniform vec2 off;\n"
            + "varying vec2 p;\n"
            + "float sd(vec2 q){vec2 d=abs(q)-hs+radius; return length(max(d,0.))+min(max(d.x,d.y),0.)-radius;}\n"
            + "void main(){\n"
            + " float d=sd(p), a=clamp(.5-d/px,0.,1.), ib=clamp(.5-(d+bw)/px,0.,1.);\n"
            + " vec4 c=mix(border, mix(fillA,fillB,clamp(p.y/(hs.y*2.)+.5,0.,1.)), bw>0.?ib:1.); c.a*=a;\n"
            + " float s=blur>0.?shadow.a*(1.-smoothstep(-blur,blur,sd(p-off)))*(1.-a):0.;\n"
            + " float o=c.a+s*(1.-c.a);\n"
            + " gl_FragColor=vec4((c.rgb*c.a+shadow.rgb*s*(1.-c.a))/max(o,1e-4),o);}";
    /** One real screen pixel in GUI units; set each frame by ValorScreen. */
    static float px = 0.5f;
    private static int program = -1;

    /** Rounded rectangle with a flat fill. */
    public static void round(float x, float y, float w, float h, float r, int fill) {
        draw(x, y, w, h, r, fill, fill, 0, 0, 0, 0, 0);
    }

    /**
     * @param fillA  colour at the top, fillB at the bottom
     * @param bw     border width in GUI pixels (0 for none)
     * @param blur   shadow softness in GUI pixels (0 for none), shadowY its downward offset
     */
    public static void draw(float x, float y, float w, float h, float r, int fillA, int fillB, int border, float bw,
                            int shadow, float blur, float shadowY) {
        if (program == -1) program = build();
        if (program == 0) {
            Ui.rect(x, y, x + w, y + h, fillA);
            return;
        }
        float m = blur * 2 + shadowY;
        GL20.glUseProgram(program);
        GL20.glUniform2f(loc("center"), x + w / 2, y + h / 2);
        GL20.glUniform2f(loc("hs"), w / 2, h / 2);
        GL20.glUniform1f(loc("radius"), Math.min(r, Math.min(w, h) / 2));
        GL20.glUniform1f(loc("bw"), bw);
        GL20.glUniform1f(loc("blur"), blur);
        GL20.glUniform1f(loc("px"), px);
        GL20.glUniform2f(loc("off"), 0, shadowY);
        color4("fillA", fillA);
        color4("fillB", fillB);
        color4("border", border);
        color4("shadow", shadow);
        GlStateManager.disableAlpha();
        Ui.rect(x - m, y - m, x + w + m, y + h + m, 0xFFFFFFFF);
        GlStateManager.enableAlpha();
        GL20.glUseProgram(0);
    }

    private static int loc(String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    private static void color4(String name, int argb) {
        GL20.glUniform4f(loc(name), (argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f);
    }

    /** Compiles a vertex + fragment program; returns 0 when shaders are unavailable or fail. */
    static int compile(String vert, String frag, String what) {
        if (!GLContext.getCapabilities().OpenGL20) return 0;
        int p = GL20.glCreateProgram();
        int vs = shader(GL20.GL_VERTEX_SHADER, vert), fs = shader(GL20.GL_FRAGMENT_SHADER, frag);
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            Valor.LOGGER.error("{} shader failed, using fallback: {}", what, GL20.glGetProgramInfoLog(p, 4096));
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

    private static int build() {
        return compile(VERT, FRAG, "Shape");
    }
}
