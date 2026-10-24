import java.util.Scanner;

public class OrderingSystem {

    // CALCULATION FUNCTIONS
    private static double totalAmount(int qty, double price) {
        return qty * price;
    }

    private static void change(double totalam) {
        Scanner scanner = new Scanner(System.in);
        double payment, change;

        System.out.println("\nTotal amount: PHP " + totalam);
        while (true) {
            System.out.print("Enter payment amount: PHP ");
            payment = scanner.nextDouble();
            if (payment >= totalam) {
                change = payment - totalam;
                System.out.println("Change: PHP " + change);
                System.out.println("Thank you!");
                break;
            } else {
                System.out.println("Insufficient payment. Please enter a valid payment amount.");
            }
        }
    }

    // ORDERING FUNCTIONS
    public static void orderMeals(double[] totalam, String[] prod_M, double[] price_M, int[] qty_M) {
        char order;
        int qty;
        double price;
        String product;
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n <------> MEAL LIST <------> ");
        System.out.println("[A] Meal 1 -> Fried Chicken W/ Garlic Rice    (PHP 156.25)");
        System.out.println("[B] Meal 2 -> Pasta and Wings W/ Garlic Bread (PHP 184.60)");
        System.out.println("[C] Meal 3 -> Steak and Fries W/ Garlic Bread (PHP 450.00)");
        System.out.print("Choose your order: ");
        order = scanner.next().charAt(0);

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
        System.out.print("How many? ");
        qty = scanner.nextInt();

        // Count total orders
        int totalOrders = 0;
        for (int i = 0; i < prod_M.length; i++) {
            if (!prod_M[i].isEmpty()) {
                totalOrders += qty_M[i]; // Count only the quantities of existing orders
            }
        }

        // Check if limit is exceeded
        if (totalOrders + qty > 5) {
            System.out.println("You can only order a maximum of 5 items in total.");
            return;
        }

        // DATA STORAGE FOR MEALS
        for (int i = 0; i < prod_M.length; i++) {
            if (prod_M[i].isEmpty()) {
                prod_M[i] = product;
                price_M[i] = price;
                qty_M[i] = qty;
                break;
            }
        }

        totalam[0] += totalAmount(qty, price);
        System.out.println("MEALS: " + product + " x" + qty + " -> PHP " + totalAmount(qty, price));
    }

    public static void orderDrinks(double[] totalam, String[] prod_D, double[] price_D, int[] qty_D) {
        char order;
        int qty;
        double price;
        String product;
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n <----> DRINKS LIST <----> ");
        System.out.println("[A] Sprite    (PHP 40.00)");
        System.out.println("[B] Juice     (PHP 20.00)");
        System.out.println("[C] Coffee    (PHP 30.00)");
        System.out.print("Choose your order: ");
        order = scanner.next().charAt(0);

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
        System.out.print("How many? ");
        qty = scanner.nextInt();

        // Count total orders
        int totalOrders = 0;
        for (int i = 0; i < prod_D.length; i++) {
            if (!prod_D[i].isEmpty()) {
                totalOrders += qty_D[i]; // Count only the quantities of existing orders
            }
        }

        // Check if limit is exceeded
        if (totalOrders + qty > 5) {
            System.out.println("You can only order a maximum of 5 items in total.");
            return;
        }

        // DATA STORAGE FOR DRINKS
        for (int i = 0; i < prod_D.length; i++) {
            if (prod_D[i].isEmpty()) {
                prod_D[i] = product;
                price_D[i] = price;
                qty_D[i] = qty;
                break;
            }
        }

        totalam[0] += totalAmount(qty, price);
        System.out.println("DRINKS: " + product + " x" + qty + " -> PHP " + totalAmount(qty, price));
    }

    public static void orderDesserts(double[] totalam, String[] prod_DT, double[] price_DT, int[] qty_DT) {
        char order;
        int qty;
        double price;
        String product;
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n <----> DESSERTS LIST <----> ");
        System.out.println("[A] Tiramisu    (PHP 60.00)");
        System.out.println("[B] Ice Cream   (PHP 20.00)");
        System.out.println("[C] Cheesecake  (PHP 90.00)");
        System.out.print("Choose your order: ");
        order = scanner.next().charAt(0);

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
        System.out.print("How many? ");
        qty = scanner.nextInt();

        // Count total orders
        int totalOrders = 0;
        for (int i = 0; i < prod_DT.length; i++) {
            if (!prod_DT[i].isEmpty()) {
                totalOrders += qty_DT[i]; // Count only the quantities of existing orders
            }
        }

        // Check if limit is exceeded
        if (totalOrders + qty > 5) {
            System.out.println("You can only order a maximum of 5 items in total.");
            return;
        }

        // DATA STORAGE FOR DESSERTS
        for (int i = 0; i < prod_DT.length; i++) {
            if (prod_DT[i].isEmpty()) {
                prod_DT[i] = product;
                price_DT[i] = price;
                qty_DT[i] = qty;
                break;
            }
        }

        totalam[0] += totalAmount(qty, price);
        System.out.println("DESSERTS: " + product + " x" + qty + " -> PHP " + totalAmount(qty, price));
    }

    public static void main(String[] args) {
        // INITIALIZE
        char input;
        double[] totalam = {0.0};
        boolean finalorder = false;

        // DATA STORAGE
        String[] prod_M = new String[5];
        double[] price_M = new double[5];
        int[] qty_M = new int[5];
        String[] prod_D = new String[5];
        double[] price_D = new double[5];
        int[] qty_D = new int[5];
        String[] prod_DT = new String[5];
        double[] price_DT = new double[5];
        int[] qty_DT = new int[5];

        // Initialize product arrays to empty strings
        for (int i = 0; i < 5; i++) {
            prod_M[i] = "";
            prod_D[i] = "";
            prod_DT[i] = "";
        }

        Scanner scanner = new Scanner(System.in);

        // MAIN MENU
        while (true) {
            System.out.println("<<<<<------ MENU ------>>>>> ");
            System.out.println("[A] Meal");
            System.out.println("[B] Drinks");
            System.out.println("[C] Dessert");
            System.out.println("[D] Payment");
            System.out.print("Order now: ");
            input = scanner.next().charAt(0);

            switch (input) {
                case 'A':
                    orderMeals(totalam, prod_M, price_M, qty_M);
                    break;
                case 'B':
                    orderDrinks(totalam, prod_D, price_D, qty_D);
                    break;
                case 'C':
                    orderDesserts(totalam, prod_DT, price_DT, qty_DT);
                    break;
                case 'D':
                    finalorder = true;
                    break;
                default:
                    System.out.println("Invalid Option.");
            }

            if (finalorder) {
                break;
            }

            System.out.print("Order more? [Y/N]: ");
            char confirm = scanner.next().charAt(0);
            if (confirm == 'N') {
                System.out.println("You may proceed to payment page");
                System.out.println("Thank you for ordering.\n");
                finalorder = true;
            } else if (confirm != 'Y') {
                System.out.println("Invalid input.");
            }
        }

        System.out.println("\n ---->>>>> ORDERED ITEMS <<<<<---- ");
        System.out.println("You Ordered: ");
        for (int i = 0; i < prod_M.length; i++) {
            if (!prod_M[i].isEmpty()) {
                System.out.println(prod_M[i] + " x" + qty_M[i] + " -> PHP " + totalAmount(qty_M[i], price_M[i]));
            }
        }
        for (int i = 0; i < prod_D.length; i++) {
            if (!prod_D[i].isEmpty()) {
                System.out.println(prod_D[i] + " x" + qty_D[i] + " -> PHP " + totalAmount(qty_D[i], price_D[i]));
            }
        }
        for (int i = 0; i < prod_DT.length; i++) {
            if (!prod_DT[i].isEmpty()) {
                System.out.println(prod_DT[i] + " x" + qty_DT[i] + " -> PHP " + totalAmount(qty_DT[i], price_DT[i]));
            }
        }

        // PROCESS PAYMENT
        change(totalam[0]);
    }
}

