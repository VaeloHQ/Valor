package com.vaelohq.valor.ui;

import com.vaelohq.valor.Config;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * The menu wallpaper: one of Valor's shaders ("silk" or "glow") or the player's own picture, GIF or video from
 * config/valor-wallpapers. Shaders fall back to a flat colour without GL 2.0; files fall back to "silk" until their
 * first frame is ready or when they can't be played.
 */
final class Background {
    static final String SILK = "silk", GLOW = "glow";

    private static final String NOISE = "#version 120\n"
            + "uniform float t; uniform vec2 res;\n"
            + "float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}\n";

    /** Drifting blurred blue light with a faint dot grid. */
    private static final String GLOW_FRAG = NOISE
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

    /**
     * Silver-blue smoke on near black with glowing cyan edges: domain-warped noise, with lines where the field crosses
     * two levels (kept a constant width on screen with fwidth), fading in and out, plus vignette and film grain.
     */
    private static final String SILK_FRAG = NOISE
            + "float n(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*f*(f*(f*6.-15.)+10.);"
            + "return mix(mix(h(i),h(i+vec2(1.,0.)),f.x),mix(h(i+vec2(0.,1.)),h(i+vec2(1.,1.)),f.x),f.y);}\n"
            + "const mat2 m=mat2(1.6,-1.2,1.2,1.6);\n"
            + "float fbm(vec2 p){float v=0.,a=.5;for(int i=0;i<4;i++){v+=a*n(p);p=m*p+17.;a*=.5;}return v;}\n"
            + "void main(){\n"
            + " vec2 uv=gl_FragCoord.xy/res; vec2 p=vec2(uv.x*res.x/res.y,uv.y)*1.05;\n"
            + " vec2 q=vec2(fbm(p+vec2(0.,t*.04)),fbm(p+vec2(5.2,1.3-t*.03)));\n"
            + " vec2 r=vec2(fbm(p+1.6*q+vec2(1.7+t*.02,9.2)),fbm(p+1.6*q+vec2(8.3,2.8-t*.025)));\n"
            + " float f=fbm(p+1.8*r), fw=fwidth(f)+1e-5, k=res.y/360.;\n"
            + " vec3 c=vec3(.010,.014,.026);\n"
            + " c=mix(c,vec3(.08,.10,.16),smoothstep(.30,.58,f));\n"
            + " c=mix(c,vec3(.56,.63,.76),smoothstep(.46,.76,f)*.9);\n"
            + " c=mix(c,vec3(.80,.86,.95),smoothstep(.72,.95,f)*.6);\n"
            + " float fade=smoothstep(.35,.75,fbm(p*.7+vec2(40.+t*.05,-13.)));\n"
            + " float e=abs(f-.53)/fw/k, e2=abs(f-.40)/fw/k;\n"
            + " float rim=(exp(-e*e*.3)+.18*exp(-abs(f-.53)*40.))*(.25+.75*fade)+.25*exp(-e2*e2*.35)*fade;\n"
            + " c+=vec3(.22,.70,1.)*rim*.85;\n"
            + " c*=1.-.55*pow(length(uv-.5)*1.25,2.);\n"
            + " c+=(h(gl_FragCoord.xy+fract(t*7.)*91.)-.5)*.035;\n"
            + " gl_FragColor=vec4(c,1.);}";

    private static final long START = System.nanoTime();
    private static final Map<String, Integer> programs = new HashMap<>();
    private static WallpaperMedia media;

    /** The folder players put their own wallpapers in. */
    static File folder() {
        File dir = new File(Loader.instance().getConfigDir(), "valor-wallpapers");
        if (!dir.isDirectory()) dir.mkdirs();
        return dir;
    }

    static boolean isShader(String name) {
        return SILK.equals(name) || GLOW.equals(name);
    }

    static void draw(float w, float h) {
        String which = Config.wallpaper;
        if (!isShader(which)) {
            File file = new File(folder(), which);
            if (media == null || !media.file.equals(file)) {
                if (media != null) media.close();
                media = file.isFile() ? new WallpaperMedia(file) : null;
            }
            if (media != null && !media.failed() && media.draw(w, h)) return;
            which = SILK;
        }
        shader(which, w, h);
    }

    /** Draws a shader over (0, 0, w, h), laid out for the whole window. */
    static void shader(String name, float w, float h) {
        Minecraft mc = Minecraft.getMinecraft();
        shader(name, w, h, mc.displayWidth, mc.displayHeight);
    }

    /** Draws a shader over (0, 0, w, h) for a render target of resW x resH pixels (the window, or a preview buffer). */
    static void shader(String name, float w, float h, int resW, int resH) {
        Integer program = programs.get(name);
        if (program == null) {
            program = Shapes.compile("void main(){gl_Position=ftransform();}", GLOW.equals(name) ? GLOW_FRAG : SILK_FRAG, "Background " + name);
            programs.put(name, program);
        }
        if (program == 0) {
            Ui.rect(0, 0, w, h, 0xFF070C1E);
            return;
        }
        GL20.glUseProgram(program);
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "t"), 20 + (System.nanoTime() - START) / 1e9f);
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "res"), resW, resH);
        Ui.rect(0, 0, w, h, 0xFFFFFFFF);
        GL20.glUseProgram(0);
    }

    static void release() {
        for (int p : programs.values()) if (p > 0) GL20.glDeleteProgram(p);
        programs.clear();
        if (media != null) media.close();
        media = null;
    }
}
