import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Modifier;

/**
 * Simple script to list shader patterns by analyzing the source code directly.
 * This approach doesn't require the full LX engine to be running.
 */
public class SimpleShaderLister {
    
    public static void main(String[] args) {
        try {
            System.out.println("Analyzing Shader Patterns in TE Application");
            System.out.println("==========================================");
            
            List<ShaderPatternInfo> patterns = new ArrayList<>();
            
            // Find auto-generated shader patterns by scanning shader files
            patterns.addAll(findAutoShaderPatterns());
            
            // Find constructed shader patterns by scanning Java config files  
            patterns.addAll(findConstructedShaderPatterns());
            
            // Find manual shader patterns by scanning Java pattern files
            patterns.addAll(findManualShaderPatterns());
            
            // Sort and display results
            Collections.sort(patterns, (a, b) -> {
                int categoryCompare = a.category.compareTo(b.category);
                if (categoryCompare != 0) return categoryCompare;
                return a.name.compareTo(b.name);
            });
            
            displayResults(patterns);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static List<ShaderPatternInfo> findAutoShaderPatterns() {
        List<ShaderPatternInfo> patterns = new ArrayList<>();
        File shaderDir = new File("te-app/resources/shaders");
        
        if (!shaderDir.exists()) {
            System.err.println("Shader directory not found: " + shaderDir.getAbsolutePath());
            return patterns;
        }
        
        File[] shaderFiles = shaderDir.listFiles((dir, name) -> name.endsWith(".fs"));
        if (shaderFiles == null) return patterns;
        
        for (File shaderFile : shaderFiles) {
            try {
                String content = new String(Files.readAllBytes(shaderFile.toPath()));
                
                // Check for auto shader pragmas
                if (content.contains("#pragma auto") || content.contains("#pragma name") || 
                    content.contains("#pragma Name") || content.contains("#pragma LXCategory")) {
                    
                    ShaderPatternInfo info = new ShaderPatternInfo();
                    info.type = "Auto";
                    info.shaderFile = shaderFile.getName();
                    
                    // Extract name from pragma or use filename
                    Pattern namePattern = Pattern.compile("#pragma\\s+[nN]ame\\(\"([^\"]+)\"\\)");
                    Matcher nameMatcher = namePattern.matcher(content);
                    if (nameMatcher.find()) {
                        info.name = nameMatcher.group(1);
                    } else {
                        // Use filename without extension
                        info.name = shaderFile.getName().replaceAll("\\.fs$", "");
                    }
                    
                    // Extract category
                    Pattern categoryPattern = Pattern.compile("#pragma\\s+LXCategory\\(\"([^\"]+)\"\\)");
                    Matcher categoryMatcher = categoryPattern.matcher(content);
                    if (categoryMatcher.find()) {
                        info.category = categoryMatcher.group(1);
                    } else {
                        info.category = "Auto Shader";
                    }
                    
                    patterns.add(info);
                }
            } catch (Exception e) {
                System.err.println("Error reading shader file " + shaderFile.getName() + ": " + e.getMessage());
            }
        }
        
        return patterns;
    }
    
    private static List<ShaderPatternInfo> findConstructedShaderPatterns() {
        List<ShaderPatternInfo> patterns = new ArrayList<>();
        
        // Check the main config files
        String[] configFiles = {
            "te-app/src/main/java/titanicsend/pattern/yoffa/config/ShaderPanelsPatternConfig.java",
            "te-app/src/main/java/titanicsend/pattern/yoffa/config/ShaderEdgesPatternConfig.java",
            "te-app/src/main/java/titanicsend/pattern/yoffa/config/OrganicPatternConfig.java"
        };
        
        for (String configFile : configFiles) {
            try {
                File file = new File(configFile);
                if (!file.exists()) continue;
                
                String content = new String(Files.readAllBytes(file.toPath()));
                patterns.addAll(parseConstructedPatterns(content));
                
            } catch (Exception e) {
                System.err.println("Error reading config file " + configFile + ": " + e.getMessage());
            }
        }
        
        return patterns;
    }
    
    private static List<ShaderPatternInfo> parseConstructedPatterns(String content) {
        List<ShaderPatternInfo> patterns = new ArrayList<>();
        
        // Find all public static class declarations that extend ConstructedShaderPattern
        Pattern classPattern = Pattern.compile(
            "@LXCategory\\(\"([^\"]+)\"\\)\\s*" +
            "public\\s+static\\s+class\\s+(\\w+)\\s+extends\\s+ConstructedShaderPattern", 
            Pattern.MULTILINE | Pattern.DOTALL
        );
        
        Matcher matcher = classPattern.matcher(content);
        while (matcher.find()) {
            ShaderPatternInfo info = new ShaderPatternInfo();
            info.type = "Constructed";
            info.category = matcher.group(1);
            info.name = matcher.group(2);
            patterns.add(info);
        }
        
        return patterns;
    }
    
    private static List<ShaderPatternInfo> findManualShaderPatterns() {
        List<ShaderPatternInfo> patterns = new ArrayList<>();
        
        // Search for GLShaderPattern extensions in pattern directories
        File patternDir = new File("te-app/src/main/java/titanicsend/pattern");
        if (!patternDir.exists()) return patterns;
        
        searchForShaderPatterns(patternDir, patterns);
        
        return patterns;
    }
    
    private static void searchForShaderPatterns(File dir, List<ShaderPatternInfo> patterns) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                searchForShaderPatterns(file, patterns);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    
                    // Look for classes extending GLShaderPattern but not ConstructedShaderPattern or TEAutoShaderPattern
                    if (content.contains("extends GLShaderPattern") && 
                        !content.contains("extends ConstructedShaderPattern") &&
                        !content.contains("extends TEAutoShaderPattern") &&
                        !content.contains("extends TEAutoDriftPattern")) {
                        
                        // Extract class name and category
                        Pattern classPattern = Pattern.compile("public\\s+class\\s+(\\w+)\\s+extends\\s+GLShaderPattern");
                        Matcher classMatcher = classPattern.matcher(content);
                        
                        if (classMatcher.find()) {
                            ShaderPatternInfo info = new ShaderPatternInfo();
                            info.type = "Manual";
                            info.name = classMatcher.group(1);
                            
                            // Extract category
                            Pattern categoryPattern = Pattern.compile("@LXCategory\\(\"([^\"]+)\"\\)");
                            Matcher categoryMatcher = categoryPattern.matcher(content);
                            if (categoryMatcher.find()) {
                                info.category = categoryMatcher.group(1);
                            } else {
                                info.category = "Uncategorized";
                            }
                            
                            patterns.add(info);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error reading Java file " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    private static void displayResults(List<ShaderPatternInfo> patterns) {
        String lastCategory = "";
        int totalCount = 0;
        
        for (ShaderPatternInfo pattern : patterns) {
            if (!pattern.category.equals(lastCategory)) {
                if (!lastCategory.isEmpty()) {
                    System.out.println();
                }
                System.out.println("Category: " + pattern.category);
                System.out.println("----------");
                lastCategory = pattern.category;
            }
            
            String displayInfo = String.format("  %-30s [%s]", pattern.name, pattern.type);
            if (pattern.shaderFile != null) {
                displayInfo += " -> " + pattern.shaderFile;
            }
            System.out.println(displayInfo);
            totalCount++;
        }
        
        System.out.println();
        System.out.println("Summary:");
        System.out.println("--------");
        System.out.println("Total shader patterns found: " + totalCount);
        
        // Count by type
        long autoCount = patterns.stream().filter(p -> p.type.equals("Auto")).count();
        long constructedCount = patterns.stream().filter(p -> p.type.equals("Constructed")).count();  
        long manualCount = patterns.stream().filter(p -> p.type.equals("Manual")).count();
        
        System.out.println("  Auto-generated patterns: " + autoCount);
        System.out.println("  Constructed patterns: " + constructedCount);
        System.out.println("  Manual shader patterns: " + manualCount);
        
        System.out.println();
        System.out.println("How shader registration works:");
        System.out.println("------------------------------");
        System.out.println("1. Auto patterns: Generated dynamically from .fs files with #pragma directives");
        System.out.println("2. Constructed patterns: Defined in Java config classes, use ConstructedShaderPattern");
        System.out.println("3. Manual patterns: Hand-written Java classes extending GLShaderPattern directly");
        System.out.println();
        System.out.println("All patterns are registered in TEApp.Plugin.initialize() via:");
        System.out.println("- lx.registry.addPattern() for manual patterns");
        System.out.println("- ShaderPatternClassFactory.registerShaders() for auto patterns");  
        System.out.println("- lx.registry.addPatterns() for constructed pattern configs");
    }
    
    private static class ShaderPatternInfo {
        String name;
        String type;
        String category;
        String shaderFile;
    }
}