package titanicsend.pattern.cnk;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class ShaderBookHard extends GLShaderPattern {

  public ShaderBookHard(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // register common controls with the UI
    addCommonControls();

    // add the OpenGL shader and its frame-time setup function
    addShader(
        "shaderbook_hard.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {}
        });
  }
}
