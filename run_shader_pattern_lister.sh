#!/bin/bash

# Script to compile and run the ShaderPatternLister
# Uses Maven to get the classpath from the te-app module

echo "Compiling and running ShaderPatternLister..."
cd "$(dirname "$0")"

# First, ensure the project is compiled
echo "Building project..."
mvn compile -q

# Get the Maven classpath
echo "Getting classpath..."
CLASSPATH=$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout):te-app/target/classes:audio-stems/target/classes

# Compile our script
echo "Compiling ShaderPatternLister..."
javac -cp "$CLASSPATH" te-app/src/main/java/titanicsend/util/ShaderPatternLister.java

if [ $? -eq 0 ]; then
    echo "Running ShaderPatternLister..."
    echo ""
    java -cp "$CLASSPATH:te-app/src/main/java" titanicsend.util.ShaderPatternLister
else
    echo "Compilation failed!"
    exit 1
fi
