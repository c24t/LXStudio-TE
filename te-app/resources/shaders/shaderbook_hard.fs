void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 st = fragCoord.xy / iResolution.xy;
    st.x *= iResolution.x / iResolution.y;

    vec3 color = vec3(0.);
    color = vec3(st.x, st.y, abs(sin(iTime)));

    fragColor = vec4(color, 1.0);
}
