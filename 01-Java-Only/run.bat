@echo off
setlocal
cd /d "%~dp0"

if not exist out mkdir out

echo [Java-Only] Compiling...
javac -d out src\Main.java
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

echo [Java-Only] Running...
java -cp out Main
