import java.io.*;

/**
 * StateBridge is the Java <-> Python <-> C++ hand-off layer.
 *
 * Java writes the customer's order into simulation_state.json.
 * Python reads it, prices it, and marks it COMPLETED.
 * C++ reads it, prints the receipt, and marks it ARCHIVED.
 *
 * The file is intentionally flat except for the "items" array so that the
 * C++ side can read it with std::ifstream and simple string parsing.
 */
public class StateBridge {

    public static final String STATE_FILE = "simulation_state.json";

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static boolean writeOrder(int customerId,
                                     String role,
                                     double subtotal,
                                     Main.Category meals,
                                     Main.Category drinks,
                                     Main.Category desserts,
                                     Main.Category sides,
                                     Main.Category bundles) {

        StringBuilder items = new StringBuilder();
        Main.Category[] categories = {meals, drinks, desserts, sides, bundles};
        boolean first = true;

        for (Main.Category category : categories) {
            for (int i = 0; i < category.products.length; i++) {
                if (category.products[i] != null && !category.products[i].isEmpty()) {
                    if (!first) {
                        items.append(",\n");
                    }
                    items.append(String.format(
                            "    {\"name\": \"%s\", \"qty\": %d, \"price\": %.2f}",
                            escape(category.products[i]),
                            category.quantities[i],
                            category.prices[i]));
                    first = false;
                }
            }
        }

        String orderId = "TXN-" + (System.currentTimeMillis() % 10000);
        String json = "{\n" +
                "    \"order_id\": \"" + orderId + "\",\n" +
                "    \"customer_id\": " + customerId + ",\n" +
                "    \"role\": \"" + escape(role) + "\",\n" +
                "    \"items\": [\n" + items + "\n    ],\n" +
                "    \"subtotal\": " + String.format("%.2f", subtotal) + ",\n" +
                "    \"total_price\": 0.00,\n" +
                "    \"payment\": 0.00,\n" +
                "    \"change\": 0.00,\n" +
                "    \"status\": \"PENDING\"\n" +
                "}";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STATE_FILE))) {
            writer.write(json);
            System.out.println("[Java] Order written to " + STATE_FILE + " as " + orderId + ".");
            return true;
        } catch (IOException exception) {
            System.out.println("[Java] Could not write state file: " + exception.getMessage());
            return false;
        }
    }

    /**
     * Reads the shared state file with a BufferedReader.
     * Kept for the admin/next-run scenarios where Java needs to inspect state.
     */
    public static String readState() {
        File file = new File(STATE_FILE);
        if (!file.exists()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException exception) {
            System.out.println("[Java] Could not read state file: " + exception.getMessage());
        }
        return content.toString();
    }

    /**
     * Writes a minimal state file. Used when the session did not produce an
     * order (customer exit) or when an admin is viewing the ledger.
     */
    private static void writeMinimalState(String orderId, int customerId, String role, String status) {
        String json = "{\n" +
                "    \"order_id\": \"" + escape(orderId) + "\",\n" +
                "    \"customer_id\": " + customerId + ",\n" +
                "    \"role\": \"" + escape(role) + "\",\n" +
                "    \"items\": [],\n" +
                "    \"subtotal\": 0.00,\n" +
                "    \"total_price\": 0.00,\n" +
                "    \"payment\": 0.00,\n" +
                "    \"change\": 0.00,\n" +
                "    \"status\": \"" + escape(status) + "\"\n" +
                "}";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STATE_FILE))) {
            writer.write(json);
        } catch (IOException exception) {
            System.out.println("[Java] Could not write state file: " + exception.getMessage());
        }
    }

    /**
     * Records that the customer left without placing an order.
     *
     * Without this, a PENDING order left behind by an earlier run would still
     * be read by Python and printed by C++ (the "exit prints a receipt" bug).
     */
    public static void writeNoOrder(String role) {
        writeMinimalState("NONE", 0, role, "NO_ORDER");
        System.out.println("[Java] No order placed this session; nothing will be printed.");
    }

    /**
     * Writes a minimal admin state so the C++ ledger viewer knows what to show.
     */
    public static void writeAdminView() {
        writeMinimalState("ADMIN", 0, "ADMIN", "ADMIN_VIEW");
    }
}
