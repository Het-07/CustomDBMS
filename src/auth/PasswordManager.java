package auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages password hashing, verification, and password recovery functionality.
 */
public class PasswordManager {
    private static final String SALT = "SafeNSecure";
    private static final Map<String, String> recoveryTokens = new HashMap<>(); // Stores recovery tokens

    /**
     * Hashes the given password using SHA-256 and a predefined salt.
     *
     * @param password The password to hash.
     * @return The hashed password as a Base64 encoded string, or null if hashing fails.
     */
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String saltedPassword = SALT + password;
            byte[] hashBytes = md.digest(saltedPassword.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifies if the given password matches the stored hashed password.
     *
     * @param password       The password to verify.
     * @param hashedPassword The stored hashed password.
     * @return True if the password matches, false otherwise.
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        String newHash = hashPassword(password);
        return newHash.equals(hashedPassword);
    }

    /**
     * Generates a temporary recovery token for a user.
     *
     * @param userId The user requesting password recovery.
     * @return A unique recovery token.
     */
    public String generateRecoveryToken(String userId) {
        String token = UUID.randomUUID().toString(); // Generate a unique token
        recoveryTokens.put(userId, token);
        return token;
    }

    /**
     * Validates if a given token is valid for a user.
     *
     * @param userId The user ID.
     * @param token  The recovery token.
     * @return True if valid, false otherwise.
     */
    public boolean validateToken(String userId, String token) {
        return recoveryTokens.containsKey(userId) && recoveryTokens.get(userId).equals(token);
    }

    /**
     * Invalidates the recovery token after use.
     *
     * @param userId The user ID.
     */
    public void invalidateToken(String userId) {
        recoveryTokens.remove(userId);
    }
}
