@echo off
setlocal
cd /d "%~dp0"

echo ============================================
echo  POLYGLOT RESTAURANT ORDERING SYSTEM
echo ============================================

where javac >nul 2>nul
if errorlevel 1 (
    echo [error] javac was not found. Install a JDK and add its bin folder to PATH.
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo [error] java was not found. Install a JDK and add its bin folder to PATH.
    exit /b 1
)

where g++ >nul 2>nul
if errorlevel 1 (
    echo [error] g++ was not found. Install MinGW-w64 and add its bin folder to PATH.
    exit /b 1
)

rem Prefer the "py" launcher, then fall back to "python". The check runs the
rem interpreter so a Microsoft Store placeholder alias does not count as Python.
set "PYTHON="
py -3 --version >nul 2>nul && set "PYTHON=py -3"
if not defined PYTHON (
    python --version >nul 2>nul && set "PYTHON=python"
)
if not defined PYTHON (
    echo [error] No working Python 3 interpreter found. Install Python 3 or enable the "py" launcher.
    exit /b 1
)

if not exist java\out mkdir java\out

echo [1/4] Compiling Java frontend...
javac -d java\out java\src\StateBridge.java java\src\Main.java
if errorlevel 1 (
    echo [error] Java compilation failed.
    exit /b 1
)

echo [2/4] Compiling C++ receipt engine...
g++ -std=c++11 cpp\receipt.cpp -o receipt.exe
if errorlevel 1 (
    echo [error] C++ compilation failed.
    exit /b 1
)

echo [3/4] Running Java CLI...
java -cp java\out Main
if errorlevel 1 (
    echo [error] Java runtime failed.
    exit /b 1
)

echo [4/4] Running Python engine then C++ renderer...
echo Using Python: %PYTHON%
%PYTHON% python\engine.py
if errorlevel 1 (
    echo [error] Python stage failed.
    exit /b 1
)

receipt.exe
if errorlevel 1 (
    echo [warn] No receipt was rendered. Place an order before the pipeline runs.
)

echo.
echo Done.
