# POLISHING — Full Project Change Log

Date: 2026-11-13 (initial polish pass) · 2026-11-14 (no-order bug fix)
Scope: `02-Full-Project` only (`java/`, `python/`, `cpp/`, runner scripts, docs)
Baseline: a pre-change snapshot of `02-Full-Project` was copied to a temp folder before any edit. `01-Java-Only` and all repository data files were left untouched.

Goal: fix the defects recorded in `DEBUGGING.md` and harden the pipeline without changing its overall design (Java writes state → Python prices → C++ presents).

---

## Summary

| # | Area | Change |
|---|---|---|
| P-01 | Java input | One shared `Scanner`; removed `BufferedReader` and per-method scanners |
| P-02 | Java input | Added `readChoice`, `readInt`, `readDouble`, `readText` helpers |
| P-03 | Java input | User-type prompt validates `1`/`2` and re-prompts |
| P-04 | Java input | Blank menu/module input re-prompts instead of throwing |
| P-05 | Java input | Customer record create/update uses validated readers |
| P-06 | Java orders | Quantity must be a number ≥ 1 |
| P-07 | Java orders | `ordermenu` returns the customer ID; Bill Out works after ordering |
| P-08 | Java output | One line per item; PHP amounts always 2 decimals |
| P-09 | Java bill out | Payment uses `readDouble` (no `InputMismatchException` loop) |
| P-10 | Java errors | Friendly messages instead of stack traces |
| P-11 | Java data | Bundle 2 label corrected |
| P-12 | C++ | Currency printed with 2 decimals |
| P-13 | C++ | `archiveState` replaces the status value format-independently |
| P-14 | Python | Validates state is a JSON object |
| P-15 | Python | Missing item fields no longer crash summarising/pricing |
| P-16 | Python | Exit codes: `1` for unreadable state, `0` for nothing to do |
| P-17 | Runner | `run.bat` checks tools and auto-detects Python |
| P-18 | Runner | `run.sh` checks tools, falls back `python3` → `python` |
| P-19 | Docs | `INSTRUCTIONS.MD` updated |
| P-20 | Docs | `README.md` documentation index added |
| P-21 | Docs | This `POLISHING.md` log added |
| P-22 | Docs | `DEBUGGING.md` annotated with post-polish status |
| P-23 | Verification | `run.sh` executed via Git Bash; `INSTRUCTIONS.MD` notes Git Bash support |
| P-24 | Java | Customer exit with no order writes `NO_ORDER`, clearing stale `PENDING` state |
| P-25 | Python | `NO_ORDER` sessions are skipped with a clear message |
| P-26 | C++ | `NO_ORDER` sessions print a friendly message and exit `0` |
| P-27 | Verification + docs | Stale-`PENDING` regression tests, matrix, and troubleshooting note |

---

## Java — `java/src/Main.java`

### P-01 — Single shared `Scanner`

**Why:** every `order*` method created a new `Scanner(System.in)`, and customer records used a separate `BufferedReader`. Multiple buffered readers on one stream lose input; piping a scripted session crashed with `NoSuchElementException` (see `DEBUGGING.md` §4).

**Before:**

```java
BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
Scanner scanner = new Scanner(System.in);
...
public static void orderMeals(Category Meals, int ID) {
    Scanner scanner = new Scanner(System.in);
    ...
    order = scanner.next().charAt(0);
```

**After:**

```java
Scanner scanner = new Scanner(System.in);
...
public static void orderMeals(Category Meals, int ID, Scanner scanner) {
    ...
    order = readChoice(scanner);
```

`BufferedReader` and every `br.readLine()`, `scanner.next()`, `nextInt()`, `nextDouble()` were removed.

**Verified:** a fully piped session now completes without exceptions (previously `NoSuchElementException` at `Main.orderMeals`).

---

### P-02 — Input helper methods

Added before `calculate(...)`:

```java
private static char readChoice(Scanner scanner)        // loops on blank input, returns uppercase first char
private static int readInt(Scanner scanner, String prompt)      // loops until a valid int
private static double readDouble(Scanner scanner, String prompt) // loops until a valid double
private static String readText(Scanner scanner, String prompt)   // loops until non-empty
```

**Verified:** invalid values (`abc`, blank line, `xyz`) print a message and re-prompt; no exceptions.

---

### P-03 — User-type validation

**Before:** `CURRENT_ROLE = scanner.nextLine().trim().equals("2") ? "ADMIN" : "CUSTOMER";` — any typo silently logged the user in as Customer.

**After:** loop accepts only `1` or `2`:

```java
while (true) {
    System.out.print("Choice: ");
    String choice = scanner.nextLine().trim();
    if (choice.equals("1")) { CURRENT_ROLE = "CUSTOMER"; break; }
    if (choice.equals("2")) { CURRENT_ROLE = "ADMIN"; break; }
    System.out.println("Please enter [1] for Customer or [2] for Admin.");
}
```

**Verified:** `abc` and blank input are rejected; `1` and `2` work.

---

### P-04 — Blank menu input

**Before:** `scanner.nextLine().toUpperCase().charAt(0)` — pressing Enter on an empty line threw `StringIndexOutOfBoundsException`.

**After:** all navigation and module prompts use `readChoice(scanner)`.

**Verified:** blank line at the order menu prints `Please enter a choice:` and continues.

---

### P-05 — Customer record input

**Before:** `Integer.parseInt(br.readLine())` — a non-numeric ID or contact crashed the program.

**After:**

```java
ID = readInt(scanner, "Enter personal ID number: ");
String Customer_Name = readText(scanner, "Enter name: ");
int Contact_No = readInt(scanner, "Enter contact number: ");
String DOR = readText(scanner, "Enter date of reservation (FORM MM/DD/YY): ");
String TOR = readText(scanner, "Enter time of reservation (FORM HH:MM): ");
int Guests = readInt(scanner, "Enter number of guests: ");
```

The update path uses the same helpers.

**Verified:** create + update + view cycle for ID 77 works; the record is replaced correctly.

---

### P-06 — Quantity validation

**Before:** `qty = scanner.nextInt()` accepted `0` and negative values.

**After:** each `order*` method (Meals, Drinks, Desserts, Side Dishes, Bundles) does:

```java
qty = readInt(scanner, "How many? ");
if (qty <= 0) {
    System.out.println("Quantity must be at least 1.");
    return;
}
```

**Verified:** non-numeric input re-prompts; `0`/negative is rejected.

---

### P-07 — Bill Out works after ordering

**Why:** the outer `ID` in `main` was only set by Customer Record create/update. `ordermenu` kept its own local `ID`, so `[C] Bill Out` after placing an order printed *"No matching ID, please register first."*

**Before:**

```java
public static void ordermenu(...) { int ID = 0; ... }
// call site
ordermenu(scanner, Meals, Drinks, Desserts, SideDishes, Bundles);
```

**After:**

```java
public static int ordermenu(...) { ... return ID; }
// call site
ID = ordermenu(scanner, Meals, Drinks, Desserts, SideDishes, Bundles);
```

**Verified:** order Meal 1 x2 → `[C]` → pay 500 → total PHP 312.50, change PHP 187.50, `Customer Receipt 25.txt` written.

---

### P-08 — Item lines and currency formatting

**Why:** multiple items in one category ran together (`...PHP 312.5Juice x2...`), and amounts printed with one decimal.

**Before (in `calculate`):**

```java
writer.write(category.products[i] + " x" + category.quantities[i] + " -> PHP " + itemTotal);
...
writer.write("No items ordered in this category.");
```

**After:**

```java
writer.write(String.format("%s x%d -> PHP %.2f\n",
        category.products[i], category.quantities[i], itemTotal));
...
writer.write("No items ordered in this category.\n");
```

`writeandsave` total is now `String.format("\n\nTotal Amount: PHP %.2f\n", totalAmount)`.

**Verified:** two meals produce two separate lines and a total of `PHP 497.10`.

---

### P-09 — Bill Out payment input

**Before:** `payment = scanner.nextDouble();` — non-numeric input threw `InputMismatchException`.

**After:** `payment = readDouble(scanner, "\nEnter payment amount: PHP ");`

**Verified:** numeric payment works; invalid text re-prompts.

---

### P-10 — Friendly error messages

Replaced stack traces with actionable messages:

| Situation | New message |
|---|---|
| Missing `Customer_Records.txt` (view) | `No customer records found yet. Create one with [A] first.` |
| Missing `Customer_Orders.txt` (view) | `No customer orders found yet.` |
| Missing records during ID check | `No customer records found yet. Please create a customer record first.` |
| Record save failure | `Could not save the customer record: <reason>` |
| Record update failure | `Could not update the customer record: <reason>` |
| Order save failure | `Could not save the order: <reason>` |
| Receipt write failure | `Could not write the receipt file: <reason>` |

---

### P-11 — Bundle 2 label

**Before:** `product = "Meal 2, Sprite, Ice Cream, & Buttered Corn ";` (missing prefix, trailing space)

**After:** `product = "Bundle 2 -> Meal 2, Sprite, Ice Cream, & Buttered Corn";`

**Verified:** label matches Bundles 1 and 3 and flows into the order file/state cleanly.

---

## C++ — `cpp/receipt.cpp`

### P-12 — Two-decimal currency

**Before:** `PHP 352.5`

**After:** added `#include <iomanip>` and:

```cpp
std::cout << " Total Price:  PHP " << std::fixed << std::setprecision(2)
          << receipt->totalPrice << "\n";
```

Applies to both individual receipts and the admin ledger.

**Verified:** `PHP 352.50`, `PHP 312.50`, `PHP 10.50`.

---

### P-13 — Format-independent status archiving

**Why:** `archiveState` searched for the literal string `"status": "COMPLETED"`. That works with Python's current `json.dump(indent=4)` output but breaks if the JSON is compact or reformatted.

**Before:**

```cpp
size_t statusPos = updated.find("\"status\": \"COMPLETED\"");
...
updated.replace(statusPos, std::string("\"status\": \"COMPLETED\"").size(),
                "\"status\": \"ARCHIVED\"");
```

**After:** locate the `"status"` key, then the quoted value after `:`, and replace only the value with `ARCHIVED`.

**Verified:** a compact state file (`{"status":"COMPLETED",...}`) printed the receipt and was archived correctly.

The guard `if (status != "COMPLETED")` in `main()` is unchanged, so only completed orders are archived.

---

## Python — `python/engine.py`

### P-14 — State shape validation

`load_state()` now checks `isinstance(data, dict)` and prints `State file is not a JSON object.` for arrays/scalars instead of failing later on `.get`.

---

### P-15 — Tolerant item handling

**Before:** `build_summary` used `item['name']`/`item['qty']`, and the total used `item["price"]`/`item["qty"]` — a malformed item raised `KeyError`/`ValueError`.

**After:**

```python
name = str(item.get("name", "Unknown item")).strip() or "Unknown item"
parts.append(f"{name} x{item.get('qty', 0)}")
```

and pricing wraps each item:

```python
try:
    total += float(item.get("price", 0.0)) * int(item.get("qty", 0))
except (TypeError, ValueError):
    print(f"[Python] Warning: skipped malformed item: {item}")
```

---

### P-16 — Exit codes

`main()` returns `0` for "nothing to do" and `1` when `simulation_state.json` exists but cannot be parsed. The entry point is now `raise SystemExit(main())`.

This lets `run.bat`/`run.sh` stop before C++ when the state file is corrupt, instead of rendering stale data.

**Verified:** invalid JSON → `Could not read state file...`, exit `1`; missing file → exit `0`.

---

## Runner scripts

### P-17 — `run.bat`

- Checks `javac`, `java`, and `g++` up front with clear `[error]` messages.
- Detects Python by **executing** candidates, so the zero-byte Microsoft Store alias cannot pass:

```bat
set "PYTHON="
py -3 --version >nul 2>nul && set "PYTHON=py -3"
if not defined PYTHON (
    python --version >nul 2>nul && set "PYTHON=python"
)
```

- Prints `Using Python: py -3` (or whichever it found).
- On C++ exit code 1 (no state to render) prints `[warn] No receipt was rendered. Place an order before the pipeline runs.` and still finishes with exit code `0`.
- Prints `Done.` at the end.

**Verified:** with an order → exit `0`, all four stages, receipt printed; without an order → warning + `Done.`, exit `0`.

---

### P-18 — `run.sh`

Mirrors the batch script: checks `javac`/`java`/`g++`, chooses `python3` then `python`, quotes the interpreter, and warns instead of failing when C++ has no state to render.

**Verified:** executed with Git Bash (`C:\Program Files\Git\bin\bash.exe`, bash 5.2.37) using a fresh `PATH`:

- With an order → exit `0`, `Using Python: python3`, receipt `PHP 312.50`, state `ARCHIVED`, ledger row appended.
- Without an order → `[warn] No receipt was rendered...` + `Done.`, exit `0`.

(`bash.exe` is not on the PowerShell `PATH` by default, which is why the first pass reviewed it statically; the executable is present at `C:\Program Files\Git\bin\bash.exe`.)

---

## Docs

### P-19 — `INSTRUCTIONS.MD`

- Section 3 notes that the runner auto-detects `py`/`python` and prints the interpreter used.
- Troubleshooting: the `python` row now points at auto-detection; the receipt row explains the no-state warning.

### P-20 — `README.md`

Added a short documentation table linking `INSTRUCTIONS.MD`, `DEBUGGING.md`, `POLISHING.md`, and `REFERENCE.md`.

### P-21 — `POLISHING.md`

This file.

### P-22 — `DEBUGGING.md` status annotations

- Added a post-polish note at the top pointing to this log.
- §4 (piped stdin) now records that the shared-`Scanner` fix resolved it.
- §8 marks the C++ currency formatting and Bundle 2 label as fixed, with the new behavior shown.

### P-23 — `run.sh` verification + Git Bash note

- P-18 and the verification matrix now record `run.sh` as **executed** via Git Bash (`C:\Program Files\Git\bin\bash.exe`, bash 5.2.37), not just reviewed.
- `INSTRUCTIONS.MD` §3 notes that Windows users can run `run.sh` from Git Bash.

---

## Bug fix, 2026-11-14 — exiting without an order printed a stale receipt

**Report:** *"I chose Customer and Exited, it still prints a ticket."*

### Cause

Java only wrote `simulation_state.json` in two places: `writeandsave` (a real order) and the admin exit path. Selecting `[1] Customer` and then `[X] Exit` wrote **nothing**, so a `PENDING` order left by a previous run was still read by Python (priced and marked `COMPLETED`) and printed by C++. C++ then archived it, which is why the same `TXN-1001` row could appear twice in `all_receipts.txt`.

### P-24 — Java writes `NO_ORDER` on a customer exit with no order

`java/src/StateBridge.java`:

- `writeOrder(...)` now returns `boolean` (the old `void` caller could not tell whether the state file was written).
- New `writeMinimalState(orderId, customerId, role, status)` holds the shared minimal JSON; `writeAdminView()` now reuses it.
- New `writeNoOrder(role)` writes:

```json
{
    "order_id": "NONE",
    "customer_id": 0,
    "role": "CUSTOMER",
    "items": [],
    "subtotal": 0.00,
    "total_price": 0.00,
    "payment": 0.00,
    "change": 0.00,
    "status": "NO_ORDER"
}
```

`java/src/Main.java`:

- Added `public static boolean ORDER_SUBMITTED = false;`.
- `writeandsave` sets it from the `writeOrder` result and only prints `Order received.` on success.

```java
ORDER_SUBMITTED = StateBridge.writeOrder(ID, CURRENT_ROLE, totalAmount,
        Meals, Drinks, Desserts, SideDishes, Bundles);
if (ORDER_SUBMITTED) {
    System.out.println("Order received. \n");
} else {
    System.out.println("The order was not saved. Please try again.");
}
```

- The exit block now distinguishes the three cases:

```java
if (CURRENT_ROLE.equals("ADMIN")) {
    StateBridge.writeAdminView();
} else if (!ORDER_SUBMITTED) {
    // No order was placed, so clear any pending order from a previous run.
    StateBridge.writeNoOrder(CURRENT_ROLE);
}
```

A real order is **left as `PENDING`** for Python. Only a session that never submitted one is marked `NO_ORDER`.

### P-25 — Python skips `NO_ORDER`

`python/engine.py`:

```python
if status == "NO_ORDER":
    print("[Python] No order was placed in this session. Nothing to price.")
    return 0
```

### P-26 — C++ handles `NO_ORDER`

`cpp/receipt.cpp`:

```cpp
if (status == "NO_ORDER") {
    std::cout << "[C++] No order was placed in this session. No receipt to print.\n";
    return 0;
}
```

Exit code `0`, so the runner does not print the "No receipt was rendered" warning for a legitimate no-order session.

### P-27 — Tests and docs

- Added the stale-`PENDING` regression cases to the matrix below.
- Added a troubleshooting row to `INSTRUCTIONS.MD`.
- `DEBUGGING.md` §8 now notes the status-gating behavior.

**Note on existing data:** the repository's `all_receipts.txt` and `simulation_state.json` still contain the two `TXN-1001` rows from the pre-fix run. They were left untouched (the app only appends). The fix prevents any new stale receipts; clearing the old rows is a separate manual decision.


---

## Verification matrix

All tests ran in temporary copies of `02-Full-Project`; repository data files were not modified.

| Test | Result |
|---|---|
| `javac -Xlint:all` | Clean, no warnings |
| `g++ -std=c++11 -Wall -Wextra` | Clean, no warnings |
| `python -m py_compile` | Clean |
| Piped customer flow (previously crashed) | Exit 0; `TXN-7906`; PHP 352.50; state `ARCHIVED` |
| Ledger row | `TXN-7906|25|...|352.50` |
| Bill Out after ordering | Total PHP 312.50, change PHP 187.50, receipt file written |
| Invalid role / invalid ID / blank menu / bad qty | Re-prompted, no exceptions |
| Multi-item category + 5-item limit | Separate lines; PHP 497.10; 3rd item rejected at limit |
| Customer record create → update → view | Pass |
| Admin flow → Python skip → C++ ledger | Pass; ledger totals 2 decimals |
| Compact (`"status":"COMPLETED"`) state | Printed and archived |
| Corrupt state file | Python exit 1, clear message |
| `run.bat` with an order | Exit 0; `Using Python: py -3`; receipt; `ARCHIVED` |
| `run.bat` with no order | Warning + `Done.`, exit 0 |
| `run.sh` via Git Bash | Exit 0 both paths; `Using Python: python3`; receipt `PHP 312.50`; state `ARCHIVED` |
| Stale `PENDING` + Customer → Exit (Java/Python/C++) | State `NO_ORDER`; no receipt; ledger unchanged |
| `run.bat` stale `PENDING` + Customer → Exit | `[C++] No order was placed...`; ledger unchanged; exit 0 |
| `run.sh` (Git Bash) stale `PENDING` + Customer → Exit | Same as above; `Using Python: python3`; exit 0 |
| Missing state + Customer → Exit | `NO_ORDER` written; Python/C++ skip; exit 0 |
| Real order after the fix | Java `PENDING` → Python `COMPLETED` → C++ `ARCHIVED`; receipt PHP 312.50 |
| Admin flow after the fix | `ADMIN_VIEW`; Python skips; C++ prints the ledger |

---

## Not changed (deliberate)

- `01-Java-Only` — separate standalone app; see `DEBUGGING.md` §5. It still does not write `simulation_state.json`.
- Repo data files (`Customer_Records.txt`, `Customer_Orders.txt`, `all_receipts.txt`, `simulation_state.json`) — no resets or fixture changes.
- Order ID generation — still `System.currentTimeMillis() % 10000`, so two orders in the same millisecond window can collide.
- Admin ordering — an admin can still place an order; Python skips `ADMIN` sessions, so no receipt is produced for it. This matches the documented admin flow.
- Bill Out — still a local Java receipt module; it does not pass through Python/C++ by design.
- Pipeline order and file formats (`simulation_state.json`, pipe-delimited `all_receipts.txt`) — unchanged.

---

## How to re-run the verification

From `02-Full-Project`:

```bat
run.bat
```

For an automated smoke test:

```bat
(echo 1& echo B& echo A& echo 25& echo A& echo A& echo 2& echo X& echo X& echo X) > input.txt
run.bat < input.txt
```

Expected: `[Python] ... priced at PHP 312.50`, a receipt with `PHP 312.50`, and final state `ARCHIVED`.

To check the no-order fix, run the same pipeline with only `Customer` → `Exit`:

```bat
(echo 1& echo X) > input.txt
run.bat < input.txt
```

Expected: `[Python] No order was placed in this session...`, `[C++] No order was placed in this session...`, **no receipt**, and final state `NO_ORDER`.
