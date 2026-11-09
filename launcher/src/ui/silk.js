// The in-game "Silk" wallpaper shader, drawn behind the home page (WebGL2; a plain dark background without it).
(() => {
  const canvas = document.getElementById('silk');
  const gl = canvas.getContext('webgl2', { antialias: false, premultipliedAlpha: false });
  if (!gl) return;
  const vert = `#version 300 es
in vec2 pos; void main(){ gl_Position = vec4(pos, 0., 1.); }`;
  const frag = `#version 300 es
precision highp float;
uniform float t; uniform vec2 res; out vec4 color;
float h(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float n(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*f*(f*(f*6.-15.)+10.);
 return mix(mix(h(i),h(i+vec2(1.,0.)),f.x),mix(h(i+vec2(0.,1.)),h(i+vec2(1.,1.)),f.x),f.y);}
const mat2 m=mat2(1.6,-1.2,1.2,1.6);
float fbm(vec2 p){float v=0.,a=.5;for(int i=0;i<4;i++){v+=a*n(p);p=m*p+17.;a*=.5;}return v;}
void main(){
 vec2 uv=gl_FragCoord.xy/res; vec2 p=vec2(uv.x*res.x/res.y,uv.y)*1.05;
 vec2 q=vec2(fbm(p+vec2(0.,t*.04)),fbm(p+vec2(5.2,1.3-t*.03)));
 vec2 r=vec2(fbm(p+1.6*q+vec2(1.7+t*.02,9.2)),fbm(p+1.6*q+vec2(8.3,2.8-t*.025)));
 float f=fbm(p+1.8*r), fw=fwidth(f)+1e-5, k=res.y/360.;
 vec3 c=vec3(.010,.014,.026);
 c=mix(c,vec3(.08,.10,.16),smoothstep(.30,.58,f));
 c=mix(c,vec3(.56,.63,.76),smoothstep(.46,.76,f)*.9);
 c=mix(c,vec3(.80,.86,.95),smoothstep(.72,.95,f)*.6);
 float fade=smoothstep(.35,.75,fbm(p*.7+vec2(40.+t*.05,-13.)));
 float e=abs(f-.53)/fw/k, e2=abs(f-.40)/fw/k;
 float rim=(exp(-e*e*.3)+.18*exp(-abs(f-.53)*40.))*(.25+.75*fade)+.25*exp(-e2*e2*.35)*fade;
 c+=vec3(.22,.70,1.)*rim*.85;
 c*=1.-.55*pow(length(uv-.5)*1.25,2.);
 c+=(h(gl_FragCoord.xy+fract(t*7.)*91.)-.5)*.035;
 color=vec4(c,1.);}`;
  const shader = (type, src) => {
    const s = gl.createShader(type);
    gl.shaderSource(s, src);
    gl.compileShader(s);
    return s;
  };
  const prog = gl.createProgram();
  gl.attachShader(prog, shader(gl.VERTEX_SHADER, vert));
  gl.attachShader(prog, shader(gl.FRAGMENT_SHADER, frag));
  gl.linkProgram(prog);
  if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) return;
  gl.useProgram(prog);
  gl.bindBuffer(gl.ARRAY_BUFFER, gl.createBuffer());
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
  const pos = gl.getAttribLocation(prog, 'pos');
  gl.enableVertexAttribArray(pos);
  gl.vertexAttribPointer(pos, 2, gl.FLOAT, false, 0, 0);
  const uT = gl.getUniformLocation(prog, 't'), uRes = gl.getUniformLocation(prog, 'res');
  const start = performance.now();
  const frame = () => {
    // Render at most 1.25x CSS pixels: the shader is soft, full Retina resolution would only cost GPU time.
    const scale = Math.min(window.devicePixelRatio, 1.25);
    const w = Math.round(canvas.clientWidth * scale), h = Math.round(canvas.clientHeight * scale);
    if (canvas.width !== w || canvas.height !== h) {
      canvas.width = w;
      canvas.height = h;
      gl.viewport(0, 0, w, h);
    }
    if (!document.hidden && canvas.offsetParent !== null && canvas.closest('.page').classList.contains('active')) {
      gl.uniform1f(uT, 20 + (performance.now() - start) / 1000);
      gl.uniform2f(uRes, w, h);
      gl.drawArrays(gl.TRIANGLES, 0, 3);
    }
    requestAnimationFrame(frame);
  };
  requestAnimationFrame(frame);
})();
