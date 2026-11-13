"""Python simulation core for the polyglot Restaurant Ordering System.

Pipeline position: after Java, before C++.

Java writes simulation_state.json (status PENDING).
This script prices the order, simulates kitchen preparation, marks the order
COMPLETED, appends a row to all_receipts.txt, and saves the state back.
C++ then renders the receipt or the admin ledger.
"""

import json
import os
import random
import time

STATE_FILE = "simulation_state.json"
LEDGER_FILE = "all_receipts.txt"


def load_state():
    if not os.path.exists(STATE_FILE):
        print("[Python] No simulation_state.json found. Nothing to do.")
        return None

    try:
        with open(STATE_FILE, "r", encoding="utf-8") as file:
            data = json.load(file)
        if not isinstance(data, dict):
            print("[Python] State file is not a JSON object. Expected a single order.")
            return None
        return data
    except (json.JSONDecodeError, OSError) as error:
        print(f"[Python] Could not read state file: {error}")
        return None


def save_state(state):
    with open(STATE_FILE, "w", encoding="utf-8") as file:
        json.dump(state, file, indent=4)


def build_summary(items):
    parts = []
    for item in items:
        name = str(item.get("name", "Unknown item")).strip() or "Unknown item"
        parts.append(f"{name} x{item.get('qty', 0)}")
    return "; ".join(parts)


def archive_order(state):
    order_id = state.get("order_id") or f"TXN-{random.randint(1000, 9999)}"
    customer_id = state.get("customer_id", 0)
    summary = state.get("summary", "")
    total = float(state.get("total_price", 0.0))

    with open(LEDGER_FILE, "a", encoding="utf-8") as ledger:
        ledger.write(f"{order_id}|{customer_id}|{summary}|{total:.2f}\n")


def main():
    state = load_state()
    if state is None:
        # Missing file = nothing to do; unreadable file = real error.
        return 1 if os.path.exists(STATE_FILE) else 0

    role = str(state.get("role", "CUSTOMER")).upper()
    status = str(state.get("status", "")).upper()

    if role == "ADMIN":
        print("[Python] Admin session detected. Skipping order simulation.")
        return 0

    if status != "PENDING":
        print(f"[Python] Nothing to process (status: {status}).")
        return 0

    items = state.get("items", [])
    if not items:
        print("[Python] Order contains no items. Marking as CANCELLED.")
        state["status"] = "CANCELLED"
        save_state(state)
        return 0

    print("[Python] Simulating kitchen preparation...")
    time.sleep(1.0)

    total = 0.0
    for item in items:
        try:
            total += float(item.get("price", 0.0)) * int(item.get("qty", 0))
        except (TypeError, ValueError):
            print(f"[Python] Warning: skipped malformed item: {item}")

    state["order_id"] = state.get("order_id") or f"TXN-{random.randint(1000, 9999)}"
    state["summary"] = build_summary(items)
    state["total_price"] = round(total, 2)
    state["status"] = "COMPLETED"

    save_state(state)
    archive_order(state)

    print(f"[Python] Order {state['order_id']} priced at PHP {state['total_price']:.2f}.")
    print("[Python] Status set to COMPLETED and archived to all_receipts.txt.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
