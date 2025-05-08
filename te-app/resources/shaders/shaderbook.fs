// Example shader from The Book of Shaders adapted to use TE uniforms
// https://thebookofshaders.com/02/
//
// #ifdef GL_ES
// precision mediump float;
// #endif
//
// uniform vec2 u_resolution;
// uniform vec2 u_mouse;
// uniform float u_time;
//
// void main() {
//     vec2 st = gl_FragCoord.xy/u_resolution.xy;
//     st.x *= u_resolution.x/u_resolution.y;
//
//     vec3 color = vec3(0.);
//     color = vec3(st.x,st.y,abs(sin(u_time)));
//
//     gl_FragColor = vec4(color,1.0);
// }_

#pragma auto

void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 st = fragCoord.xy / iResolution.xy;
    st.x *= iResolution.x / iResolution.y;

    vec3 color = vec3(0.);
    color = vec3(st.x, st.y, abs(sin(iTime)));

    fragColor = vec4(color, 1.0);
}