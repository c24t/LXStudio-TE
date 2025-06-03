#pragma Name("ModelTest3D")

const float SMOOTH_EDGES = .125;

float pwm(in float x, in float duty) { 
  if (duty == 0.) return 0.;
  if (duty <= -1. || duty >= 1.) return 1.;

  x = fract(x);  // just in case

  float cutoff = duty > 0. ? 1. - duty : abs(duty);

  // for hard edges
  // float sv = step(cutoff, x);

  float feather = abs(duty) * SMOOTH_EDGES * iTranslate.y;
  float sv = duty > 0. ? 
      min(
        smoothstep(cutoff - feather * .5, cutoff + feather * .5, x),
        smoothstep(1., 1. - feather * .5, x)) :
      max(
        smoothstep(cutoff - feather * .5, cutoff + feather * .5, x),
        smoothstep(feather * .5, 0., x))
      ;
  return duty > 0. ? sv : 1. - sv;
}

float xMod(in float x, in float period, in float phase) {
  return fract(x / period + phase);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
  vec3 p = texelFetch(lxModelCoords, ivec2(gl_FragCoord.xy), 0).xyz;
  float period = 1. - iQuantity;
  float axis = (iRotationAngle < 1.) ? p.x :  // fore to aft
               (iRotationAngle < 2.) ? p.y :  // top to bottom
                                        p.z;   // port to starboard

  float inPlane = pwm(xMod(axis, period, iTranslate.x), iScale);
  vec3 rgb = vec3(0.) + inPlane * iColorRGB;
  fragColor = vec4(rgb, 1.);
}
