package titanicsend.util;

import heronarts.lx.LX;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.model.GridModel;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon.SpiralDiamonds;
import heronarts.lx.parameter.LXNormalizedParameter;
import java.util.Set;

/**
 * Debug script to inspect SpiralDiamonds pattern's unused controls
 */
public class SpiralDiamondsDebug {
    
    public static void main(String[] args) {
        try {
            System.out.println("Initializing LX Engine...");
            
            // Initialize LX engine in headless mode
            LX lx = new LX(new GridModel(1, 1));
            
            // Initialize the TE plugin which sets up the TE model
            System.out.println("Initializing TE Plugin...");
            heronarts.lx.studio.TEApp.Plugin plugin = new heronarts.lx.studio.TEApp.Plugin(lx);
            plugin.initialize(lx);
            
            System.out.println("Checking if TEApp.wholeModel is initialized...");
            System.out.println("TEApp.wholeModel: " + (heronarts.lx.studio.TEApp.wholeModel != null ? "initialized" : "null"));
            
            System.out.println("Creating SpiralDiamonds pattern...");
            SpiralDiamonds pattern = new SpiralDiamonds(lx);
            
            System.out.println("Inspecting unused controls...");
            Set<LXNormalizedParameter> unusedParams = pattern.getControls().unusedParams;
            
            System.out.println("Number of unused parameters: " + unusedParams.size());
            
            // Check each control tag
            for (TEControlTag tag : TEControlTag.values()) {
                try {
                    LXNormalizedParameter param = pattern.getControls().getLXControl(tag);
                    boolean isUnused = unusedParams.contains(param);
                    System.out.println(tag.name() + ": " + (isUnused ? "UNUSED" : "used"));
                } catch (Exception e) {
                    System.out.println(tag.name() + ": ERROR - " + e.getMessage());
                }
            }
            
            // Clean up
            pattern.dispose();
            lx.dispose();
            Runtime.getRuntime().halt(0);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            Runtime.getRuntime().halt(1);
        }
    }
}