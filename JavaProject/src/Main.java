import java.io.*;
import java.util.*;

private static void createFile() {
    try {
        File createFile = new File("UserDetails.txt");
        if (createFile.createNewFile()) {
            System.out.println("File " + createFile.getName() + " is created successfully.");
        } else {
            System.out.println("File already exists in the directory.");
        }
    } catch (IOException exception) {
        System.out.println("An unexpected error has occurred.");
        exception.printStackTrace();
    }
}

private static void writeFile(int ID, String fullname, int age, String email, int contact) {
    try {
        FileWriter writeFile = new FileWriter("UserDetails.txt");
        writeFile.write("ID: " + ID + "\t");
        writeFile.write("Full Name: " + fullname + "\t");
        writeFile.write("Age: " + age + "\t");
        writeFile.write("Email Address: " + email + "\t");
        writeFile.write("Contact Number: " + contact);
        writeFile.close();
        System.out.println("Content has been successfully written.");
    } catch (IOException exception) {
        System.out.println("An unexpected error has occurred.");
        exception.printStackTrace();
    }
}

private static void searchRecord(int searchId) {
    try (Scanner scan = new Scanner(new File("UserDetails.txt"))) {
        boolean found = false;
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.contains("ID: " + searchId)) {
                System.out.println("Record found: " + line);
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("No record found with ID: " + searchId);
        }
    } catch (FileNotFoundException exception) {
        System.out.println("An unexpected error has occurred.");
        exception.printStackTrace();
    }
}

private static void updateRecord(int updateID, String updateName, int updateAge, String updateEmail, int updateContact) {
    File inputFile = new File("UserDetails.txt");
    File BackFile = new File("BackupUserDetails.txt");

    try (Scanner scan = new Scanner(inputFile);
         FileWriter writeFile = new FileWriter(BackFile)) {

        boolean updated = false;

        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.contains("ID: " + updateID)) {
                String updateLine = "ID: " + updateID + "\tFull Name: " + updateName +
                        "\tAge: " + updateAge + "\tEmail Address: " + updateEmail +
                        "\tContact Number: " + updateContact;
                writeFile.write(updateLine + "\n");
                System.out.println("Record with ID: " + updateID + " has been updated.");
                updated = true;
            } else {
                writeFile.write(line + "\n");
            }
        }

        if (!updated) {
            System.out.println("No record found with ID: " + updateID);
        }
    } catch (IOException exception) {
        System.out.println("Unexpected error has occurred.");
        exception.printStackTrace();
    }
    if (inputFile.delete() && BackFile.renameTo(inputFile)) {
        System.out.println("File update successful.");
    } else {
        System.out.println("Error occurred during update.");
    }
}

private static void deleteRecord(int deleteId) {
    File inputFile = new File("UserDetails.txt");
    File BackFile = new File("BackupUserDetails.txt");

    try (Scanner scan = new Scanner(inputFile);
         FileWriter writeFile = new FileWriter(BackFile)) {

        boolean deleted = false;

        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line.contains("ID: " + deleteId)) {
                System.out.println("Record with ID: " + deleteId + " has been deleted.");
                deleted = true;
            } else {
                writeFile.write(line + "\n");
            }
        }
        if (!deleted) {
            System.out.println("No record found with ID: " + deleteId);
        }
    } catch (IOException exception) {
        System.out.println("An unexpected error has occurred.");
        exception.printStackTrace();
    }

    if (inputFile.delete() && BackFile.renameTo(inputFile)) {
        System.out.println("File update successful.");
    } else {
        System.out.println("Error occurred during file update.");
    }
}

private static void readFile() {
    try {
        File readFile = new File("UserDetails.txt");
        Scanner scan = new Scanner(readFile);
        while (scan.hasNextLine()) {
            String dataFile = scan.nextLine();
            System.out.println(dataFile);
        }
        scan.close();
    } catch (FileNotFoundException exception) {
        System.out.println("An unexpected error has occurred.");
        exception.printStackTrace();
    }
}

public static void main (String [] args) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    boolean finish = false;

    while (!finish) {
        System.out.println("USER MENU");
        System.out.println("C - CREATE FILE");
        System.out.println("W - WRITE DATA TO FILE");
        System.out.println("S - SEARCH RECORD BY ID");
        System.out.println("U - UPDATE RECORD BY ID");
        System.out.println("D - DELETE RECORD BY ID");
        System.out.println("V - VIEW FILE");
        System.out.println("E - EXIT PROGRAM");

        char input = (char) br.read();
        br.readLine();

        switch (input) {
            case 'C': // CREATE
                createFile();
                break;

            case 'W': // WRITE
                System.out.println("Enter your ID: ");
                int ID = Integer.parseInt(br.readLine());

                System.out.println("Enter your full name: ");
                String fullname = br.readLine();

                System.out.println("Enter your age: ");
                int age = Integer.parseInt(br.readLine());

                System.out.println("Enter your email address: ");
                String email = br.readLine();

                System.out.println("Enter your contact number: ");
                int contact = Integer.parseInt(br.readLine());

                writeFile(ID, fullname, age, email, contact);
                break;

            case 'V':
                readFile();
                break;

            case 'S':
                System.out.println("Enter ID to search: ");
                int searchId = Integer.parseInt(br.readLine());
                searchRecord(searchId);
                break;

            case 'U':
                System.out.println("Enter ID: ");
                int updateId = Integer.parseInt(br.readLine());

                System.out.println("Enter the update for full name: ");
                String updatedName = br.readLine();

                System.out.println("Enter the update for age: ");
                int updatedAge = Integer.parseInt(br.readLine());

                System.out.println("Enter the update for email address: ");
                String updatedEmail = br.readLine();

                System.out.println("Enter the update for contact number: ");
                int updatedContact = Integer.parseInt(br.readLine());

                updateRecord(updateId, updatedName, updatedAge, updatedEmail, updatedContact);
                break;

            case 'D':
                System.out.println("Enter ID to delete: ");
                int deleteId = Integer.parseInt(br.readLine());
                deleteRecord(deleteId);
                break;

            case 'E': // EXIT
                System.out.println("Exiting program...");
                finish = true;
                break;

            default:
                System.out.println("Invalid input.");
        }

        if (finish) {
            break;
        }

        System.out.print("Continue? [Y/N]");
        char confirm = (char) br.read(); // Read the user input
        br.readLine(); // Consume the newline character after the input

        if (confirm == 'N') {
            finish = true;
            System.exit(0);
        } else if (confirm != 'Y') {
            System.out.println("Invalid input.");
        }
    }
}