//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

import java.io.*;
import java.util.*;

public class Main {
    public static String CURRENT_ROLE = "CUSTOMER";

    public static void main(String[] args) throws IOException {
        boolean session = false;
        Scanner scanner = new Scanner(System.in);
        int ID = 0;

        System.out.println("SELECT USER TYPE");
        System.out.println("[1] Customer");
        System.out.println("[2] Admin");
        while (true) {
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            if (choice.equals("1")) {
                CURRENT_ROLE = "CUSTOMER";
                break;
            }
            if (choice.equals("2")) {
                CURRENT_ROLE = "ADMIN";
                break;
            }
            System.out.println("Please enter [1] for Customer or [2] for Admin.");
        }
        System.out.println("Logged in as: " + CURRENT_ROLE + "\n");

        // CATEGORY INITIALIZE + DATA STORAGE
        Category Meals = new Category(5);
        Category Drinks = new Category(5);
        Category Desserts = new Category(5);
        Category SideDishes = new Category(5);
        Category Bundles = new Category(5);

        while (!session) {
            System.out.println("\n ----- NAV MENU ----- ");
            System.out.println("[A] Create Customer Record");
            System.out.println("[B] Create Reservation Record");
            System.out.println("[C] Bill Out Module\n");
            System.out.println("[X] Exit System");

            char input = readChoice(scanner);

            switch (input) {

                case 'A':
                    // Customer Record Module
                    boolean crm = true;
                    while (crm) {
                        System.out.println("\nCUSTOMER RECORD MODULE");
                        System.out.println("[A] Create Customer Record");
                        System.out.println("[B] Update Customer Record");
                        System.out.println("[C] View Customer Record\n");
                        System.out.println("[X] Return to Main Menu");

                        char chooseA = readChoice(scanner);
                        switch (chooseA) {
                            case 'A':
                                // Create Customer Record
                                ID = readInt(scanner, "Enter personal ID number: ");

                                String Customer_Name = readText(scanner, "Enter name: ");

                                int Contact_No = readInt(scanner, "Enter contact number: ");

                                String DOR = readText(scanner, "Enter date of reservation (FORM MM/DD/YY): ");

                                String TOR = readText(scanner, "Enter time of reservation (FORM HH:MM): ");

                                int Guests = readInt(scanner, "Enter number of guests: ");

                                ccrecord(ID, Customer_Name, Contact_No, DOR, TOR, Guests);
                                break;
                            case 'B':
                                // Update Customer Record
                                ID = readInt(scanner, "Enter customer ID: ");

                                String updateName = readText(scanner, "Update customer's name: ");

                                int updateContact = readInt(scanner, "Update customer's contact number: ");

                                String updateDOR = readText(scanner, "Update the date of reservation (FORM MM/DD/YY): ");

                                String updateTOR = readText(scanner, "Update the time of reservation (FORM HH:MM): ");

                                int updateGuests = readInt(scanner, "Update the number of guests: ");

                                ucrecord(ID, updateName, updateContact, updateDOR, updateTOR, updateGuests);
                                break;
                            case 'C':
                                // View Customer Record
                                vcrecord();
                                break;
                            case 'X':
                                // Return to Main Menu
                                crm = false;
                                break;
                            default:
                                System.out.println("Invalid option.");
                        }
                    }
                    break;

                case 'B':
                    // Reservation Record Module
                    boolean rrm = true;
                    while (rrm) {
                        System.out.println("\nRESERVATION RECORD MODULE");
                        System.out.println("[A] Create Orders Record");
                        System.out.println("[B] View Orders Record\n");
                        System.out.println("[X] Return to Main Menu");

                        char chooseB = readChoice(scanner);
                        switch (chooseB) {
                            case 'A':
                                // Create Orders Record
                                ID = ordermenu(scanner, Meals, Drinks, Desserts, SideDishes, Bundles);
                                break;
                            case 'B':
                                // View Orders Record
                                vieworder();
                                break;
                            case 'X':
                                // Return to Main Menu
                                rrm = false;
                                break;
                            default:
                                System.out.println("Invalid option.");
                        }
                    }
                    break;

                case 'C':
                    // Bill Out Module
                    if (ID != 0) {
                        billout(Meals, Drinks, Desserts, SideDishes, Bundles, ID, scanner);
                    } else {
                        System.out.println("No matching ID, please register first.");
                    }
                    break;

                case 'X':
                    // Exit System
                    session = true;
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }

        System.out.println("Thank you for coming!");
        if (CURRENT_ROLE.equals("ADMIN")) {
            StateBridge.writeAdminView();
            System.out.println("[Java] Admin session saved. C++ will print the master ledger.");
        }
        scanner.close();
    }

    // INPUT HELPERS
    private static char readChoice(Scanner scanner) {
        while (true) {
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return Character.toUpperCase(line.charAt(0));
            }
            System.out.print("Please enter a choice: ");
        }
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid amount.");
            }
        }
    }

    private static String readText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    // HELPER FUNCTIONS
    private static double calculate(Category category, FileWriter writer) throws IOException {
        double total = 0.0;
        boolean orderedCategory = false;

        for (int i = 0; i < category.products.length; i++) {
            if (!category.products[i].isEmpty()) {
                double itemTotal = category.prices[i] * category.quantities[i];
                writer.write(String.format("%s x%d -> PHP %.2f\n",
                        category.products[i], category.quantities[i], itemTotal));
                total += itemTotal;
                orderedCategory = true;
            }
        }

        if (!orderedCategory) {
            writer.write("No items ordered in this category.\n");
        }
        return total;

    }
    private static void writeandsave(double totalAmount, Category Meals, Category Drinks, Category Desserts, Category SideDishes, Category Bundles, int ID) {
        try {
            FileWriter writer = new FileWriter("Customer_Orders.txt", true);

            writer.write("----->>>>> ORDERED ITEMS <<<<<-----");
            writer.write("\nCustomer ID number " + ID + " ordered: \n");
            writer.write("\n---- Meals ----\n");
            totalAmount += calculate(Meals, writer);
            writer.write("\n---- Drinks ----\n");
            totalAmount += calculate(Drinks, writer);
            writer.write("\n---- Desserts ----\n");
            totalAmount += calculate(Desserts, writer);
            writer.write("\n---- Side Dishes ----\n");
            totalAmount += calculate(SideDishes, writer);
            writer.write("\n---- Bundles ----\n");
            totalAmount += calculate(Bundles, writer);
            writer.write(String.format("\n\nTotal Amount: PHP %.2f\n", totalAmount));
            writer.write("\n=================================\n");

            writer.close();
            StateBridge.writeOrder(ID, CURRENT_ROLE, totalAmount, Meals, Drinks, Desserts, SideDishes, Bundles);
            System.out.println("Order received. \n");
        } catch (IOException exception) {
            System.out.println("Could not save the order: " + exception.getMessage());
        }
    }
    public static void orderMeals(Category Meals, int ID, Scanner scanner) {
        char order;
        int qty;
        double price;
        String product;

        System.out.println("\n <------> MEAL LIST <------> ");
        System.out.println("[A] Meal 1 -> Fried Chicken W/ Garlic Rice    (PHP 156.25)");
        System.out.println("[B] Meal 2 -> Pasta and Wings W/ Garlic Bread (PHP 184.60)");
        System.out.println("[C] Meal 3 -> Steak and Fries W/ Garlic Bread (PHP 450.00)");
        System.out.print("Choose your order: ");
        order = readChoice(scanner);

        // Determine product and price
        if (order == 'A') {
            product = "Meal 1 -> Fried Chicken W/ Garlic Rice";
            price = 156.25;
        } else if (order == 'B') {
            product = "Meal 2 -> Pasta and Wings W/ Garlic Bread";
            price = 184.60;
        } else if (order == 'C') {
            product = "Meal 3 -> Steak and Fries W/ Garlic Bread";
            price = 450.00;
        } else {
            System.out.println("Invalid option");
            return;
        }

        System.out.println(product + " - " + price);
        qty = readInt(scanner, "How many? ");
        if (qty <= 0) {
            System.out.println("Quantity must be at least 1.");
            return;
        }

        // Count total orders
        int totalOrders = 0;
        for (int i = 0; i < Meals.products.length; i++) {
            if (!Meals.products[i].isEmpty()) {
                totalOrders += Meals.quantities[i]; // Count only the quantities of existing orders
            }
        }

        // Check if limit is exceeded
        if (totalOrders + qty > 5) {
            System.out.println("You can only order a maximum of 5 items in total.");
            return;
        }

        // DATA STORAGE FOR MEALS
        for (int i = 0; i < Meals.products.length; i++) {
            if (Meals.products[i].isEmpty()) {
                Meals.products[i] = product;
                Meals.prices[i] = price;
                Meals.quantities[i] = qty;
                break;
            }
        }

        System.out.println("Customer ID: " + ID + " ordered MEALS: " + product + " x" + qty);
    }
    public static void orderDrinks(Category Drinks, int ID, Scanner scanner) {
        char order;
        int qty;
        double price;
        String product;

        System.out.println("\n <----> DRINKS LIST <----> ");
        System.out.println("[A] Sprite    (PHP 40.00)");
        System.out.println("[B] Juice     (PHP 20.00)");
        System.out.println("[C] Coffee    (PHP 30.00)");
        System.out.print("Choose your order: ");
        order = readChoice(scanner);

        // Determine product and price
        if (order == 'A') {
            product = "Sprite";
            price = 40.00;
        } else if (order == 'B') {
            product = "Juice";
            price = 20.00;
        } else if (order == 'C') {
            product = "Coffee";
            price = 30.00;
        } else {
            System.out.println("Invalid Option");
            return;
        }

        System.out.println(product + " - PHP " + price);
        qty = readInt(scanner, "How many? ");
        if (qty <= 0) {
            System.out.println("Quantity must be at least 1.");
            return;
        }

        // Count total orders
        int totalOrders = 0;
        for (int i = 0; i < Drinks.products.length; i++) {
            if (!Drinks.products[i].isEmpty()) {
                totalOrders += Drinks.quantities[i]; // Count only the quantities of existing orders
            }
        }

        // Check if limit is exceeded
        if (totalOrders + qty > 5) {
            System.out.println("You can only order a maximum of 5 items in total.");
            return;
        }

        // DATA STORAGE FOR DRINKS
        for (int i = 0; i < Drinks.products.length; i++) {
            if (Drinks.products[i].isEmpty()) {
                Drinks.products[i] = product;
                Drinks.prices[i] = price;
                Drinks.quantities[i] = qty;
                break;
            }
        }

        System.out.println("Customer ID: " + ID + " ordered DRINKS: " + product + " x" + qty);
    }
    public static void orderDesserts(Category Desserts, int ID, Scanner scanner) {
        char order;
        int qty;
        double price;
        String product;

        System.out.println("\n <----> DESSERTS LIST <----> ");
        System.out.println("[A] Tiramisu    (PHP 60.00)");
        System.out.println("[B] Ice Cream   (PHP 20.00)");
        System.out.println("[C] Cheesecake  (PHP 90.00)");
        System.out.print("Choose your order: ");
        order = readChoice(scanner);

        // Determine product and price
        if (order == 'A') {
            product = "Tiramisu";
            price = 60.00;
        } else if (order == 'B') {
            product = "Ice Cream";
            price = 20.00;
        } else if (order == 'C') {
            product = "Cheesecake";
            price = 90.00;
        } else {
            System.out.println("Invalid Option.");
            return;
        }

        System.out.println(product + " - PHP " + price);
        qty = readInt(scanner, "How many? ");
        if (qty <= 0) {
            System.out.println("Quantity must be at least 1.");
            return;
        }

        // Count total orders
        int totalOrders = 0;
        for (int i = 0; i < Desserts.products.length; i++) {
            if (!Desserts.products[i].isEmpty()) {
                totalOrders += Desserts.quantities[i]; // Count only the quantities of existing orders
            }
        }

        // Check if limit is exceeded
        if (totalOrders + qty > 5) {
            System.out.println("You can only order a maximum of 5 items in total.");
            return;
        }

        // DATA STORAGE FOR DESSERTS
        for (int i = 0; i < Desserts.products.length; i++) {
            if (Desserts.products[i].isEmpty()) {
                Desserts.products[i] = product;
                Desserts.prices[i] = price;
                Desserts.quantities[i] = qty;
                break;
            }
        }

        System.out.println("Customer ID: " + ID + " ordered DESSERTS: " + product + " x" + qty);
    }
    public static void orderSideDish(Category SideDishes, int ID, Scanner scanner) {
        char order;
        int qty;
        double price;
        String product;

        System.out.println("\n <----> SIDE DISHES LIST <----> ");
        System.out.println("[A] Coleslaw        (PHP 25.00)");
        System.out.println("[B] Mashed Potato   (PHP 35.00)");
        System.out.println("[C] Buttered Corn   (PHP 25.00)");
        System.out.print("Choose your order: ");
        order = readChoice(scanner);

        // Determine product and price
        if (order == 'A') {
            product = "Coleslaw";
            price = 25.00;
        } else if (order == 'B') {
            product = "Mashed Potato";
            price = 35.00;
        } else if (order == 'C') {
            product = "Buttered Corn";
            price = 25.00;
        } else {
            System.out.println("Invalid Option.");
            return;
        }

        System.out.println(product + " - PHP " + price);
        qty = readInt(scanner, "How many? ");
        if (qty <= 0) {
            System.out.println("Quantity must be at least 1.");
            return;
        }

        // Count total orders
        int totalOrders = 0;
        for (int i = 0; i < SideDishes.products.length; i++) {
            if (!SideDishes.products[i].isEmpty()) {
                totalOrders += SideDishes.quantities[i]; // Count only the quantities of existing orders
            }
        }

        // Check if limit is exceeded
        if (totalOrders + qty > 5) {
            System.out.println("You can only order a maximum of 5 items in total.");
            return;
        }

        // DATA STORAGE FOR SIDE DISHES
        for (int i = 0; i < SideDishes.products.length; i++) {
            if (SideDishes.products[i].isEmpty()) {
                SideDishes.products[i] = product;
                SideDishes.prices[i] = price;
                SideDishes.quantities[i] = qty;
                break;
            }
        }

        System.out.println("Customer ID: " + ID + " ordered SIDE DISHES: " + product + " x" + qty);
    }
    public static void orderBundle(Category Bundles, int ID, Scanner scanner) {
        char order;
        int qty;
        double price;
        String product;

        System.out.println("\n <----> BUNDLES LIST <----> ");
        System.out.println("[A] Bundle 1 -> Meal 1, Juice, Tiramisu, & Coleslaw         (PHP 380.60)");
        System.out.println("[B] Bundle 2 -> Meal 2, Sprite, Ice Cream, & Buttered Corn  (PHP 340.60)");
        System.out.println("[C] Bundle 3 -> Meal 3, Coffee, Cheesecake, & Mashed Potato (PHP 600.00)");
        System.out.print("Choose your order: ");
        order = readChoice(scanner);

        // Determine product and price
        if (order == 'A') {
            product = "Bundle 1 -> Meal 1, Juice, Tiramisu, & Coleslaw";
            price = 380.60;
        } else if (order == 'B') {
            product = "Bundle 2 -> Meal 2, Sprite, Ice Cream, & Buttered Corn";
            price = 340.60;
        } else if (order == 'C') {
            product = "Meal 3, Coffee, Cheesecake, & Mashed Potato";
            price = 600.00;
        } else {
            System.out.println("Invalid Option.");
            return;
        }

        System.out.println(product + " - PHP " + price);
        qty = readInt(scanner, "How many? ");
        if (qty <= 0) {
            System.out.println("Quantity must be at least 1.");
            return;
        }

        // Count total orders
        int totalOrders = 0;
        for (int i = 0; i < Bundles.products.length; i++) {
            if (!Bundles.products[i].isEmpty()) {
                totalOrders += Bundles.quantities[i]; // Count only the quantities of existing orders
            }
        }

        // Check if limit is exceeded
        if (totalOrders + qty > 5) {
            System.out.println("You can only order a maximum of 5 items in total.");
            return;
        }

        // DATA STORAGE FOR BUNDLES
        for (int i = 0; i < Bundles.products.length; i++) {
            if (Bundles.products[i].isEmpty()) {
                Bundles.products[i] = product;
                Bundles.prices[i] = price;
                Bundles.quantities[i] = qty;
                break;
            }
        }

        System.out.println("Customer ID: " + ID + " ordered BUNDLES: " + product + " x" + qty);
    }
    public static class Category {
        String[] products;
        double[] prices;
        int[] quantities;

        public Category(int size) {
            this.products = new String[size];
            this.prices = new double[size];
            this.quantities = new int[size];

            for (int i = 0; i < size; i++) {
                this.products[i] = "";
            }
        }
    }
    public static class IDValidator {
        static boolean validateID(int ID) {
            File records = new File("Customer_Records.txt");

            try (Scanner scan = new Scanner(records)) {
                while (scan.hasNextLine()) {
                    String check = scan.nextLine().trim();
                    if (check.equals("ID: " + ID)) {
                        return true;
                    }
                }
            } catch (FileNotFoundException e) {
                System.out.println("No customer records found yet. Please create a customer record first.");
            }
            return false;
        }
    }

    // OPTION A -> Customer Record Module FUNCTIONS
    // [A]
    public static void ccrecord(int ID, String Customer_Name, int Contact_No, String DOR, String TOR, int Guests) {
        try {
            FileWriter writeFile = new FileWriter("Customer_Records.txt", true);
            writeFile.write("ID: " + ID + "\n");
            writeFile.write("Name: " + Customer_Name + "\n");
            writeFile.write("Contact: " + Contact_No + "\n");
            writeFile.write("Reservation Date: " + DOR + "\n");
            writeFile.write("Reservation Time: " + TOR + "\n");
            writeFile.write("Number of Guests: " + Guests + "\n");
            writeFile.write("\n=================================\n");
            writeFile.close();
        } catch (IOException exception) {
            System.out.println("Could not save the customer record: " + exception.getMessage());
        }
    }
    // [B]
    public static void ucrecord(int ID, String updateCustomer_Name, int updateContact, String updateDOR, String updateTOR, int updateGuests) {
        File updateFile = new File("Customer_Records.txt");
        File backupFile = new File("BKPCRecords.txt");

        boolean match = false;

        try (Scanner scan = new Scanner(updateFile);
             FileWriter writeFile = new FileWriter(backupFile)) {

            while (scan.hasNextLine()) {
                String line = scan.nextLine();

                if (line.trim().equals("ID: " + ID)) {
                    String updateRecord = "ID: " + ID + "\n" +
                            "Name: " + updateCustomer_Name + "\n" +
                            "Contact: " + updateContact + "\n" +
                            "Reservation Date: " + updateDOR + "\n" +
                            "Reservation Time: " + updateTOR + "\n" +
                            "Number of Guests: " + updateGuests + "\n" +
                            "\n=================================\n";

                    writeFile.write(updateRecord + "\n");
                    System.out.println("Customer record ID number " + ID + " has been updated.");
                    match = true;

                    while (scan.hasNextLine()) {
                        String separator = scan.nextLine();
                        if (separator.trim().equals("=================================")) {
                            break;
                        }
                    }
                } else {
                    writeFile.write(line + "\n");
                }
            }

            if (!match) {
                System.out.println("No customer record found with the ID number: " + ID + ".");
            }

        } catch (IOException exception) {
            System.out.println("Could not update the customer record: " + exception.getMessage());
        }
        if (updateFile.delete() && backupFile.renameTo(updateFile)) {
            if (match) {
                System.out.println("Record successfully updated.");
            } else {
                System.out.println("Unexpected error occurred.");
            }
        }
    }
    // [C]
    public static void vcrecord() {
        try {
            File readFile = new File("Customer_Records.txt");
            Scanner scan = new Scanner(readFile);
            while (scan.hasNextLine()) {
                String dataFile = scan.nextLine();
                System.out.println(dataFile);
            }
            scan.close();
        } catch (FileNotFoundException exception) {
            System.out.println("No customer records found yet. Create one with [A] first.");
        }
    }

    // OPTION B -> Reservation Record Module FUNCTIONS
    // [A]
    public static int ordermenu(Scanner scanner, Category Meals, Category Drinks, Category Desserts, Category SideDishes, Category Bundles) {

        double totalAmount = 0.0;
        boolean finalorder = false;
        char input;
        boolean validID = false;
        int ID = 0;

        while (!validID) {
            ID = readInt(scanner, "Please enter your customer ID: ");

            validID = IDValidator.validateID(ID);

            if (!validID) {
                System.out.println("Invalid customer ID.");
            } else {
                System.out.println("Proceeding to menu.");
            }

        }

        do {
            System.out.println("\n<<<<<------ MENU ------>>>>> ");
            System.out.println("[A] Meals");
            System.out.println("[B] Drinks");
            System.out.println("[C] Desserts");
            System.out.println("[D] Side Dishes");
            System.out.println("[E] Bundles \n");

            System.out.println("[X] Return to Main Menu");
            input = readChoice(scanner);

            switch (input) {
                case 'A':
                    orderMeals(Meals, ID, scanner);
                    break;
                case 'B':
                    orderDrinks(Drinks, ID, scanner);
                    break;
                case 'C':
                    orderDesserts(Desserts, ID, scanner);
                    break;
                case 'D':
                    orderSideDish(SideDishes, ID, scanner);
                    break;
                case 'E':
                    orderBundle(Bundles, ID, scanner);
                    break;

                case 'X':
                    finalorder = true;
                    writeandsave(totalAmount, Meals, Drinks, Desserts, SideDishes, Bundles, ID);
                    break;
                default:
                    System.out.println("Invalid Option.");
            }
        } while (!finalorder);

        return ID;
    }
    // [B]
    private static void vieworder() {
        try {
            File readFile = new File("Customer_Orders.txt");
            Scanner scan = new Scanner(readFile);
            while (scan.hasNextLine()) {
                String dataFile = scan.nextLine();
                System.out.println(dataFile);
            }
            scan.close();
        } catch (FileNotFoundException exception) {
            System.out.println("No customer orders found yet.");
        }
    }

    // OPTION C -> Bill Out Module FUNCTIONS
    // [C]
    public static void billout(Category Meals, Category Drinks, Category Desserts, Category SideDishes, Category Bundles, int ID, Scanner scanner) {

        boolean validator = false;
        double totalam = 0.0;
        double payment;
        double change;

        try (FileWriter writer = new FileWriter("Customer Receipt " + ID + ".txt")) {

            writer.write("\n------>>>>> BILL OUT <<<<<------\n");
            writer.write("\nCustomer ID number " + ID + " ordered: \n");
            writer.write("\n---- Meals ----\n");
            totalam += calculate(Meals, writer);
            writer.write("\n---- Drinks ----\n");
            totalam += calculate(Drinks, writer);
            writer.write("\n---- Desserts ----\n");
            totalam += calculate(Desserts, writer);
            writer.write("\n---- Side Dishes ----\n");
            totalam += calculate(SideDishes, writer);
            writer.write("\n---- Bundles ----\n");
            totalam += calculate(Bundles, writer);
            writer.write("\n=================================\n");

            writer.write(String.format("\nTotal amount due: PHP %.2f\n", totalam));
            System.out.printf("\nTotal amount due: PHP %.2f\n", totalam);

            while (!validator) {
                payment = readDouble(scanner, "\nEnter payment amount: PHP ");
                if (payment >= totalam) {
                    validator = true;
                    change = payment - totalam;
                    System.out.printf("\nChange: PHP %.2f\n", change);
                    System.out.println("Thank you for ordering!\n");

                    writer.write(String.format("Payment: PHP %.2f\n", payment));
                    writer.write(String.format("Change: PHP %.2f\n", change));
                    writer.write("\nThank you for ordering!\n");
                } else {
                    System.out.println("Insufficient payment. Please enter a valid payment amount.\n");
                }
            }
        } catch (IOException exception) {
            System.out.println("Could not write the receipt file: " + exception.getMessage());
        }
    }


}