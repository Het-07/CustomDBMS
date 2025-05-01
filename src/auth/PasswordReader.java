package auth;

import java.io.Console;
import java.io.IOException;
import java.util.Arrays;

/**
 * Utility class for securely reading passwords from the console with masking.
 * Provides fallback mechanisms for environments where Console is not available.
 */
public class PasswordReader {

    /**
     * Reads a password from the console with masking.
     * Falls back to alternative methods if Console is not available.
     *
     * @param prompt The prompt to display to the user
     * @return The password as a string
     */
    public static String readPassword(String prompt) {
        Console console = System.console();

        // If console is available, use its built-in password reading functionality
        if (console != null) {
            char[] passwordChars = console.readPassword("%s", prompt);
            String password = new String(passwordChars);
            Arrays.fill(passwordChars, ' '); // Clear the password from memory
            return password;
        } else {
            // Console not available (e.g., running in IDE), use custom implementation
            return readPasswordWithMasking(prompt);
        }
    }

    /**
     * Custom implementation of password masking for environments where Console is not available.
     * Does not display any character as the user types for maximum security.
     *
     * @param prompt The prompt to display to the user
     * @return The password as a string
     */
    private static String readPasswordWithMasking(String prompt) {
        System.out.print(prompt);
        StringBuilder password = new StringBuilder();

        try {
            // Disable echo if possible
            String os = System.getProperty("os.name").toLowerCase();

            // For Unix/Linux/Mac systems
            if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                try {
                    // Try to use stty to disable echo
                    new ProcessBuilder("stty", "-echo").inheritIO().start().waitFor();
                } catch (Exception e) {
                    // Silently fail and continue with default approach
                }
            }

            // Read password without showing characters
            while (true) {
                int c = System.in.read();

                // Enter key pressed - end of password
                if (c == '\r' || c == '\n') {
                    System.out.println(); // Move to next line after password entry
                    break;
                }

                // Backspace handling
                if (c == '\b' || c == 127) {
                    if (password.length() > 0) {
                        password.deleteCharAt(password.length() - 1);
                    }
                    continue;
                }

                // Ignore control characters
                if (c < 32) {
                    continue;
                }

                // Add character to password without displaying anything
                password.append((char) c);
            }

            // Re-enable echo if it was disabled
            if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                try {
                    new ProcessBuilder("stty", "echo").inheritIO().start().waitFor();
                } catch (Exception e) {
                    // Silently fail
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading password: " + e.getMessage());
        }

        return password.toString();
    }
}
