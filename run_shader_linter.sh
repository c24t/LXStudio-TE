#!/bin/bash

# Script to compile and run the ShaderPatternControlLinter
# Uses Maven to get the classpath from the te-app module

echo "Compiling and running ShaderPatternControlLinter..."
cd "$(dirname "$0")"

# First, ensure the project is compiled
echo "Building project..."
mvn compile -q

# Get the Maven classpath
echo "Getting classpath..."
CLASSPATH=$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout):te-app/target/classes:audio-stems/target/classes

# Compile our script
echo "Compiling ShaderPatternControlLinter..."
javac -cp "$CLASSPATH" ShaderPatternControlLinter.java

if [ $? -eq 0 ]; then
    echo "Running ShaderPatternControlLinter..."
    echo ""
    java -cp "$CLASSPATH:." ShaderPatternControlLinter
else
    echo "Compilation failed!"
    exit 1
fi