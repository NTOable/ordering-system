# REFERENCE — Polyglot Restaurant Ordering System

This document records the baseline decision, the two folders that were created,
the data contracts between the three languages, the working scaffold, and the
consultation notes that shaped the architecture.

---

## 1. Purpose

Build a **CLI Restaurant Ordering System** with two user types:

- **Customers** — view the menu, order items, and pay.
- **Admins** — customize the menu and view customer accounts.

All persistence is done with **plain text files** (`BufferedReader`,
`BufferedWriter`, `FileWriter`, `Scanner`, `std::ifstream`, Python `open`)
instead of a database. The end goal is to run **Java + Python + C++** as one
pipeline.

---

## 2. Baseline determination

### 2.1 Inventory of every `main` file

| File | Lines | What it is | Verdict |
|---|---:|---|---|
| `HotelRestoSystem/src/Main.java` | 646 | Valid `public class Main` with `public static void main(String[])`. Has customer records (create/update/view), reservation/ordering, billing/payment, nested `Category` and `IDValidator`, and file persistence. | **BEST BASELINE** |
| `Finals/src/HotelRestoSystem.java` | 642 | Same restaurant logic, but **structurally broken**: no class declaration, bare `public void main()` (instance method, not an entry point), a `private int cID` field placed outside any class, and broken indentation. | Not usable as-is |
| `Finals/src/Main.java` | 0 | Empty file. | Ignore |
| `JavaProject/src/Main.java` | 201 | Generic user CRUD (`UserDetails.txt`): create/write/search/update/delete/view. No menu, ordering, or payment. | Useful only as a **file-CRUD reference** |
| `OrderingSystem.java` (repo root) | 264 | Customer ordering and payment only. No customer records and no admin side. | Useful only as an **ordering-logic reference** |

### 2.2 Why `HotelRestoSystem/src/Main.java` wins

1. **It actually compiles and runs.** It is the only large file with a proper
   class wrapper and `main(String[] args)` entry point.
2. **It already covers the required feature set** for the first milestone:
   - customer record CRUD (`ccrecord`, `ucrecord`, `vcrecord`),
   - menu browsing and ordering (`ordermenu`, `orderMeals`, `orderDrinks`, ...),
   - billing and payment (`billout`),
   - file persistence with no database.
3. **It has a reusable data model.** `Category` (products, prices, quantities)
   and `IDValidator` can be extended instead of rewritten.
4. **It ships with sample data** (`Customer_Records.txt`,
   `Customer_Orders.txt`, `Customer Receipt 25.txt`) so the flow can be tested
   immediately.
5. **The `Finals` copy is the same logic but damaged.** It is useful as a
   reference diff, not as the starting point.

**Chosen baseline: `HotelRestoSystem/src/Main.java`.**

---

## 3. Folders created

```text
ordering_system/
├── 01-Java-Only/
│   ├── src/
│   │   └── Main.java                 # exact copy of the chosen baseline
│   ├── Customer_Records.txt          # sample data
│   ├── Customer_Orders.txt           # sample data
│   ├── Customer Receipt 25.txt       # sample data
│   ├── run.bat                       # compile + run
│   └── .gitignore
│
├── 02-Full-Project/
│   ├── java/
│   │   └── src/
│   │       ├── Main.java             # baseline + role prompt + StateBridge hook
│   │       └── StateBridge.java      # Java -> shared state writer/reader
│   ├── python/
│   │   └── engine.py                 # pricing, simulation, ledger append
│   ├── cpp/
│   │   └── receipt.cpp               # receipt renderer + admin ledger
│   ├── simulation_state.json         # shared state (seed)
│   ├── all_receipts.txt              # append-only ledger (seed)
│   ├── Customer_Records.txt          # sample data
│   ├── Customer_Orders.txt           # sample data
│   ├── run.bat                       # Windows orchestrator
│   ├── run.sh                        # Linux/macOS orchestrator
│   └── .gitignore
│
├── Finals/                           # original (reference only)
├── HotelRestoSystem/                 # original baseline project
├── JavaProject/                      # original CRUD reference
├── OrderingSystem.java               # original ordering reference
├── README.md
├── REFERENCE.md                      # this file
└── INSTRUCTIONS.MD                   # how to run the app
```

### What each folder is for

- **`01-Java-Only/`** — the Java frontend by itself. This is the folder you own
  and can freely restructure. It contains no Python, no C++, and no shared
  state. Run `run.bat` from inside the folder.
- **`02-Full-Project/`** — the integration target. It keeps the same Java
  baseline but adds `StateBridge`, the Python engine, the C++ renderer, the
  shared state file, the ledger, and the orchestrators.

### Is `01-Java-Only/` the frontend of the full app?

**Yes.** `01-Java-Only/src/Main.java` is the complete Java frontend of the
entire CLI application. Read it first to learn how the system works.

`02-Full-Project/java/src/Main.java` is the *same file* plus 14 lines and no
deletions (744 lines vs. 758 lines). The only differences are the integration
hooks:

1. `public static String CURRENT_ROLE = "CUSTOMER";`
2. The startup `SELECT USER TYPE` prompt (`[1] Customer` / `[2] Admin`).
3. `StateBridge.writeOrder(...)` after an order is saved.
4. `StateBridge.writeAdminView()` when an admin exits.

Everything that makes up the app's UI and behavior lives in the Java-only
file:

- main NAV menu and submenus,
- Customer Record module (create / update / view),
- Reservation / Order module (menu browsing, quantities, 5-item limit),
- Bill Out module (payment and change),
- `Category` and `IDValidator`,
- all `Customer_Records.txt` / `Customer_Orders.txt` / receipt file I/O.

The only thing it does **not** do is hand the order to Python and C++ via
`simulation_state.json`. In other words: `01-Java-Only/` is the frontend in
isolation; `02-Full-Project/java/` is that frontend wired to the pipeline.

> Do not confuse this with the repo-root `OrderingSystem.java` (264 lines).
> That is an older ordering/payment-only reference, not the Java-only app.

---

## 4. Architecture: Pipeline File Sharing

The three languages do **not** run at the same time. An orchestrator runs them
sequentially, and each stage reads and writes the same state file.

```text
[User Input]
     │
     ▼
1. JAVA CLI (java/src/Main.java + StateBridge.java)
   - reads/writes Customer_Records.txt, Customer_Orders.txt
   - writes simulation_state.json with status PENDING
     │
     ▼
2. PYTHON ENGINE (python/engine.py)
   - reads simulation_state.json
   - prices the order, simulates preparation
   - sets status COMPLETED, appends to all_receipts.txt
     │
     ▼
3. C++ RENDERER (cpp/receipt.cpp)
   - reads simulation_state.json
   - CUSTOMER: prints receipt, sets status ARCHIVED
   - ADMIN: reads all_receipts.txt and prints the master ledger
```

### Division of labor

| Layer | Language | Responsibility |
|---|---|---|
| Input / UI | **Java** | Menus, role selection, input validation, customer/admin flows, text-file CRUD |
| Business rules | **Python** | Pricing, inventory/simulation, order status transitions, ledger append |
| Presentation | **C++** | Receipt rendering, admin ledger display, fast text parsing, pointer practice |

---

## 5. Data contracts

### 5.1 `simulation_state.json`

Written by Java, updated by Python, archived by C++.

| Field | Type | Written by | Meaning |
|---|---|---|---|
| `order_id` | string | Java | `TXN-####` identifier |
| `customer_id` | int | Java | Customer record ID |
| `role` | string | Java | `CUSTOMER` or `ADMIN` |
| `items` | array | Java | `{name, qty, price}` objects |
| `subtotal` | number | Java | Sum of item lines |
| `total_price` | number | Python | Final computed total |
| `payment` | number | Java/Python | Amount tendered |
| `change` | number | Java/Python | Change due |
| `status` | string | Java/Python/C++ | `PENDING` → `COMPLETED` → `ARCHIVED` (or `CANCELLED`, `ADMIN_VIEW`) |
| `summary` | string | Python | Flattened item list for C++ |

Example:

```json
{
    "order_id": "TXN-1001",
    "customer_id": 25,
    "role": "CUSTOMER",
    "items": [
        {"name": "Meal 1 -> Fried Chicken W/ Garlic Rice", "qty": 2, "price": 156.25},
        {"name": "Juice", "qty": 2, "price": 20.00}
    ],
    "subtotal": 352.50,
    "total_price": 0.00,
    "payment": 0.00,
    "change": 0.00,
    "status": "PENDING"
}
```

### 5.2 `all_receipts.txt` (append-only ledger)

**Pipe-delimited**, not space-delimited:

```text
order_id|customer_id|summary|total_price
```

Example:

```text
TXN-1001|25|Meal 1 -> Fried Chicken W/ Garlic Rice x2; Juice x2|352.50
```

> **Why pipes?** The original consultation suggested
> `ID ItemName Quantity Price` parsed with `>>`. Item names contain spaces
> (`Meal 1 -> Fried Chicken W/ Garlic Rice`), so `>>` would split them into
> multiple tokens. Pipes keep every field intact and are trivial for
> `std::getline(stream, field, '|')`.

### 5.3 Existing text files (unchanged)

- `Customer_Records.txt` — customer ID, name, contact, reservation details.
- `Customer_Orders.txt` — human-readable order log.
- `Customer Receipt <ID>.txt` — Java `billout` receipt (legacy path).

---

## 6. What was implemented

### 6.1 `01-Java-Only/`

- Exact copy of `HotelRestoSystem/src/Main.java`.
- This is the **frontend of the full application**: all menus, role-free
  customer/admin flows, input validation, ordering, billing, and text-file CRUD
  live here. The full project's `java/src/Main.java` is this same file plus the
  14 integration lines described in section 3 (role prompt + `StateBridge`
  hooks).
- Sample data files copied alongside it.
- `run.bat` compiles to `out/` and runs `Main`.

### 6.2 `02-Full-Project/java/src/Main.java`

The baseline is preserved. Only three additions were made:

1. `public static String CURRENT_ROLE = "CUSTOMER";`
2. A role prompt at startup:
   ```text
   SELECT USER TYPE
   [1] Customer
   [2] Admin
   ```
3. A hand-off call after an order is saved:
   ```java
   StateBridge.writeOrder(ID, CURRENT_ROLE, totalAmount,
           Meals, Drinks, Desserts, SideDishes, Bundles);
   ```
4. On admin exit, `StateBridge.writeAdminView()` writes an `ADMIN` state so
   C++ knows to print the ledger.

### 6.3 `02-Full-Project/java/src/StateBridge.java`

- `writeOrder(...)` serializes the five `Category` arrays into
  `simulation_state.json` (status `PENDING`).
- `readState()` demonstrates `BufferedReader` reading of the shared file.
- `writeAdminView()` writes a minimal `ADMIN_VIEW` state.
- Uses `BufferedWriter`, satisfying the "BufferedString/Text files" requirement.

### 6.4 `02-Full-Project/python/engine.py`

- Loads the state file.
- Skips admin sessions.
- Computes `total_price = Σ price × qty`.
- Simulates preparation with `time.sleep(1)`.
- Sets `status = COMPLETED`, adds `summary`, saves state.
- Appends a pipe-delimited row to `all_receipts.txt`.
- Marks empty orders as `CANCELLED`.

### 6.5 `02-Full-Project/cpp/receipt.cpp`

- Reads `simulation_state.json` with `std::ifstream`.
- Robust `extractString` / `extractNumber` helpers (comma-safe, quote-aware).
- **Customer path:** prints the receipt, then replaces
  `"status": "COMPLETED"` with `"status": "ARCHIVED"` in place.
- **Admin path:** loads `all_receipts.txt` into `std::vector<Receipt*>`
  (heap allocation with `new`), prints via `const Receipt*`, then `delete`s
  every pointer to avoid leaks.
- Fixes from the consultation draft:
  - corrected the `rPtrPtr` typo,
  - added `#include <cstdlib>` for `atoi`/`atof`,
  - pipe-delimited ledger parsing,
  - in-place status replacement instead of rebuilding the JSON (preserves
    `items` and `summary`).

### 6.6 Orchestrators

- `run.bat` — Windows: `javac` → `g++` → `java` → `python` → `receipt.exe`.
- `run.sh` — Linux/macOS equivalent.

---

## 7. How to run

> See `INSTRUCTIONS.MD` for the complete run guide, including the manual
> Java -> Python -> C++ command sequence.

### Java only

```bat
cd 01-Java-Only
run.bat
```

or manually:

```bat
javac -d out src\Main.java
java -cp out Main
```

### Full project (Windows)

```bat
cd 02-Full-Project
run.bat
```

### Full project (Linux/macOS)

```bash
cd 02-Full-Project
chmod +x run.sh
./run.sh
```

### Test Python and C++ alone (without Java)

```bat
cd 02-Full-Project
python python\engine.py
receipt.exe
```

The seed `simulation_state.json` is `PENDING`, so Python will price it,
append a ledger row, and C++ will print the receipt and archive it.

### Admin flow

1. Start the Java CLI and choose `[2] Admin`.
2. Use the existing Customer Record Module to view/update accounts.
3. Exit the CLI.
4. `StateBridge.writeAdminView()` writes an `ADMIN` state.
5. Python skips the simulation.
6. C++ prints the master ledger from `all_receipts.txt`.

### Windows Python note

If `python` opens the Microsoft Store instead of running, use `py` or the real
interpreter path, for example:

```bat
"C:\Users\<user>\AppData\Local\Python\bin\python.exe" python\engine.py
```

Update `run.bat` to match the machine's interpreter.

---

## 8. Integration checklist / TODO

- [ ] **Admin menu customization (CRUD on menu items).** The Java admin can
      currently view/update customer accounts and trigger the C++ ledger, but
      editing menu items (add/edit/delete dish, change price) is not wired yet.
      Recommended: a `menu.txt` file managed by Java, with Python reading the
      same file for pricing.
- [ ] **Unify billing.** Java `billout` still writes its own receipt. Decide
      whether payment/change live in Java (write them into the state file) or
      in Python/C++. Avoid two receipt sources.
- [ ] **Role-based routing.** Currently the admin path reuses the customer NAV
      menu. Split the Java menus into `CustomerMenu` and `AdminMenu`.
- [ ] **Inventory.** Add stock counts to `menu.txt` and decrement them in
      Python; reject orders that exceed stock.
- [ ] **Schema validation.** Validate `simulation_state.json` before each stage
      (required keys, numeric ranges).
- [ ] **Keep filenames stable.** All three languages hard-code
      `simulation_state.json` and `all_receipts.txt`; do not rename them
      without updating every stage.
- [ ] **Cleanup.** `Customer Receipt <ID>.txt` and `BKPCRecords.txt` are
      leftovers from the baseline; decide whether to keep or remove them.

---

## 9. Known caveats

- The Java-only folder reads/writes its data files **relative to the current
  working directory**. Run it from inside `01-Java-Only/`.
- The full project expects to be launched from `02-Full-Project/`; the
  orchestrators `cd` there automatically.
- `all_receipts.txt` is append-only. Re-running the same seed adds another row;
  this is expected. Delete the file (or keep the seed row) to reset.
- The pipeline is strictly sequential; no file locking is needed.
- C++ uses `std::atoi`/`std::atof`, which are fine for this controlled format.

---

## 10. Appendix: consultation reference

The following is the architecture that was consulted (Gemini) and then adapted
into this repository.

### 10.1 Shared-State File Pattern

> Instead of running simultaneously, the three languages execute sequentially
> in a loop managed by a shell script (or one language acting as master
> controller). They pass state forward through a temporary file
> (`simulation_state.json`).
>
> ```text
> [User Input] ──> 1. JAVA CLI (reads history -> appends new order)
>                          │ (saves state)
>                          ▼
>                    2. PYTHON ENGINE (reads file -> simulation/inventory)
>                          │ (saves updated state)
>                          ▼
>                    3. C++ RECEIPT (reads file -> prints receipt/ledger)
> ```

### 10.2 Original Java sketch

```java
import java.io.*;

public class OrderCli {
    public static void main(String[] args) {
        File file = new File("simulation_state.json");
        String existingData = "{}";

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                existingData = sb.toString();
            } catch (IOException e) {
                System.out.println("Error reading state file.");
            }
        }

        try (BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("=== POLYGLOT ORDERING SYSTEM ===");
            System.out.print("Enter item name to order: ");
            String itemName = sysIn.readLine();

            System.out.print("Enter quantity: ");
            int quantity = Integer.parseInt(sysIn.readLine());

            String updatedJson = String.format(
                "{\"last_order\": \"%s\", \"qty\": %d, \"status\": \"PENDING\"}",
                itemName, quantity
            );

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(updatedJson);
            }
            System.out.println("[Java] Order captured successfully.");

        } catch (Exception e) {
            System.out.println("Invalid input.");
        }
    }
}
```

### 10.3 Original Python sketch

```python
import json
import time

def process_simulation():
    filename = "simulation_state.json"

    try:
        with open(filename, "r") as file:
            state = json.load(file)
    except FileNotFoundError:
        print("[Python] No state file found to process.")
        return

    print(f"[Python] Simulating kitchen preparation for: {state['last_order']} x{state['qty']}...")
    time.sleep(1.5)

    base_price = 150.00
    state["total_price"] = base_price * state["qty"]
    state["status"] = "COMPLETED"

    with open(filename, "w") as file:
        json.dump(state, file, indent=4)

    print("[Python] Calculation complete. State updated.")

if __name__ == "__main__":
    process_simulation()
```

### 10.4 Original C++ receipt sketch

```cpp
#include <iostream>
#include <fstream>
#include <string>

std::string extractValue(const std::string& json, const std::string& key) {
    size_t keyPos = json.find("\"" + key + "\"");
    if (keyPos == std::string::npos) return "";

    size_t colonPos = json.find(":", keyPos);
    size_t valStart = json.find_first_not_of(" \"", colonPos + 1);
    size_t valEnd = json.find_first_of("\",}", valStart);

    return json.substr(valStart, valEnd - valStart);
}

int main() {
    std::string filename = "simulation_state.json";
    std::ifstream inFile(filename);

    if (!inFile.is_open()) {
        std::cout << "[C++] Error: No state file available.\n";
        return 1;
    }

    std::string fileContent = "";
    std::string line;
    while (std::getline(inFile, line)) {
        fileContent += line;
    }
    inFile.close();

    std::string lastOrder = extractValue(fileContent, "last_order");
    std::string qty = extractValue(fileContent, "qty");
    std::string totalPrice = extractValue(fileContent, "total_price");
    std::string status = extractValue(fileContent, "status");

    if (status == "COMPLETED") {
        std::cout << "\n=================================\n";
        std::cout << "        OFFICIAL RECEIPT         \n";
        std::cout << "=================================\n";
        std::cout << " Item ordered: " << lastOrder << "\n";
        std::cout << " Quantity:     " << qty << "\n";
        std::cout << " Total Amount: PHP " << totalPrice << "\n";
        std::cout << "=================================\n";
        std::cout << " Thank you for using our system!\n\n";

        std::ofstream outFile(filename);
        if (outFile.is_open()) {
            outFile << "{\n";
            outFile << "    \"last_order\": \"" << lastOrder << "\",\n";
            outFile << "    \"qty\": " << qty << ",\n";
            outFile << "    \"total_price\": " << totalPrice << ",\n";
            outFile << "    \"status\": \"ARCHIVED\"\n";
            outFile << "}";
            outFile.close();
            std::cout << "[C++] Receipt logged and state archived.\n";
        }
    } else {
        std::cout << "[C++] Order is not ready for a receipt (Status: " << status << ").\n";
    }

    return 0;
}
```

### 10.5 Original orchestrators

`run.sh`:

```bash
#!/bin/bash
g++ -std=c++11 receipt.cpp -o receipt
java OrderCli
python3 simulation.py
./receipt
```

`run.bat`:

```bat
@echo off
g++ -std=c++11 receipt.cpp -o receipt.exe
java OrderCli
python simulation.py
receipt.exe
```

### 10.6 Master Ledger Pattern (admin history)

> Instead of overwriting the state every run, Python acts as the permanent
> archiver. Every completed customer order is appended to
> `all_receipts.txt`. When an admin logs in through Java, C++ reads that
> history and prints the whole ledger using pointers.

Original C++ admin sketch (note: the published draft contained a `rPtrPtr`
typo; the implemented version in `cpp/receipt.cpp` fixes it):

```cpp
struct Receipt {
    std::string orderId;
    std::string itemName;
    int qty;
    double totalPrice;
};

void printReceiptDetails(const Receipt* rPtr) {
    if (rPtr == nullptr) return;
    std::cout << " Order ID:     " << rPtr->orderId << "\n";
    std::cout << " Item Name:    " << rPtr->itemName << "\n";
    std::cout << " Quantity:     " << rPtr->qty << "\n";
    std::cout << " Total Price:  PHP " << rPtr->totalPrice << "\n";
    std::cout << "---------------------------------\n";
}

// Admin branch
std::ifstream ledgerFile("all_receipts.txt");
std::vector<Receipt*> historicalReceipts;

std::string id, name;
int q;
double price;
while (ledgerFile >> id >> name >> q >> price) {
    Receipt* newReceipt = new Receipt{id, name, q, price};
    historicalReceipts.push_back(newReceipt);
}

for (size_t i = 0; i < historicalReceipts.size(); ++i) {
    printReceiptDetails(historicalReceipts[i]);
}

for (Receipt* ptr : historicalReceipts) {
    delete ptr;
}
historicalReceipts.clear();
```

Original Python archiver sketch:

```python
import json
import random

def archive_to_ledger():
    with open("simulation_state.json", "r") as file:
        state = json.load(file)

    if state.get("status") == "COMPLETED" and state.get("role") == "CUSTOMER":
        txn_id = f"TXN-{random.randint(1000, 9999)}"

        with open("all_receipts.txt", "a") as ledger:
            ledger.write(f"{txn_id} {state['last_order']} {state['qty']} {state['total_price']}\n")

if __name__ == "__main__":
    archive_to_ledger()
```

### 10.7 Why this architecture is academically strong

- **Java** demonstrates standard input handling, validation, and text-file
  CRUD with `BufferedReader`/`BufferedWriter`.
- **Python** demonstrates business rules, calculations, and simple file-based
  persistence without a database.
- **C++** demonstrates low-level file streams, manual parsing, structs,
  heap allocation (`new`/`delete`), and pointer traversal.
- The **orchestrator** makes the three languages cooperate while keeping each
  stage decoupled and independently testable.
