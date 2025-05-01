package auth;

import storage.AuditLogger;

import java.io.Console;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Handles user authentication, registration, and two-factor authentication (Captcha).
 * - Supports user login with password verification and Captcha validation.
 * - Supports new user registration with security question and answer.
 * - Ensures user data is stored and retrieved properly.
 */
public class Authentication {
    private final UserManager userStorage = new UserManager(); // Manages user data storage
    private final PasswordManager pwdManager = new PasswordManager(); // Handles password hashing
    private final Scanner scanner = new Scanner(System.in); // User input scanner

    /**
     * Handles user login with password verification and Captcha authentication.
     * - If the user does not exist, prompts to register first.
     * - If the password is correct, prompts for Captcha validation.
     *
     * @return true if login is successful, false otherwise.
     */
    public boolean login() {
        try {
            System.out.print("User ID: ");
            String userId = scanner.nextLine();

            // Read password securely without displaying it
            String password = readPasswordSecurely("Password: ");

            // Load existing users from storage
            List<User> users = userStorage.loadUsers();
            User target = null;

            // Check if the user exists
            for (User user : users) {
                if (user.getUserId().equals(userId)) {
                    target = user;
                    break;
                }
            }

            // If user is not found, prompt to register
            if (target == null) {
                System.out.println("User not found! Please register first.");
                AuditLogger.logEvent(userId, "LOGIN_FAILED", "192.168.1.1"); // Simulated IP
                return false;
            }

            // Verify password
            if (!pwdManager.verifyPassword(password, target.getPassword())) {
                System.out.println("Invalid credentials!");
                AuditLogger.logEvent(userId, "LOGIN_FAILED", "192.168.1.1");
                return false;
            }

            // Captcha Verification
            System.out.println("\n--- CAPTCHA VERIFICATION ---");
            int start = getValidInput("Enter start (1-9): ", 1, 9);
            int increment = getValidInput("Enter increment (1-5): ", 1, 5);

            // Generate Captcha using user input
            CaptchaGenerator captcha = new CaptchaGenerator(start, increment);

            // Display the Captcha pattern
            System.out.println("Solve the pattern: " + captcha.getCaptchaQuestion());
            System.out.print("Enter the next number: ");
            int answer = scanner.nextInt();
            scanner.nextLine(); // Clear buffer

            // Validate Captcha
            if (captcha.validateCaptcha(answer)) {
                System.out.println("Login successful!");
                AuditLogger.logEvent(userId, "LOGIN_SUCCESS", "192.168.1.1");
                return true;
            } else {
                System.out.println("Incorrect Captcha! Try again.");
                AuditLogger.logEvent(userId, "LOGIN_FAILED_CAPTCHA", "192.168.1.1");
                return false;
            }

        } catch (IOException | NumberFormatException e) {
            System.out.println("Error during login: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets a valid integer input from the user within a specified range.
     * Ensures input is within the given range, otherwise prompts again.
     *
     * @param prompt The message to display to the user.
     * @param min The minimum valid value.
     * @param max The maximum valid value.
     * @return A valid integer within the specified range.
     */
    private int getValidInput(String prompt, int min, int max) {
        int input;
        while (true) {
            try {
                System.out.print(prompt);
                input = Integer.parseInt(scanner.nextLine());

                // Check if input is within the valid range
                if (input >= min && input <= max) {
                    return input;
                } else {
                    System.out.println("Error: Please enter a number between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Invalid input. Please enter a valid number.");
            }
        }
    }

    /**
     * Registers a new user in the system.
     * - Checks if the user ID already exists.
     * - Prompts for password, security question, and security answer.
     * - Hashes password and security answer before storing them.
     * - Saves the user in 'data/users.json' without overwriting existing users.
     */
    public void registerUser() {
        try {
            System.out.print("New User ID: ");
            String userId = scanner.nextLine();

            // Load existing users from storage
            List<User> users = userStorage.loadUsers();

            // Check if the user already exists
            for (User user : users) {
                if (user.getUserId().equals(userId)) {
                    System.out.println("User ID already exists! Please use a different ID.");
                    return; // Exit registration
                }
            }

            // Prompt for user credentials with masked password
            String password = readPasswordSecurely("Password: ");
            String hashedPassword = pwdManager.hashPassword(password); // Hash the password

            System.out.print("Security Question: ");
            String question = scanner.nextLine();

            // Mask security answer as well for additional security
            String answer = readPasswordSecurely("Security Answer: ");
            String hashedAnswer = pwdManager.hashPassword(answer); // Hash the security answer

            // Create a new User object
            User newUser = new User(userId, hashedPassword, question, hashedAnswer);

            // Save the new user to 'users.json'
            userStorage.saveUsers(newUser);

            System.out.println("Registration successful! Please login now.\n");

        } catch (IOException e) {
            System.out.println("Registration failed due to an error: " + e.getMessage());
        }
    }

    /**
     * Recovers a password by validating security question and auto-starting the reset process.
     */
    public void recoverPassword() {
        try {
            System.out.print("Enter your User ID: ");
            String userId = scanner.nextLine();

            // Load users
            List<User> users = userStorage.loadUsers();
            User target = null;

            // Find user
            for (User u : users) {
                if (u.getUserId().equals(userId)) {
                    target = u;
                    break;
                }
            }

            if (target == null) {
                System.out.println("User not found! Please register first.");
                return;
            }

            // Ask security question
            System.out.println("Security Question: " + target.getSecurityQuestion());
            String answer = readPasswordSecurely("Enter your answer: ");

            // Validate answer (hashed)
            if (!pwdManager.verifyPassword(answer, target.getSecurityAnswer())) {
                System.out.println("Incorrect answer! Password recovery failed.");
                return;
            }

            // Generate recovery token
            String token = pwdManager.generateRecoveryToken(userId);
            System.out.println("Password recovery successful! Use this token to reset your password: " + token);

            // Automatically proceed to password reset
            resetPassword(userId, token);

        } catch (IOException e) {
            System.out.println("Error during password recovery: " + e.getMessage());
        }
    }

    /**
     * Allows a user to reset their password using the recovery token.
     *
     * @param userId The user ID (auto-passed from recovery).
     * @param token The recovery token (auto-passed from recovery).
     */
    private void resetPassword(String userId, String token) {
        try {
            // Prompt user for token (but pre-filled if coming from recoverPassword)
            System.out.print("Enter recovery token: ");
            String inputToken = scanner.nextLine();

            if (!pwdManager.validateToken(userId, inputToken)) {
                System.out.println("Invalid or expired token. Password reset failed.");
                return;
            }

            // Prompt for new password with masking
            String newPassword = readPasswordSecurely("Enter new password: ");
            String hashedPassword = pwdManager.hashPassword(newPassword);

            // Update password
            List<User> users = userStorage.loadUsers();
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                if (u.getUserId().equals(userId)) {
                    users.set(i, new User(userId, hashedPassword, u.getSecurityQuestion(), u.getSecurityAnswer()));
                    break;
                }
            }

            userStorage.saveUsers(users.get(0)); // Save updated password
            pwdManager.invalidateToken(userId);
            System.out.println("Password reset successful! You can now log in with your new password.");

        } catch (IOException e) {
            System.out.println("Error during password reset: " + e.getMessage());
        }
    }

    /**
     * Reads a password securely without displaying it in the console.
     * Uses multiple approaches to ensure compatibility across different environments.
     *
     * @param prompt The prompt to display to the user
     * @return The password as a string
     */
    private String readPasswordSecurely(String prompt) {
        Console console = System.console();

        // Method 1: Use Java's built-in Console.readPassword() if available
        if (console != null) {
            char[] passwordChars = console.readPassword(prompt);
            String password = new String(passwordChars);
            // Clear the password from memory for security
            for (int i = 0; i < passwordChars.length; i++) {
                passwordChars[i] = ' ';
            }
            return password;
        }

        // Method 2: Use jline library if available (not included by default)
        try {
            Class<?> readerClass = Class.forName("jline.console.ConsoleReader");
            Object reader = readerClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method readLine = readerClass.getMethod("readLine", String.class, Character.class);
            String password = (String) readLine.invoke(reader, prompt, Character.valueOf('*'));
            return password;
        } catch (Exception e) {
            // JLine not available, continue to next method
        }

        // Method 3: Simple fallback - warn user and use regular input
        System.out.println("[Security Notice: Password will be visible as you type in this environment]");
        System.out.print(prompt);
        return scanner.nextLine();
    }
}