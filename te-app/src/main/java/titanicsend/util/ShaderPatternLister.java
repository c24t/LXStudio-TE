package titanicsend.util;

import heronarts.lx.LX;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.LXCategory;
import heronarts.lx.model.GridModel;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.glengine.TEAutoShaderPattern;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.glengine.TEShader;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Script to list all registered shader patterns in the TE application. This uses the same machinery
 * as the main application to initialize patterns and then prints information about each registered
 * shader pattern.
 */
public class ShaderPatternLister {

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
      Map<String, List<String>> shaderFileToPatterns = new HashMap<>();

      for (Class<? extends LXPattern> patternClass : allPatterns) {
        if (isShaderPattern(patternClass)) {
          ShaderPatternInfo info = new ShaderPatternInfo();
          info.className = patternClass.getSimpleName();
          info.fullClassName = patternClass.getName();
          info.type = getShaderPatternType(patternClass);
          info.category = getPatternCategory(patternClass);
          info.shaderFileNames = getShaderFileNames(lx, patternClass);
          shaderPatterns.add(info);

          for (String shaderFileName : info.shaderFileNames) {
            shaderFileToPatterns.computeIfAbsent(shaderFileName, k -> new ArrayList<>()).add(info.className);
          }
        }
      }

      // Sort by category then name
      shaderPatterns.sort(
          (a, b) -> {
            int categoryCompare = a.category.compareTo(b.category);
            if (categoryCompare != 0) return categoryCompare;
            return a.className.compareTo(b.className);
          });

      String lastCategory = "";
      for (ShaderPatternInfo pattern : shaderPatterns) {
        if (!pattern.category.equals(lastCategory)) {
          System.out.println("\nCategory: " + pattern.category);
          System.out.println("--------------------------");
          lastCategory = pattern.category;
        }
        String shaderFiles = String.join(", ", pattern.shaderFileNames);
        System.out.printf("  %-30s [%s] %s%n", pattern.className, pattern.type, shaderFiles);
      }

      System.out.println("Shader File Usage:");
      System.out.println("==================");

      File shaderDir = new File("/Users/libc/src/LXStudio-TE/te-app/resources/shaders");
      if (!shaderDir.isDirectory()) {
        System.out.println("Error: Shader directory not found at " + shaderDir.getAbsolutePath());
        return;
      }
      File[] shaderFiles = shaderDir.listFiles((dir, name) -> name.endsWith(".fs"));
      if (shaderFiles != null) {
        for (File shaderFile : shaderFiles) {
          String fileName = shaderFile.getName();
          List<String> patterns = shaderFileToPatterns.getOrDefault(fileName, Collections.emptyList());
          if (patterns.isEmpty()) {
            System.out.printf("  %-30s [Unused]%n", fileName);
          } else {
            System.out.printf("  %-30s %s%n", fileName, String.join(", ", patterns));
          }
        }
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

  private static Set<String> getShaderFileNames(LX lx, Class<? extends LXPattern> patternClass) {
    Set<String> shaderFileNames = new HashSet<>();
    try {
      LXPattern pattern = patternClass.getConstructor(LX.class).newInstance(lx);
      if (pattern instanceof GLShaderPattern) {
        GLShaderPattern glPattern = (GLShaderPattern) pattern;
        for (TEShader shader : glPattern.shaders) {
          String shaderFileName = shader.getShaderName();
          if (shaderFileName != null && !shaderFileName.isEmpty()) {
            shaderFileNames.add(shaderFileName);
          }
        }
      }
      pattern.dispose();
    } catch (Exception e) {
      // Could not instantiate, so we can't get shader names
    }
    return shaderFileNames;
  }

  private static class ShaderPatternInfo {
    String className;
    String fullClassName;
    String type;
    String category;
    Set<String> shaderFileNames = new HashSet<>();
  }
}
