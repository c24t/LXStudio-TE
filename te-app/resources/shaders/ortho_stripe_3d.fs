#pragma Name("OrthoStripe3D")

const float SMOOTH_EDGES = .125;

/**
 * @brief Generates a centered, smooth Pulse Width Modulation (PWM) signal.
 *
 * This function produces a smooth PWM waveform based on a normalized input `x`
 * (typically time or phase) in [0, 1] and a `duty` cycle. The pulse is centered
 * at x = 0.5. The duty cycle (in [0, 1]) determines the width of the pulse.
 * At 0, the signal is always low, at 1, the signal is high on the interval [0,
 * 1].
 *
 * @param x in: Normalized time/phase in [0, 1].
 * @param duty in: Duty cycle, in [0, 1]. Effective width of the pulse.
 * @param margin in: Feather width of step edges in [0, .25].
 * @return: Smooth PWM signal value, in [0, 1].
 */
float centeredPwm(in float x, in float duty, in float margin) {
    if (duty <= 0.) return 0.;
    // duty = min(duty, 1.);

    float halfDuty = duty * 0.5;
    margin = min(margin, halfDuty);
    float outerMargin = margin * duty;
    float innerMargin = margin - outerMargin;
    float x0 = .5 - halfDuty - outerMargin;  // start of rising edge
    float x1 = .5 - halfDuty + innerMargin;  // end of rising edge
    float x2 = .5 + halfDuty - innerMargin;  // start of falling edge
    float x3 = .5 + halfDuty + outerMargin;  // end of falling edge

    return smoothstep(x0, x1, x) - smoothstep(x2, x3, x);
}

float xMod(in float x, in float period, in float offset) {
  return fract(x / period + offset + .5);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
  vec3 p = texelFetch(lxModelCoords, ivec2(gl_FragCoord.xy), 0).xyz;

  // if iRotationAngle is:
  // - 0, use p.x (fore to aft)
  // - 1, use p.y (top to bottom)
  // - 2, use p.z (port to starboard)
  float axis = p[clamp(int(round(iRotationAngle)), 0, 2)];

  // float period = max(1. - iQuantity, 1e-5);

  float period = .984375 * -log2(iQuantity + 1) + 1;

  float offset = -iTranslate.x;
  float inPlane = centeredPwm(xMod(axis, period, offset), iScale, iTranslate.y);
  fragColor = vec4(inPlane * iColorRGB, inPlane);
}
