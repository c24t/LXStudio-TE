package titanicsend.pattern.cnk;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Examples")
public class OrthoStripe3D extends GLShaderPattern {

  public OrthoStripe3D(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.SPEED, 0.0, -4.0, 4.0);
    controls.setRange(TEControlTag.SIZE, 0.25, 0.0, 1.0);
    controls
        .setRange(TEControlTag.ANGLE, 0, 0, 3)
        .setUnits(TEControlTag.ANGLE, LXParameter.Units.INTEGER);
    controls.setRange(TEControlTag.YPOS, 0.125, 0.0, 0.25);

    // disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.SPEED));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();

    addShader(
        "ortho_stripe_3d.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            s.setUniform(
                "iRotationAngle", (float) controls.getControl(TEControlTag.ANGLE).getValue());
          }
        });
  }
}
