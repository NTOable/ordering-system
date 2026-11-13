#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

echo "============================================"
echo " POLYGLOT RESTAURANT ORDERING SYSTEM"
echo "============================================"

for tool in javac java g++; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "[error] $tool was not found on PATH." >&2
        exit 1
    fi
done

if command -v python3 >/dev/null 2>&1; then
    PYTHON=python3
elif command -v python >/dev/null 2>&1; then
    PYTHON=python
else
    echo "[error] No working Python 3 interpreter found." >&2
    exit 1
fi

mkdir -p java/out

echo "[1/4] Compiling Java frontend..."
javac -d java/out java/src/StateBridge.java java/src/Main.java

echo "[2/4] Compiling C++ receipt engine..."
g++ -std=c++11 cpp/receipt.cpp -o receipt

echo "[3/4] Running Java CLI..."
java -cp java/out Main

echo "[4/4] Running Python engine then C++ renderer..."
echo "Using Python: $PYTHON"
"$PYTHON" python/engine.py

if ./receipt; then
    :
else
    echo "[warn] No receipt was rendered. Place an order before the pipeline runs." >&2
fi

echo
echo "Done."
