package storage;

import java.io.*;
import java.util.*;

/**
 * Manages persistent storage for the lightweight DBMS.
 * Stores data in JSON format with a custom delimiter for organization.
 */
public class StorageManager {
    private static final String DATABASE_FILE = "data/database.json";
    private static final String DELIMITER = " | ";

    /**
     * Creates a new database if it does not already exist.
     *
     * @param dbName The name of the database to create.
     */
    public void createDatabase(String dbName) {
        Map<String, List<String>> database = loadDatabase();

        if (database.containsKey(dbName)) {
            System.out.println("Error: Database '" + dbName + "' already exists.");
            return;
        }

        // Add a new database entry with no tables
        database.put(dbName, new ArrayList<>());

        saveDatabase(database);
        System.out.println("Database '" + dbName + "' created successfully.");
    }

    /**
     * Saves the entire database structure to a JSON file.
     *
     * @param database The database structure to save.
     */
    public void saveDatabase(Map<String, List<String>> database) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATABASE_FILE))) {
            writer.write("{\n");
            int dbCount = 0;

            for (Map.Entry<String, List<String>> dbEntry : database.entrySet()) {
                writer.write("  \"" + dbEntry.getKey() + "\": [");

                List<String> tables = dbEntry.getValue();
                for (int i = 0; i < tables.size(); i++) {
                    writer.write("\"" + tables.get(i) + "\"");
                    if (i < tables.size() - 1) writer.write(", ");
                }

                writer.write("]");
                if (++dbCount < database.size()) writer.write(",");
                writer.write("\n");
            }
            writer.write("}");
        } catch (IOException e) {
            System.err.println("Error saving database: " + e.getMessage());
        }
    }


    /**
     * Saves data into a JSON file with a custom delimiter system.
     * Supports storing multiple tables and their records.
     *
     * @param tableName The name of the table being stored.
     * @param data The list of records to be stored.
     */
    public void saveTable(String tableName, List<String> data) {
        Map<String, List<String>> database = loadDatabase();

        // Extract database name from tableName if it contains a dot
        String dbName = tableName.contains(".") ? tableName.split("\\.")[0] : "mydb";
        String tableNameOnly = tableName.contains(".") ? tableName.split("\\.")[1] : tableName;

        // Add table to database if not already present
        if (!database.containsKey(dbName)) {
            database.put(dbName, new ArrayList<>());
        }

        List<String> tables = database.get(dbName);
        if (!tables.contains(tableNameOnly)) {
            tables.add(tableNameOnly);
        }

        // Update database structure
        saveDatabase(database);

        // Now save the actual table data to a separate file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data/" + tableName + ".json"))) {
            writer.write("[\n");
            for (int i = 0; i < data.size(); i++) {
                writer.write("  \"" + data.get(i).replace("\"", "\\\"") + "\"");
                if (i < data.size() - 1) writer.write(",");
                writer.write("\n");
            }
            writer.write("]");

            System.out.println("Table '" + tableNameOnly + "' successfully saved.");
        } catch (IOException e) {
            System.err.println("Error saving table '" + tableNameOnly + "': " + e.getMessage());
        }
    }

    /**
     * Loads the entire database from the JSON file.
     * Parses table names and records while preserving the custom delimiter system.
     *
     * @return A map containing table names and their respective data records.
     */
    public Map<String, List<String>> loadDatabase() {
        File file = new File(DATABASE_FILE);
        Map<String, List<String>> database = new HashMap<>();

        if (!file.exists() || file.length() == 0) {
            System.out.println("No database found. Initializing a new database.");
            return database;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }

            // Parse the JSON structure properly
            String json = content.toString();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1).trim();

                if (!json.isEmpty()) {
                    String[] dbEntries = json.split("\",\\s*\"|\",\"");

                    for (String dbEntry : dbEntries) {
                        String[] parts = dbEntry.split("\":\\s*\\[|\":\\[");
                        if (parts.length < 2) continue;

                        String dbName = parts[0].replace("\"", "").trim();
                        String tableList = parts[1].replace("]", "").trim();

                        List<String> tables = new ArrayList<>();
                        if (!tableList.isEmpty()) {
                            String[] tableNames = tableList.split(",");
                            for (String tableName : tableNames) {
                                tables.add(tableName.replace("\"", "").trim());
                            }
                        }

                        database.put(dbName, tables);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading database: " + e.getMessage());
        }
        return database;
    }

    // Add method to load table data
    public List<String> loadTableData(String tableName) {
        List<String> tableData = new ArrayList<>();
        File file = new File("data/" + tableName + ".json");

        if (!file.exists() || file.length() == 0) {
            return tableData;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }

            String json = content.toString();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1).trim();

                if (!json.isEmpty()) {
                    String[] rows = json.split("\",\\s*\"|\",\"");
                    for (String row : rows) {
                        tableData.add(row.replace("\"", "").trim());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading table data: " + e.getMessage());
        }

        return tableData;
    }
}
