package auth;

import storage.AuditLogger;

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
            System.out.print("Password: ");
            String password = scanner.nextLine();

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

            // Prompt for user credentials
            System.out.print("Password: ");
            String password = pwdManager.hashPassword(scanner.nextLine()); // Hash the password

            System.out.print("Security Question: ");
            String question = scanner.nextLine();

            System.out.print("Security Answer: ");
            String answer = pwdManager.hashPassword(scanner.nextLine()); // Hash the security answer

            // Create a new User object
            User newUser = new User(userId, password, question, answer);

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
            System.out.print("Enter your answer: ");
            String answer = scanner.nextLine();

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

            // Prompt for new password
            System.out.print("Enter new password: ");
            String newPassword = pwdManager.hashPassword(scanner.nextLine());

            // Update password
            List<User> users = userStorage.loadUsers();
            for (User u : users) {
                if (u.getUserId().equals(userId)) {
                    u = new User(userId, newPassword, u.getSecurityQuestion(), u.getSecurityAnswer());
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
}
