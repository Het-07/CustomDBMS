package storage;

import java.util.*;

/**
 * Manages in-memory indexes for optimizing query performance.
 * Supports indexing of table data and efficient retrieval.
 */
public class IndexManager {
    private final Map<String, TreeMap<Integer, String>> tableIndexes; // Table -> (ID -> Row Data)

    /**
     * Constructor initializes an empty index map.
     */
    public IndexManager() {
        this.tableIndexes = new HashMap<>();
    }

    /**
     * Adds an entry to the in-memory index when new data is inserted.
     *
     * @param tableName The name of the table.
     * @param id        The primary key ID.
     * @param rowData   The data to store.
     */
    public void addToIndex(String tableName, int id, String rowData) {
        tableIndexes.computeIfAbsent(tableName, k -> new TreeMap<>());
        tableIndexes.get(tableName).put(id, rowData);
        System.out.println("Added to index: Table=" + tableName + ", ID=" + id);
    }

    /**
     * Retrieves a record by its ID from the index.
     *
     * @param tableName The table name.
     * @param id        The primary key.
     * @return The record if found, otherwise null.
     */
    public String getRecordById(String tableName, int id) {
        if (!tableIndexes.containsKey(tableName)) {
            return null;
        }
        return tableIndexes.get(tableName).get(id);
    }

    /**
     * Deletes an entry from the index.
     *
     * @param tableName The table name.
     * @param id        The ID to delete.
     */
    public void deleteFromIndex(String tableName, int id) {
        if (tableIndexes.containsKey(tableName)) {
            tableIndexes.get(tableName).remove(id);
            System.out.println("Removed from index: Table=" + tableName + ", ID=" + id);
        }
    }

    /**
     * Updates an entry in the index.
     *
     * @param tableName The table name.
     * @param id        The ID to update.
     * @param newData   The new data.
     */
    public void updateIndex(String tableName, int id, String newData) {
        if (tableIndexes.containsKey(tableName)) {
            if (tableIndexes.get(tableName).containsKey(id)) {
                tableIndexes.get(tableName).put(id, newData);
                System.out.println("Updated index: Table=" + tableName + ", ID=" + id);
            }
        }
    }

    /**
     * Retrieves all indexed records from a table.
     *
     * @param tableName The table name.
     * @return List of indexed data.
     */
    public List<String> getAllRecords(String tableName) {
        if (!tableIndexes.containsKey(tableName)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tableIndexes.get(tableName).values());
    }

    /**
     * Checks if a table is indexed.
     *
     * @param tableName The table name.
     * @return True if indexed, false otherwise.
     */
    public boolean isTableIndexed(String tableName) {
        return tableIndexes.containsKey(tableName) && !tableIndexes.get(tableName).isEmpty();
    }

    /**
     * Rebuilds the index for a table from the provided data.
     *
     * @param tableName The table name.
     * @param data      The table data.
     */
    public void rebuildIndex(String tableName, List<String> data) {
        // Clear existing index for this table
        tableIndexes.remove(tableName);
        tableIndexes.put(tableName, new TreeMap<>());

        // Skip the schema row (index 0)
        for (int i = 1; i < data.size(); i++) {
            String row = data.get(i);
            String[] values = row.split(",");

            try {
                int id = Integer.parseInt(values[0].trim());
                tableIndexes.get(tableName).put(id, row);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // Skip rows that don't have a valid ID in the first column
            }
        }

        System.out.println("Rebuilt index for table " + tableName + " with " +
                (tableIndexes.get(tableName).size()) + " entries");
    }
}
