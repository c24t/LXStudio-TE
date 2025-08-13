# TE Shader Pattern Registration Analysis

## Overview

This analysis examines how shader patterns are registered in the Titanic's End (TE) application, which is built on top of the Chromatik/LX framework. The application uses three different approaches to register shader patterns.

## How Shader Registration Works

The shader registration happens in the `TEApp.Plugin.initialize()` method through three mechanisms:

### 1. Manual Registration (`lx.registry.addPattern()`)
Hand-written Java classes that directly extend `GLShaderPattern` are registered manually:
```java
lx.registry.addPattern(ArcEdges.class);
lx.registry.addPattern(Electric.class);
// ... more manual registrations
```

### 2. Auto-generated Patterns (`ShaderPatternClassFactory.registerShaders()`)
Shader files (`.fs`) with `#pragma` directives are automatically discovered and converted into pattern classes:
```java
ShaderPatternClassFactory spf = new ShaderPatternClassFactory();
spf.registerShaders(lx);
```

### 3. Constructed Pattern Configs (`lx.registry.addPatterns()`)
Pattern configurations defined in Java config classes are bulk-registered:
```java
lx.registry.addPatterns(patternGetter.apply(OrganicPatternConfig.class));
lx.registry.addPatterns(patternGetter.apply(ShaderPanelsPatternConfig.class));
lx.registry.addPatterns(patternGetter.apply(ShaderEdgesPatternConfig.class));
```

## Pattern Types Found

### Auto-generated Patterns (13 patterns)
These patterns are automatically generated from `.fs` shader files that contain `#pragma` directives:

**Examples:**
- `artofcode.fs` → ArtOfCode pattern
- `techno_church.fs` → TechnoChurch pattern
- `starfield1.fs` → starfield1 pattern

**Key Features:**
- Use `#pragma name "PatternName"` to set display name
- Use `#pragma LXCategory("Category")` to set UI category
- Use `#pragma TEControl.*` to configure controls
- Automatically wrapped in `TEAutoShaderPattern` or `TEAutoDriftPattern`

### Constructed Patterns (33 patterns)
These patterns are defined in Java configuration classes and use the `ConstructedShaderPattern` base class:

**Configuration Files:**
- `ShaderPanelsPatternConfig.java` - Panel-focused shader patterns
- `ShaderEdgesPatternConfig.java` - Edge-focused shader patterns  
- `OrganicPatternConfig.java` - Organic/experimental patterns

**Example:**
```java
@LXCategory("Native Shaders Panels")
public static class Galaxy extends ConstructedShaderPattern {
    public Galaxy(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);
    }
    
    @Override
    protected void createShader() {
        controls.setRange(TEControlTag.SPEED, 0, -4, 4);
        addShader("galaxy.fs");
    }
}
```

### Manual Shader Patterns (23 patterns)
These are hand-written Java classes that directly extend `GLShaderPattern`:

**Examples:**
- `ArcEdges.java` - Creates electrical arcs between vehicle edges
- `Fireflies.java` - Animated firefly effect with custom uniforms
- `Phasers.java` - Uses car geometry data for unique effects

**Key Features:**
- Full control over shader setup and frame-time calculations
- Can send custom uniforms to shaders
- Can use multiple shaders in sequence (multipass rendering)
- Can access car geometry data

## Key Components

### ShaderPatternClassFactory
- Uses ByteBuddy library for runtime class generation
- Scans `resources/shaders/*.fs` files for `#pragma` directives
- Creates classes that extend `TEAutoShaderPattern` or `TEAutoDriftPattern`
- Handles automatic control configuration

### GLShaderPattern Base Class
- Wrapper for OpenGL shader management
- Provides framework for shader setup and frame-time updates
- Handles common controls and parameter management
- Manages shader lifecycle and memory

### TEAutoShaderPattern
- Simplified base class for auto-generated patterns
- Automatically configures controls based on shader `#pragma` directives
- Handles single-shader patterns with minimal Java code

## Categories Found

The analysis found 69 shader patterns across 13 categories:

1. **Auto Shader** (12) - Auto-generated patterns
2. **Native Shaders Panels** (25) - Panel-focused effects
3. **Native Shaders Edges** (4) - Edge-focused effects  
4. **Combo FG** (6) - Patterns affecting both panels and edges
5. **DREVO Shaders** (6) - Specific shader collection
6. **Look Shader Patterns** (5) - Audio-reactive look patterns
7. **Mothership** (3) - Mothership vehicle specific
8. **Noise** (2) - Noise-based patterns
9. **Panel FG** (1) - Panel foreground effects
10. **TE Examples** (1) - Teaching examples
11. **Utility** (2) - Development/debugging tools
12. **Bogus Test Pattern** (2) - Test patterns
13. **Native Shaders** (1) - Base shader class

## Running the Analysis

Two scripts are provided:

1. **ShaderPatternControlLinter.java** - Uses full LX engine initialization (requires working build)
2. **SimpleShaderLister.java** - Analyzes source code directly (works without compilation)

To run the working script:
```bash
javac SimpleShaderLister.java
java SimpleShaderLister
```

## Conclusion

The TE application uses a sophisticated three-tier shader pattern registration system that supports:

1. **Rapid prototyping** via auto-generated patterns from `.fs` files
2. **Structured development** via constructed pattern configurations  
3. **Advanced features** via manual GLShaderPattern implementations

This architecture allows developers to quickly create simple shader patterns while still providing the flexibility for complex, interactive effects that integrate with the vehicle's geometry and audio systems.