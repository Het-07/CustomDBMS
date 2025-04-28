package storage;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles logging of security-related events, such as login attempts and password resets.
 * Stores logs dynamically in JSON format, ensuring proper structure.
 */
public class AuditLogger {
    private static final String LOG_FILE = "data/audit_logs.json";

    /**
     * Logs a security-related event (e.g., login attempt, password reset).
     *
     * @param userId     The user ID associated with the event.
     * @param eventType  The type of event (e.g., "LOGIN_SUCCESS", "LOGIN_FAILED").
     * @param ipAddress  Simulated IP address.
     */
    public static void logEvent(String userId, String eventType, String ipAddress) {
        try {
            List<Map<String, String>> existingLogs = loadLogs();

            // Create a new log entry dynamically
            Map<String, String> logEntry = new LinkedHashMap<>();
            logEntry.put("timestamp", LocalDateTime.now().toString());
            logEntry.put("userId", userId);
            logEntry.put("event", eventType);
            logEntry.put("ipAddress", ipAddress);

            existingLogs.add(logEntry);
            saveLogs(existingLogs);
        } catch (IOException e) {
            System.err.println("Error writing to audit log: " + e.getMessage());
        }
    }

    /**
     * Loads existing logs from the audit log file.
     *
     * @return List of log entries as JSON-structured Maps.
     */
    private static List<Map<String, String>> loadLogs() throws IOException {
        File file = new File(LOG_FILE);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        List<Map<String, String>> logs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line.trim());
            }

            // Validate and parse JSON properly
            String jsonString = jsonContent.toString().trim();
            if (!jsonString.isEmpty() && jsonString.startsWith("[") && jsonString.endsWith("]")) {
                logs = parseJsonArray(jsonString);
            }
        }
        return logs;
    }

    /**
     * Saves logs to the audit log file in proper JSON format.
     *
     * @param logs List of log entries as JSON-structured Maps.
     */
    private static void saveLogs(List<Map<String, String>> logs) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE))) {
            writer.write("[\n");
            for (int i = 0; i < logs.size(); i++) {
                writer.write(mapToJson(logs.get(i)));
                if (i < logs.size() - 1) writer.write(",\n"); // Ensures correct JSON formatting
            }
            writer.write("\n]");
        }
    }

    /**
     * Displays all stored log entries dynamically.
     */
    public static void viewLogs() {
        try {
            List<Map<String, String>> logs = loadLogs();
            if (logs.isEmpty()) {
                System.out.println("No logs available.");
            } else {
                System.out.println("\n--- Audit Logs ---");
                for (Map<String, String> log : logs) {
                    System.out.println(mapToJson(log));
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading logs: " + e.getMessage());
        }
    }

    /**
     * Converts a JSON array string into a list of maps.
     *
     * @param json The JSON array as a string.
     * @return List of maps representing JSON objects.
     */
    private static List<Map<String, String>> parseJsonArray(String json) {
        List<Map<String, String>> logs = new ArrayList<>();
        json = json.substring(1, json.length() - 1).trim(); // Remove square brackets

        String[] entries = json.split("\\},\\s*\\{"); // Split JSON objects
        for (String entry : entries) {
            entry = entry.replace("{", "").replace("}", "").trim();
            String[] keyValues = entry.split("\",\\s*\"");
            Map<String, String> logEntry = new LinkedHashMap<>();
            for (String keyValue : keyValues) {
                String[] pair = keyValue.split("\":\\s*\"");
                if (pair.length == 2) {
                    logEntry.put(pair[0].replace("\"", "").trim(), pair[1].replace("\"", "").trim());
                }
            }
            logs.add(logEntry);
        }
        return logs;
    }

    /**
     * Converts a map to a properly formatted JSON object.
     *
     * @param map The map representing JSON key-value pairs.
     * @return JSON string of the map.
     */
    private static String mapToJson(Map<String, String> map) {
        StringBuilder json = new StringBuilder("{ ");
        int count = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            if (++count < map.size()) json.append(", ");
        }
        json.append(" }");
        return json.toString();
    }
}
