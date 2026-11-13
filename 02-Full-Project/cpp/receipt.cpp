#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <cstdlib>
#include <iomanip>

/**
 * C++ presentation layer for the polyglot Restaurant Ordering System.
 *
 * Reads simulation_state.json produced by Java + Python.
 * - CUSTOMER: prints the individual receipt and archives the state.
 * - ADMIN:    loads all_receipts.txt into heap-allocated Receipt structs and
 *             prints the master ledger using pointers.
 */

struct Receipt {
    std::string orderId;
    int customerId;
    std::string summary;
    double totalPrice;
};

static std::string trim(const std::string& value) {
    const std::string whitespace = " \t\r\n";
    size_t start = value.find_first_not_of(whitespace);
    if (start == std::string::npos) {
        return "";
    }
    size_t end = value.find_last_not_of(whitespace);
    return value.substr(start, end - start + 1);
}

static std::string readFile(const std::string& filename) {
    std::ifstream input(filename.c_str());
    if (!input.is_open()) {
        return "";
    }

    std::string content;
    std::string line;
    while (std::getline(input, line)) {
        content += line;
        content += "\n";
    }
    return content;
}

static std::string extractString(const std::string& json, const std::string& key) {
    size_t keyPos = json.find("\"" + key + "\"");
    if (keyPos == std::string::npos) {
        return "";
    }

    size_t colonPos = json.find(':', keyPos);
    size_t quoteStart = json.find('"', colonPos + 1);
    if (colonPos == std::string::npos || quoteStart == std::string::npos) {
        return "";
    }

    size_t quoteEnd = quoteStart + 1;
    while (quoteEnd < json.size()) {
        if (json[quoteEnd] == '\\') {
            quoteEnd += 2;
            continue;
        }
        if (json[quoteEnd] == '"') {
            break;
        }
        ++quoteEnd;
    }

    return json.substr(quoteStart + 1, quoteEnd - quoteStart - 1);
}

static std::string extractNumber(const std::string& json, const std::string& key) {
    size_t keyPos = json.find("\"" + key + "\"");
    if (keyPos == std::string::npos) {
        return "0";
    }

    size_t colonPos = json.find(':', keyPos);
    if (colonPos == std::string::npos) {
        return "0";
    }

    size_t valueStart = json.find_first_not_of(" \t\r\n", colonPos + 1);
    if (valueStart == std::string::npos) {
        return "0";
    }

    size_t valueEnd = json.find_first_of(",}\r\n", valueStart);
    return trim(json.substr(valueStart, valueEnd - valueStart));
}

static void printReceiptDetails(const Receipt* receipt) {
    if (receipt == nullptr) {
        return;
    }

    std::cout << " Order ID:     " << receipt->orderId << "\n";
    std::cout << " Customer ID:  " << receipt->customerId << "\n";
    std::cout << " Items:        " << receipt->summary << "\n";
    std::cout << " Total Price:  PHP " << std::fixed << std::setprecision(2)
              << receipt->totalPrice << "\n";
    std::cout << "---------------------------------\n";
}

static void printAdminLedger() {
    std::cout << "\n=================================\n";
    std::cout << "   ADMIN PANEL: MASTER LEDGER    \n";
    std::cout << "=================================\n";

    std::ifstream ledgerFile("all_receipts.txt");
    if (!ledgerFile.is_open()) {
        std::cout << " No historical receipts found.\n";
        return;
    }

    std::vector<Receipt*> historicalReceipts;
    std::string line;

    while (std::getline(ledgerFile, line)) {
        if (trim(line).empty()) {
            continue;
        }

        std::stringstream row(line);
        std::string orderId;
        std::string customerIdText;
        std::string summary;
        std::string totalText;

        std::getline(row, orderId, '|');
        std::getline(row, customerIdText, '|');
        std::getline(row, summary, '|');
        std::getline(row, totalText, '|');

        Receipt* receipt = new Receipt();
        receipt->orderId = orderId;
        receipt->customerId = std::atoi(customerIdText.c_str());
        receipt->summary = summary;
        receipt->totalPrice = std::atof(totalText.c_str());

        historicalReceipts.push_back(receipt);
    }
    ledgerFile.close();

    if (historicalReceipts.empty()) {
        std::cout << " Ledger is empty.\n";
        return;
    }

    for (size_t i = 0; i < historicalReceipts.size(); ++i) {
        printReceiptDetails(historicalReceipts[i]);
    }

    size_t totalOrders = historicalReceipts.size();

    for (size_t i = 0; i < historicalReceipts.size(); ++i) {
        delete historicalReceipts[i];
    }
    historicalReceipts.clear();

    std::cout << " Total archived orders: " << totalOrders << "\n";
}

static bool archiveState(const std::string& filename, const std::string& content) {
    size_t keyPos = content.find("\"status\"");
    if (keyPos == std::string::npos) {
        return false;
    }

    size_t colonPos = content.find(':', keyPos);
    if (colonPos == std::string::npos) {
        return false;
    }

    size_t valueStart = content.find('"', colonPos + 1);
    if (valueStart == std::string::npos) {
        return false;
    }

    size_t valueEnd = content.find('"', valueStart + 1);
    if (valueEnd == std::string::npos) {
        return false;
    }

    std::string updated = content;
    updated.replace(valueStart + 1, valueEnd - valueStart - 1, "ARCHIVED");

    std::ofstream output(filename.c_str());
    if (!output.is_open()) {
        return false;
    }
    output << updated;
    return true;
}

int main() {
    const std::string stateFile = "simulation_state.json";
    std::string state = readFile(stateFile);

    if (state.empty()) {
        std::cout << "[C++] No state file available.\n";
        return 1;
    }

    std::string role = extractString(state, "role");
    std::string status = extractString(state, "status");

    if (role == "ADMIN") {
        printAdminLedger();
        return 0;
    }

    if (status == "NO_ORDER") {
        std::cout << "[C++] No order was placed in this session. No receipt to print.\n";
        return 0;
    }

    if (status != "COMPLETED") {
        std::cout << "[C++] Order is not ready for a receipt (Status: " << status << ").\n";
        return 0;
    }

    Receipt currentOrder;
    currentOrder.orderId = extractString(state, "order_id");
    currentOrder.customerId = std::atoi(extractNumber(state, "customer_id").c_str());
    currentOrder.summary = extractString(state, "summary");
    currentOrder.totalPrice = std::atof(extractNumber(state, "total_price").c_str());

    Receipt* receiptPointer = &currentOrder;

    std::cout << "\n=================================\n";
    std::cout << "        OFFICIAL RECEIPT         \n";
    std::cout << "=================================\n";
    printReceiptDetails(receiptPointer);
    std::cout << " Thank you for using our system!\n\n";

    if (archiveState(stateFile, state)) {
        std::cout << "[C++] Receipt logged and state archived.\n";
    } else {
        std::cout << "[C++] Could not archive the state file.\n";
    }

    return 0;
}
