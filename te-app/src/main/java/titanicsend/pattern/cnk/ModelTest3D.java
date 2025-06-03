package titanicsend.pattern.cnk;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Examples")
public class ModelTest3D extends GLShaderPattern {

  public ModelTest3D(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.SPEED, 0.0, -4.0, 4.0);
    controls.setRange(TEControlTag.SIZE, 0.25, -1.0, 1.0);
    controls
        .setRange(TEControlTag.ANGLE, 0, 0, 3)
        .setUnits(TEControlTag.ANGLE, LXParameter.Units.INTEGER);
    controls.setRange(TEControlTag.YPOS, 0.5, 0.0, 1.0);

    // disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();

    addShader(
        "model_test_3d.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            s.setUniform(
                "iRotationAngle", (float) controls.getControl(TEControlTag.ANGLE).getValue());
          }
        });
  }
}
