package main;

import java.util.Scanner;

import auth.Authentication;
import queryHandler.Query;

/**
 * Main entry point for the Custom Multiuser DBMS.
 * - Displays all available options.
 * - Executes authentication and password management functions.
 */
public class Main {
    public static void main(String[] args) {
        Authentication auth = new Authentication();
        Query queryProcessor = new Query();
        Scanner scanner = new Scanner(System.in);

        // Display menu once
        System.out.println("\n=== Database Management System ===\n");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Forgot Password?");
        System.out.println("4. Exit");

        System.out.print("\nChoice: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                auth.registerUser();
                if (auth.login()) {
                    proceedToQueryProcessing(queryProcessor);
                }
                break;
            case "2":
                if (auth.login()) {
                    proceedToQueryProcessing(queryProcessor);
                }
                break;
            case "3":
                auth.recoverPassword();
                break;
            case "4":
                System.out.println("Exiting the Database Management System. Goodbye!");
                scanner.close();
                return;
            default:
                System.out.println("Invalid choice. Restart the program and select a valid option.");
        }

        scanner.close();
    }

    /**
     * Proceeds to query execution after successful login.
     *
     * @param queryProcessor The query processing system.
     */
    private static void proceedToQueryProcessing(Query queryProcessor) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nWelcome to the Database Management System.");
        System.out.println("Type SQL-like commands to interact with the database.");
        System.out.println("Type 'EXIT' to logout.\n");

        String query;
        while (true) {
            System.out.print("SQL> ");
            query = scanner.nextLine().trim();

            if (query.equalsIgnoreCase("EXIT")) {
                System.out.println("Logging out. Goodbye!");
                break;
            }

            queryProcessor.processQuery(query);
        }

        scanner.close();
    }
}
