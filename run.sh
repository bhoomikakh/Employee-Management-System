#!/bin/bash
# Compile and run without VS Code (needs JDK 17+). Linux / macOS
cd "$(dirname "$0")"
mkdir -p bin
javac -encoding UTF-8 -cp "lib/*" -d bin $(find src -name "*.java")
java -cp "bin:lib/*" com.ems.Main
