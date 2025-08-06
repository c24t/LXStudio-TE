package titanicsend.util;

import heronarts.lx.LX;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.LXCategory;
import heronarts.lx.model.GridModel;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.glengine.TEAutoShaderPattern;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.glengine.TEShader;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import heronarts.lx.parameter.LXNormalizedParameter;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Script to list all registered shader patterns in the TE application. This uses the same machinery
 * as the main application to initialize patterns and then prints information about each registered
 * shader pattern.
 */
public class ShaderPatternLister {

  // Static mapping for the 5 specific controls we want to check
  private static final Map<TEControlTag, String> CONTROL_TO_UNIFORM =
      Map.of(
          TEControlTag.WOW1, "iWow1",
          TEControlTag.WOW2, "iWow2",
          TEControlTag.WOWTRIGGER, "iWowTrigger"
          // TEControlTag.LEVELREACTIVITY, "levelReact",
          // TEControlTag.FREQREACTIVITY, "frequencyReact"
          );

  // Mapping for controls that may be used in Java code without a direct reference
  private static final Map<TEControlTag, List<String>> CONTROL_TO_JAVA_TERMS =
      Map.of(
          TEControlTag.WOW1, List.of("getWow1"),
          TEControlTag.WOW2, List.of("getWow2"),
          TEControlTag.WOWTRIGGER, List.of("onWowTrigger", "getWowTrigger"));

  public static void main(String[] args) {
    try {
      System.out.println("Initializing LX Engine and TE Plugin...");

      // Initialize LX engine in headless mode
      LX lx = new LX(new GridModel(1, 1));

      // Initialize the TE plugin which registers all patterns
      heronarts.lx.studio.TEApp.Plugin plugin = new heronarts.lx.studio.TEApp.Plugin(lx);
      plugin.initialize(lx);

      System.out.println("\nRegistered Shader Patterns:");
      System.out.println("===========================");

      // Get all registered patterns
      List<Class<? extends LXPattern>> allPatterns = new ArrayList<>(lx.registry.patterns);

      // Filter for shader patterns and collect information
      List<ShaderPatternInfo> shaderPatterns = new ArrayList<>();

      for (Class<? extends LXPattern> patternClass : allPatterns) {
        if (isShaderPattern(patternClass)) {
          ShaderPatternInfo info = new ShaderPatternInfo();
          info.className = patternClass.getSimpleName();
          info.fullClassName = patternClass.getName();
          info.type = getShaderPatternType(patternClass);
          info.category = getPatternCategory(patternClass);
          info.patternClass = patternClass; // Store the class for later use
          info.unusedControls = getUnusedControls(lx, patternClass);
          // Get both the control analysis and shader file names in one call
          analyzePatternControls(lx, patternClass, info);
          shaderPatterns.add(info);
        }
      }

      // Sort by category then name
      shaderPatterns.sort(
          (a, b) -> {
            int categoryCompare = a.category.compareTo(b.category);
            if (categoryCompare != 0) return categoryCompare;
            return a.className.compareTo(b.className);
          });

      System.out.println("Patterns with unused but enabled controls:");
      System.out.println("==========================================");

      int issuesFound = 0;

      for (ShaderPatternInfo pattern : shaderPatterns) {
        // Only show patterns that have controls enabled but not used in shader
        if (!pattern.unusedButNotInShader.isEmpty()) {
          issuesFound++;

          // Get uniform names for the controls
          List<String> unusedUniforms = new ArrayList<>();
          for (String controlName : pattern.unusedButNotInShader) {
            TEControlTag tag = TEControlTag.valueOf(controlName);
            unusedUniforms.add(CONTROL_TO_UNIFORM.get(tag));
          }

          String shaderFiles =
              pattern.shaderFileNames.isEmpty()
                  ? "no-shader"
                  : String.join(", ", pattern.shaderFileNames);

          System.out.printf(
              "%s [%s] %s - unused, but not disabled: %s%n",
              pattern.className, pattern.category, shaderFiles, String.join(", ", unusedUniforms));

          // Show where to make the fixes
          if (pattern.type.equals("Auto")) {
            // Auto-generated pattern: fix in shader file
            String shaderFile =
                pattern.shaderFileNames.isEmpty()
                    ? "unknown.fs"
                    : pattern.shaderFileNames.iterator().next();
            System.out.printf("  Disable these controls in resources/shaders/%s as:%n", shaderFile);
          } else {
            // Manual or Constructed pattern: fix in Java class
            String javaFilePath = getJavaFilePath(pattern.patternClass);
            System.out.printf("  Disable these controls in %s as:%n", javaFilePath);

            // For inner classes, show the full class name and line info
            if (pattern.fullClassName.contains("$")) {
              String innerClassName =
                  pattern.fullClassName.substring(pattern.fullClassName.lastIndexOf(".") + 1);
              int lineNumber = findInnerClassLineNumber(javaFilePath, innerClassName);
              if (lineNumber > 0) {
                System.out.printf("  (%s defined on L%d)%n", innerClassName, lineNumber);
              } else {
                System.out.printf("  (%s defined in this file)%n", innerClassName);
              }
            }
          }

          // Show fix suggestions for each unused control
          for (String controlName : pattern.unusedButNotInShader) {
            TEControlTag tag = TEControlTag.valueOf(controlName);
            if (pattern.type.equals("Auto")) {
              // Auto-generated pattern: suggest correct #pragma syntax
              System.out.printf("    #pragma TEControl.%s.Disable%n", tag.name());
            } else {
              // Manual or Constructed pattern: suggest Java code
              System.out.printf(
                  "    controls.markUnused(controls.getLXControl(TEControlTag.%s));%n", tag.name());
            }
          }
          System.out.println();
        }
      }

      if (issuesFound == 0) {
        System.out.println("No patterns found with unused but enabled controls!");
      } else {
        System.out.println(
            "Found " + issuesFound + " patterns with controls that should be disabled.");
      }

      System.out.println();
      System.out.println("Summary:");
      System.out.println("--------");
      System.out.println("Total shader patterns found: " + shaderPatterns.size());

      // Count by type
      long autoCount = shaderPatterns.stream().filter(p -> p.type.equals("Auto")).count();
      long constructedCount =
          shaderPatterns.stream().filter(p -> p.type.equals("Constructed")).count();
      long manualCount = shaderPatterns.stream().filter(p -> p.type.equals("Manual")).count();

      System.out.println("  Auto-generated patterns: " + autoCount);
      System.out.println("  Constructed patterns: " + constructedCount);
      System.out.println("  Manual shader patterns: " + manualCount);

      // Properly shutdown LX engine threads before exit
      // The running field is private, but dispose() handles stopping the engine
      lx.dispose();

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static boolean isShaderPattern(Class<? extends LXPattern> patternClass) {
    return GLShaderPattern.class.isAssignableFrom(patternClass);
  }

  private static String getShaderPatternType(Class<? extends LXPattern> patternClass) {
    if (TEAutoShaderPattern.class.isAssignableFrom(patternClass)) {
      return "Auto";
    } else if (ConstructedShaderPattern.class.isAssignableFrom(patternClass)) {
      return "Constructed";
    } else if (GLShaderPattern.class.isAssignableFrom(patternClass)) {
      return "Manual";
    }
    return "Unknown";
  }

  private static String getPatternCategory(Class<? extends LXPattern> patternClass) {
    LXCategory categoryAnnotation = patternClass.getAnnotation(LXCategory.class);
    if (categoryAnnotation != null) {
      return categoryAnnotation.value();
    }
    return "Uncategorized";
  }

  private static Set<String> getUnusedControls(LX lx, Class<? extends LXPattern> patternClass) {
    Set<String> unusedControls = new HashSet<>();
    try {
      // Only check TEPerformancePattern subclasses as they have common controls
      if (TEPerformancePattern.class.isAssignableFrom(patternClass)) {
        // Create an instance of the pattern to inspect its controls
        LXPattern pattern = patternClass.getConstructor(LX.class).newInstance(lx);
        if (pattern instanceof TEPerformancePattern) {
          TEPerformancePattern tePattern = (TEPerformancePattern) pattern;

          // Access the unusedParams set from the controls field
          Set<LXNormalizedParameter> unusedParams = tePattern.getControls().unusedParams;

          // Map unused parameters back to their control tags
          for (TEControlTag tag : TEControlTag.values()) {
            try {
              LXNormalizedParameter param = tePattern.getControls().getLXControl(tag);
              if (unusedParams.contains(param)) {
                unusedControls.add(tag.name());
              }
            } catch (Exception e) {
              // Some patterns might not have all controls, skip
            }
          }
        }
        // Clean up the pattern instance
        pattern.dispose();
      }
    } catch (Exception e) {
      // Pattern instantiation failed, skip
      System.err.println(
          "Warning: Could not instantiate "
              + patternClass.getSimpleName()
              + " to check unused controls: "
              + e.getMessage());
    }
    return unusedControls;
  }

  /** Analyze pattern controls and shader files, populating the info object. */
  private static void analyzePatternControls(
      LX lx, Class<? extends LXPattern> patternClass, ShaderPatternInfo info) {
    // Only analyze shader patterns
    if (GLShaderPattern.class.isAssignableFrom(patternClass)) {
      try {
        // Create a temporary instance to analyze
        LXPattern tempPattern = patternClass.getConstructor(LX.class).newInstance(lx);

        if (tempPattern instanceof GLShaderPattern) {
          GLShaderPattern glPattern = (GLShaderPattern) tempPattern;

          // Get all shader files used by this pattern
          for (TEShader shader : glPattern.shaders) {
            String shaderFileName = shader.getShaderName();
            if (shaderFileName != null && !shaderFileName.isEmpty()) {
              info.shaderFileNames.add(shaderFileName);
            }
          }

          // Get unused controls for this pattern
          Set<String> unusedControls = info.unusedControls;

          // Check each of our 5 target controls
          for (Map.Entry<TEControlTag, String> entry : CONTROL_TO_UNIFORM.entrySet()) {
            TEControlTag controlTag = entry.getKey();
            String uniformName = entry.getValue();

            // Skip if this control is explicitly marked as unused
            if (unusedControls.contains(controlTag.name())) {
              continue;
            }

            // Check if the uniform is mentioned in any shader file
            boolean foundInShader = false;
            for (String shaderFileName : info.shaderFileNames) {
              String shaderPath =
                  "/Users/libc/src/LXStudio-TE/te-app/resources/shaders/" + shaderFileName;
              if (shaderFileContainsUniform(shaderPath, uniformName)) {
                foundInShader = true;
                break;
              }
            }

            // Check if the control is used in the Java pattern code
            boolean usedInJavaCode = false;
            if (!foundInShader) {
              usedInJavaCode = isControlUsedInJavaCode(patternClass, controlTag);
            }

            // Special checks for certain controls that might be used via getters or specific methods
            if (!usedInJavaCode && CONTROL_TO_JAVA_TERMS.containsKey(controlTag)) {
              String javaFilePath = getJavaFilePath(patternClass);
              for (String term : CONTROL_TO_JAVA_TERMS.get(controlTag)) {
                if (fileContainsText(javaFilePath, term)) {
                  usedInJavaCode = true;
                  break;
                }
              }
            }

            // Only report as unused if: not in shader AND not used in Java code
            if (!foundInShader && !usedInJavaCode) {
              info.unusedButNotInShader.add(controlTag.name());
            }
          }
        }

        // Clean up
        tempPattern.dispose();

      } catch (Exception e) {
        // If we can't instantiate the pattern, skip analysis
        System.err.println(
            "Warning: Could not analyze shader for "
                + patternClass.getSimpleName()
                + ": "
                + e.getMessage());
      }
    }
  }

  /** Check if a shader file contains a specific uniform name. */
  private static boolean shaderFileContainsUniform(String filePath, String uniformName) {
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(uniformName)) {
          return true;
        }
      }
    } catch (IOException e) {
      // If we can't read the file, assume uniform is not present
    }
    return false;
  }

  /** Get the actual source file path for a Java class, handling inner classes properly. */
  private static String getJavaFilePath(Class<? extends LXPattern> patternClass) {
    String className = patternClass.getName();

    // Handle inner classes - remove everything after the first $
    if (className.contains("$")) {
      className = className.substring(0, className.indexOf("$"));
    }

    // Convert package notation to file path
    String filePath = className.replace(".", "/");
    return "src/main/java/" + filePath + ".java";
  }

  /** Find the line number where an inner class is defined in a Java file. */
  private static int findInnerClassLineNumber(String javaFilePath, String innerClassName) {
    // Extract just the inner class name after the $
    String simpleInnerClassName =
        innerClassName.contains("$")
            ? innerClassName.substring(innerClassName.lastIndexOf("$") + 1)
            : innerClassName;

    try (BufferedReader reader = new BufferedReader(new FileReader(javaFilePath))) {
      String line;
      int lineNumber = 0;

      while ((line = reader.readLine()) != null) {
        lineNumber++;
        // Look for class definition patterns like "class InnerClassName" or "static class
        // InnerClassName"
        if (line.matches(".*\\b(static\\s+)?class\\s+" + simpleInnerClassName + "\\b.*")) {
          return lineNumber;
        }
      }
    } catch (IOException e) {
      // If we can't read the file, return -1
    }

    return -1;
  }

  /** Check if a control is used in the Java pattern code. */
  private static boolean isControlUsedInJavaCode(
      Class<? extends LXPattern> patternClass, TEControlTag controlTag) {
    String javaFilePath = getJavaFilePath(patternClass);
    String controlTagText = "TEControlTag." + controlTag.name();

    try {
      if (patternClass.getName().contains("$")) {
        // Inner class - need to find just the relevant class definition
        return isControlUsedInInnerClass(javaFilePath, patternClass.getName(), controlTagText);
      } else {
        // Regular class - search the whole file
        return fileContainsText(javaFilePath, controlTagText);
      }
    } catch (Exception e) {
      // If we can't read the file, assume it's not used
      return false;
    }
  }

  /** Check if control is used within a specific inner class definition. */
  private static boolean isControlUsedInInnerClass(
      String javaFilePath, String fullClassName, String controlTagText) {
    String innerClassName = fullClassName.substring(fullClassName.lastIndexOf("$") + 1);

    try (BufferedReader reader = new BufferedReader(new FileReader(javaFilePath))) {
      String line;
      boolean inTargetClass = false;
      int braceLevel = 0;
      int classStartBraceLevel = -1;

      while ((line = reader.readLine()) != null) {
        // Look for the class definition
        if (!inTargetClass
            && line.matches(".*\\b(static\\s+)?class\\s+" + innerClassName + "\\b.*")) {
          inTargetClass = true;
          classStartBraceLevel = braceLevel;
        }

        if (inTargetClass) {
          // Count braces to know when we exit the class
          for (char c : line.toCharArray()) {
            if (c == '{') braceLevel++;
            if (c == '}') braceLevel--;
          }

          // Check if the control is used in this line
          if (line.contains(controlTagText)) {
            return true;
          }

          // If we've closed all braces for this class, we're done
          if (braceLevel <= classStartBraceLevel) {
            break;
          }
        }
      }
    } catch (IOException e) {
      // If we can't read the file, assume it's not used
    }

    return false;
  }

  /** Check if a file contains specific text. */
  private static boolean fileContainsText(String filePath, String text) {
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(text)) {
          return true;
        }
      }
    } catch (IOException e) {
      // If we can't read the file, assume text is not present
    }
    return false;
  }

  private static class ShaderPatternInfo {
    String className;
    String fullClassName;
    String type;
    String category;
    Class<? extends LXPattern> patternClass;
    Set<String> unusedControls = new HashSet<>();
    Set<String> unusedButNotInShader = new HashSet<>();
    Set<String> shaderFileNames = new HashSet<>();
  }
}
