@echo off
REM Compile and run without VS Code (needs JDK 17+ on PATH)
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d bin @sources.txt
del sources.txt
java -cp "bin;lib/*" com.ems.Main
