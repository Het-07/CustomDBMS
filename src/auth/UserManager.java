package auth;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages user data storage and retrieval in JSON format.
 * Ensures that users are loaded from and stored in the existing 'data/users.json' file.
 */
public class UserManager {
    private static final String USER_FILE = "data/users.json"; // Path to the user data file

    /**
     * Loads users from the existing 'data/users.json' file.
     * If the file does not exist or is empty, returns an empty list.
     *
     * @return List of registered users.
     * @throws IOException if there is an error reading the file.
     */
    public List<User> loadUsers() throws IOException {
        List<User> users = new ArrayList<>();
        File file = new File(USER_FILE);

        // If the file does not exist or is empty, return an empty list
        if (!file.exists() || file.length() == 0) {
            return users;
        }

        // Read the entire file content
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }

            // Remove JSON array brackets []
            String json = content.toString().replaceAll("[\\[\\]]", "");
            if (!json.isEmpty()) {
                String[] userEntries = json.split("\\},\\s*\\{"); // Split user entries

                for (String entry : userEntries) {
                    entry = entry.replaceAll("[{}]", "").trim(); // Remove curly brackets
                    String[] fields = entry.split(",");

                    String userID = null, password = null, question = null, answer = null;

                    // Extract key-value pairs from JSON
                    for (String field : fields) {
                        String[] keyValue = field.split(":");
                        if (keyValue.length < 2) continue; // Skip invalid entries

                        String key = keyValue[0].trim().replaceAll("\"", "");
                        String value = keyValue[1].trim().replaceAll("\"", "");

                        // Map fields to variables
                        switch (key) {
                            case "userID": userID = value; break;
                            case "password": password = value; break;
                            case "securityQuestion": question = value; break;
                            case "securityAnswer": answer = value; break;
                        }
                    }

                    // Ensure valid user data before adding to the list
                    if (userID != null && password != null && question != null && answer != null) {
                        users.add(new User(userID, password, question, answer));
                    }
                }
            }
        }

        return users;
    }

    /**
     * Saves a new user to 'data/users.json' without overwriting existing users.
     * If the user ID already exists, registration is rejected.
     *
     * @param newUser The user to be saved.
     * @throws IOException if there is an error writing to the file.
     */
    public void saveUsers(User newUser) throws IOException {
        File file = new File(USER_FILE);

        // If the file does not exist, create an empty JSON file
        if (!file.exists()) {
            System.err.println("Warning: 'data/users.json' not found. Creating a new file...");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("[]"); // Create an empty JSON array
            }
        }

        // Load existing users from the file (if any)
        List<User> existingUsers = loadUsers();

        // Check if the user already exists
        for (User user : existingUsers) {
            if (user.getUserId().equals(newUser.getUserId())) {
                System.out.println("User ID already exists!");
                return;
            }
        }

        // Add new user to the list
        existingUsers.add(newUser);

        // Write the updated user list to 'users.json'
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_FILE))) {
            writer.write("[\n");
            for (int i = 0; i < existingUsers.size(); i++) {
                User u = existingUsers.get(i);
                writer.write(String.format(
                        "  {\"userID\":\"%s\",\"password\":\"%s\",\"securityQuestion\":\"%s\",\"securityAnswer\":\"%s\"}",
                        u.getUserId(), u.getPassword(), u.getSecurityQuestion(), u.getSecurityAnswer()
                ));
                if (i < existingUsers.size() - 1) writer.write(",\n"); // Avoid trailing comma
            }
            writer.write("\n]");
        }
    }
}
