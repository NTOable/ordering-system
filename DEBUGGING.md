# DEBUGGING — Ordering System Verification

Date: 2026-11-13
Last re-test: 2026-11-13, after the `PATH` reorder
Scope: `02-Full-Project` (Java + Python + C++) and `01-Java-Only`

> **Post-polish status (2026-11-13):** the Full Project defects in §4 (piped stdin) and §8 (currency formatting, Bundle 2 label) were fixed in the polish pass — see `POLISHING.md`. §3 (Python PATH) was resolved separately. §5 (Java-Only) is unchanged by design.

## TL;DR

| Question | Answer |
|---|---|
| Does the Full Project work? | **Yes.** Java → Python → C++ pipeline verified for both customer and admin flows. |
| Does Java-Only work together with Python + C++? | **No.** Java-Only never writes `simulation_state.json`, so the other two stages have nothing to process. |
| Is `python` failing because Python isn't installed? | **No.** Python 3.14.3 is installed. `python.exe` was shadowed by the Microsoft Store App Execution Alias. **Resolved by the `PATH` reorder** — `python` now reports 3.14.3 (section 3). |

No files in this repository were modified during testing — all runs used scratch copies under the temp directory.

---

## 1. Environment

| Tool | Version | Status |
|---|---|---|
| `java` / `javac` | 17 (2021-09-14 LTS) | Working |
| `g++` | 16.2.0 (MSYS2) | Working |
| `py` | Python 3.14.3 | Working |
| `python` | Python 3.14.3 | **Working after `PATH` reorder** (previously exit code `9009`) |
| `python3` | Python 3.14.3 | Working after `PATH` reorder |
| Real interpreter | `C:\Users\pards\AppData\Local\Python\pythoncore-3.14-64\python.exe` | Working |
| Launcher shim | `C:\Users\pards\AppData\Local\Python\bin\python.exe` | Working when called directly |

---

## 2. Full Project verification (`02-Full-Project`)

Test order: customer ID `25` (exists in `Customer_Records.txt`), Meal 1 x2 (PHP 156.25) + Juice x2 (PHP 20.00) = PHP 352.50.

| Stage | Observed result |
|---|---|
| `javac -d java\out java\src\StateBridge.java java\src\Main.java` | Compiled, no errors |
| `g++ -std=c++11 cpp\receipt.cpp -o receipt.exe` | Compiled, no errors |
| Java CLI (customer flow) | `[Java] Order written to simulation_state.json as TXN-...` |
| State after Java | `status: "PENDING"`, `total_price: 0.00`, items present |
| `python engine.py` | `[Python] Order TXN-... priced at PHP 352.50.` / `Status set to COMPLETED` |
| State after Python | `status: "COMPLETED"`, `total_price: 352.5`, `summary` added |
| `receipt.exe` | Printed `OFFICIAL RECEIPT`, then `[C++] Receipt logged and state archived.` |
| State after C++ | `status: "ARCHIVED"` |
| `all_receipts.txt` | New row appended: `TXN-...|25|...|352.50` |

Admin flow (choice `[2] Admin`, then `[X]`):

| Stage | Observed result |
|---|---|
| Java | Wrote state with `role: "ADMIN"`, `status: "ADMIN_VIEW"` |
| Python | `[Python] Admin session detected. Skipping order simulation.` |
| C++ | Printed `ADMIN PANEL: MASTER LEDGER` with all archived rows |

Relevant code paths:

- `02-Full-Project/java/src/Main.java:20` — user-type selection sets `CURRENT_ROLE`
- `02-Full-Project/java/src/Main.java:161` — admin exit calls `StateBridge.writeAdminView()`
- `02-Full-Project/java/src/Main.java:207` — order save calls `StateBridge.writeOrder(...)`
- `02-Full-Project/java/src/StateBridge.java:15` — `STATE_FILE = "simulation_state.json"`
- `02-Full-Project/python/engine.py:60-66` — admin skip and `PENDING` gate
- `02-Full-Project/cpp/receipt.cpp:198-226` — admin ledger vs receipt + archive

**Conclusion:** the Full Project is functioning as designed.

### Re-test after the PATH change (2026-11-13)

Ran the actual `run.bat` from a fresh environment (registry `PATH` rebuilt as a new terminal would see it):

```text
[1/4] Compiling Java frontend...
[2/4] Compiling C++ receipt engine...
[3/4] Running Java CLI...
[Java] Order written to simulation_state.json as TXN-5571.
[4/4] Running Python engine then C++ renderer...
[Python] Order TXN-5571 priced at PHP 352.50.
[C++] Receipt logged and state archived.
```

`run.bat` exit code: **0**. State ended `ARCHIVED`, and `all_receipts.txt` gained the `TXN-5571|25|...|352.50` row. The admin flow was re-run with `python python\engine.py` (plain `python`, no workaround) and also passed: Python skipped the simulation and C++ printed the ledger.

---

## 3. Caveat 1 — `run.bat` fails at the Python stage (RESOLVED)

> **Status: resolved by the 2026-11-13 `PATH` reorder.**
> `C:\Users\pards\AppData\Local\Python\bin` now precedes
> `C:\Users\pards\AppData\Local\Microsoft\WindowsApps` in the user `PATH`, so
> `python` resolves to the real interpreter (`Python 3.14.3`, exit code `0`).
> The section below is kept as the root-cause record.

### Original symptom

```text
[4/4] Running Python engine then C++ renderer...
Python was not found; run without arguments to install from the Microsoft Store...
```

Exit code `9009`, so `if errorlevel 1` aborts before `receipt.exe`.

### Root cause — NOT a missing interpreter

Python 3.14.3 **is** installed:

```text
> py --list-paths
 -V:3.14[-64] *   C:\Users\pards\AppData\Local\Python\pythoncore-3.14-64\python.exe
```

The problem is command resolution. **Before the fix**, `PATH` contained both of these, in this order:

```text
C:\Users\pards\AppData\Local\Microsoft\WindowsApps     <-- first
C:\Users\pards\AppData\Local\Python\bin                <-- second
```

`C:\Users\pards\AppData\Local\Microsoft\WindowsApps\python.exe` is a **0-byte App Execution Alias stub**, not a real interpreter. Because it comes first, `python` hits the stub, which prints the Microsoft Store message and returns `9009`. The real `python.exe` in `...\Python\bin` is never reached.

`python3` is shadowed the same way.

### Evidence (before the fix)

```text
> where.exe python
C:\Users\pards\AppData\Local\Microsoft\WindowsApps\python.exe   (0-byte alias)
C:\Users\pards\AppData\Local\Python\bin\python.exe              (real shim)

> cmd /c python --version     -> 9009 (Store message)
> cmd /c py --version         -> Python 3.14.3
> & "$env:LOCALAPPDATA\Python\bin\python.exe" --version  -> Python 3.14.3
```

### Fixes (pick one)

1. **Recommended — use the launcher in the script.**
   Change `python python\engine.py` to `py python\engine.py` in `02-Full-Project\run.bat`.
   (`py` resolves to a working launcher on this machine.)
2. **Turn off the alias.** Settings → Apps → Advanced app settings → App execution aliases → switch off `python.exe` (and `python3.exe`). `python` then resolves to the real shim further down `PATH`.
3. **Reorder `PATH`** so `C:\Users\pards\AppData\Local\Python\bin` appears before `...\Microsoft\WindowsApps`. **<-- applied 2026-11-13**
4. **Call the interpreter directly** in the script (absolute path).

Note: `run.sh` uses `python3`. Under Git Bash on Windows it would have hit the same stub; `python3` now also resolves to 3.14.3 in a fresh shell. On Linux/macOS this does not apply.

### Verification of the fix

```text
--- registry User PATH (current) ---
C:\Users\pards\AppData\Local\Python\bin          <-- now first
C:\Users\pards\AppData\Local\Microsoft\WindowsApps

> where.exe python
C:\Users\pards\AppData\Local\Python\bin\python.exe

> python --version    -> Python 3.14.3   (exit 0)
> python3 --version   -> Python 3.14.3   (exit 0)
```

Important: a **fresh terminal** is required. Processes started before the change (already-open shells, IDEs, or an editor's integrated terminal) keep the old `PATH` until restarted. The tool session used for the first review still had the stale value; the re-test explicitly rebuilt `PATH` from the registry to simulate a new terminal.

The zero-byte alias stubs still exist in `WindowsApps`. If `...\Python\bin` is ever removed from `PATH` or moved back behind `WindowsApps`, this failure returns.

---

## 4. Caveat 2 — piped stdin crashes the Java CLI

### Symptom

```text
Exception in thread "main" java.util.NoSuchElementException
    at java.base/java.util.Scanner.next(Scanner.java:1478)
    at Main.orderMeals(Main.java:226)
```

This happens when input is piped, e.g. `Get-Content input.txt | java -cp java\out Main`.

### Cause

`Main` creates several independent `Scanner` instances over `System.in` (one in `main`, plus a new `Scanner` inside `orderMeals`, `orderDrinks`, `orderDesserts`, `orderSideDish`, and `orderBundle`). Each `Scanner` buffers input, so with a pipe the outer scanner swallows the whole input and the inner scanner finds the stream empty.

### Impact

- **Interactive use: unaffected.** Verified by driving the CLI one line at a time with a short delay per line.
- **Automated/scripted use: broken.** Not a problem for the intended demo flow, but blocks piped test scripts.

### Workaround / fix

- Workaround: type input interactively, or send lines one at a time with a small delay.
- Proper fix: use a single shared `Scanner` (pass it into the `order*` methods) instead of creating new ones.

**Re-test 2026-11-13:** this still reproduced after the `PATH` change (it is unrelated to Python), but it was **fixed in the polish pass**: the Java CLI now uses one shared `Scanner` plus validated reader helpers, and a fully piped session completes without exceptions (see `POLISHING.md` P-01/P-02). Interactive use was never affected.

---

## 5. Java-Only + Python + C++ does not work

### Why

`01-Java-Only/src/Main.java` is the Full Project's `Main.java` with the integration removed:

- no `CURRENT_ROLE` / no user-type prompt,
- no `StateBridge.writeOrder(...)` in `writeandsave`,
- no `StateBridge.writeAdminView()` on exit.

Since `simulation_state.json` is only ever written by `StateBridge`, Java-Only produces **no state file**, and Python/C++ have no input.

### Test A — Java-Only folder with `python/` and `cpp/` copied in

Order placed successfully (saved to `Customer_Orders.txt`), then:

```text
[Python] No simulation_state.json found. Nothing to do.
[C++] No state file available.        (exit code 1)
```

### Test B — Java-Only `Main.java` compiled into `02-Full-Project/java/`

The order placed was **Meal 3 x1 (PHP 450.00)**. The receipt printed an entirely different, older order:

```text
 Order ID:     TXN-1001
 Customer ID:  25
 Items:        Meal 1 -> Fried Chicken W/ Garlic Rice x2; Juice x2
 Total Price:  PHP 352.5
```

Cause: `Main.class` never updated `simulation_state.json`, so Python picked up the stale `PENDING` order left from a previous run, marked it `COMPLETED`, and C++ printed it. The ledger then got a duplicate `TXN-1001` row.

This is worse than failing loudly: the pipeline appears to work while reporting the wrong order.

### Conclusion

- **Standalone Java frontend** → use `01-Java-Only` (`run.bat` or the `javac`/`java` commands in `INSTRUCTIONS.MD` section 5). It correctly writes `Customer_Orders.txt`, and its `[C] Bill Out Module` writes `Customer Receipt <ID>.txt`.
- **Polyglot pipeline with receipts** → must use `02-Full-Project/java/src/Main.java` together with `StateBridge.java`. They are not interchangeable.

**Re-test 2026-11-13:** unchanged. Java-Only compiled and ran, `simulation_state.json` was still not created, `python` printed `No simulation_state.json found. Nothing to do.`, and the C++ renderer printed `[C++] No state file available.` with exit code `1`. The `PATH` fix only affects Python command resolution, not the missing `StateBridge` integration.

---

## 6. How to reproduce the verification

From `02-Full-Project` (Windows):

```bat
javac -d java\out java\src\StateBridge.java java\src\Main.java
g++ -std=c++11 cpp\receipt.cpp -o receipt.exe
java -cp java\out Main
python python\engine.py
receipt.exe
```

or simply:

```bat
run.bat
```

Use `py python\engine.py` instead if `python` ever regresses to the Store stub.

Then check:

```bat
type simulation_state.json
type all_receipts.txt
```

Expected after the full sequence: `"status": "ARCHIVED"`, a new ledger row, and a printed receipt.

---

## 7. Files inspected

- `02-Full-Project/java/src/Main.java`
- `02-Full-Project/java/src/StateBridge.java`
- `02-Full-Project/python/engine.py`
- `02-Full-Project/cpp/receipt.cpp`
- `02-Full-Project/run.bat`, `02-Full-Project/run.sh`
- `02-Full-Project/simulation_state.json`
- `02-Full-Project/Customer_Records.txt`, `Customer_Orders.txt`, `all_receipts.txt`
- `01-Java-Only/src/Main.java`, `01-Java-Only/run.bat`
- `INSTRUCTIONS.MD`

---

## 8. Other observations (not blockers)

- **Currency formatting in C++:** ~~`receipt.cpp` uses default `std::cout` precision, so PHP 352.50 prints as `PHP 352.5`.~~ **Fixed in polish:** totals now print with two decimals (`PHP 352.50`).
- **Bundle 2 name is inconsistent:** ~~`Main.java:469` sets `product = "Meal 2, Sprite, Ice Cream, & Buttered Corn "` — missing the `Bundle 2 ->` prefix (and it has a trailing space), unlike Bundles 1 and 3.~~ **Fixed in polish:** now `Bundle 2 -> Meal 2, Sprite, Ice Cream, & Buttered Corn`.
- **Append-only files:** `all_receipts.txt` and `Customer_Orders.txt` accumulate rows across runs. Delete them (or their contents) to reset a demo.
- **Status gating:** if `simulation_state.json` is left `ARCHIVED` or `COMPLETED`, re-running Python/C++ without a fresh Java order does nothing (`[Python] Nothing to process` / C++ "not ready for a receipt").
