# INSTRUCTIONS — How to Run the Ordering System

This guide covers both ways to run the project:

1. **One command** using the provided orchestrator scripts.
2. **Manual stage-by-stage**, running Java, then Python, then C++ yourself.

---

## 1. Prerequisites

Install these and make sure they are on your `PATH`:

| Tool | Check | Notes |
|---|---|---|
| Java JDK | `javac -version` and `java -version` | JDK 8 or newer |
| Python 3 | `python --version` or `py --version` | `python3` on Linux/macOS |
| g++ | `g++ --version` | MinGW-w64 on Windows |

---

## 2. Folder layout

```text
ordering_system/
├── 01-Java-Only/        # the Java frontend by itself
│   ├── src/Main.java
│   └── run.bat
│
├── 02-Full-Project/     # Java + Python + C++ pipeline
│   ├── java/src/Main.java
│   ├── java/src/StateBridge.java
│   ├── python/engine.py
│   ├── cpp/receipt.cpp
│   ├── simulation_state.json
│   ├── all_receipts.txt
│   ├── run.bat
│   └── run.sh
│
└── ...
```

All data files are opened **relative to the current working directory**, so run
each command from the folder named below.

---

## 3. Run everything with one command

### Windows

```bat
cd 02-Full-Project
run.bat
```

### Linux / macOS

```bash
cd 02-Full-Project
chmod +x run.sh
./run.sh
```

On Windows, the same script works from Git Bash (`C:\Program Files\Git\bin\bash.exe`),
run from inside `02-Full-Project`.

The script compiles Java and C++, runs the Java CLI, then runs Python, then
runs the C++ receipt renderer. It also auto-detects a working Python 3
interpreter (`py` first, then `python`) and prints which one it used.

---

## 4. Manual stage-by-stage run

This is the same order as the original design: **Java -> Python -> C++**.

### Windows

From inside `02-Full-Project`:

```bat
rem 1. Compile and run the Java frontend
javac -d java\out java\src\StateBridge.java java\src\Main.java
java -cp java\out Main

rem 2. Run the Python engine
python python\engine.py

rem 3. Compile and run the C++ renderer
g++ -std=c++11 cpp\receipt.cpp -o receipt.exe
receipt.exe
```

### Linux / macOS

From inside `02-Full-Project`:

```bash
# 1. Compile and run the Java frontend
javac -d java/out java/src/StateBridge.java java/src/Main.java
java -cp java/out Main

# 2. Run the Python engine
python3 python/engine.py

# 3. Compile and run the C++ renderer
g++ -std=c++11 cpp/receipt.cpp -o receipt
./receipt
```

You can also compile C++ first; the important part is the **run order**:
Java must finish before Python, and Python must finish before C++.

---

## 5. Run the Java-only version

From inside `01-Java-Only`:

```bat
javac -d out src\Main.java
java -cp out Main
```

or simply:

```bat
run.bat
```

This runs only the frontend. It does **not** create
`simulation_state.json`, so Python and C++ have nothing new to process.

---

## 6. Suggested walkthrough (full pipeline)

1. Start the full project (`run.bat` / `run.sh` or the manual commands).
2. Choose `[1] Customer`.
3. Press `[B]` to open the Reservation Record module.
4. Press `[A]` to create an order.
5. Enter the sample customer ID `25` (it already exists in
   `Customer_Records.txt`).
6. Pick a category, choose an item, and enter a quantity.
7. Press `[X]` to save the order. Java writes `simulation_state.json`.
8. Press `[X]` again to exit the CLI.
9. Python prices the order, marks it `COMPLETED`, and appends a row to
   `all_receipts.txt`.
10. C++ prints the receipt and marks the state `ARCHIVED`.

### Admin flow

1. Start the full project and choose `[2] Admin`.
2. Use the Customer Record module if needed.
3. Exit the CLI with `[X]`.
4. Java writes an `ADMIN` state.
5. Python skips the simulation.
6. C++ prints the master ledger from `all_receipts.txt`.

---

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| `javac` or `java` not found | Install a JDK and add its `bin` folder to `PATH`. |
| `g++` not found | Install MinGW-w64 (Windows) and add its `bin` folder to `PATH`. |
| `python` opens the Microsoft Store | `run.bat` / `run.sh` auto-detect `py` or `python`; if both fail, install Python 3 and reopen the terminal. |
| Files not found at runtime | Run from `01-Java-Only/` or `02-Full-Project/`, not the repo root. |
| Receipt does not print | Place an order before exiting the CLI. `run.bat` now prints a warning when there is no state to render, and Python/C++ report the status they found. |
| Old/duplicate orders appear | `all_receipts.txt` is append-only. Delete its contents or the file to reset. |

### Reset a demo run

From `02-Full-Project`:

```bat
del simulation_state.json
del all_receipts.txt
```

Recreate the seed files (or keep a backup) if you want the original demo data.
