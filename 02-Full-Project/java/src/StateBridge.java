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

    public static void writeOrder(int customerId,
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
        } catch (IOException exception) {
            System.out.println("[Java] Could not write state file: " + exception.getMessage());
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
     * Writes a minimal admin state so the C++ ledger viewer knows what to show.
     */
    public static void writeAdminView() {
        String json = "{\n" +
                "    \"order_id\": \"ADMIN\",\n" +
                "    \"customer_id\": 0,\n" +
                "    \"role\": \"ADMIN\",\n" +
                "    \"items\": [],\n" +
                "    \"subtotal\": 0.00,\n" +
                "    \"total_price\": 0.00,\n" +
                "    \"payment\": 0.00,\n" +
                "    \"change\": 0.00,\n" +
                "    \"status\": \"ADMIN_VIEW\"\n" +
                "}";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STATE_FILE))) {
            writer.write(json);
        } catch (IOException exception) {
            System.out.println("[Java] Could not write admin state: " + exception.getMessage());
        }
    }
}
