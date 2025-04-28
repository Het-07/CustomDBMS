package transactionHandling;

import storage.StorageManager;
import concurrencyControl.LockManager;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages transactions with concurrency control using Read-Write locks.
 * - Supports BEGIN TRANSACTION, COMMIT, and ROLLBACK dynamically.
 * - Allows multiple read operations but restricts concurrent writes.
 */
public class TransactionManager {
    private boolean transactionActive = false;
    private final List<String> transactionLog = new LinkedList<>();
    private final StorageManager storageManager;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final LockManager lockManager = new LockManager();

    /**
     * Constructor initializes the TransactionManager with a StorageManager instance.
     *
     * @param storageManager Storage manager for database persistence.
     */
    public TransactionManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    /**
     * Checks if a transaction is currently active.
     *
     * @return true if a transaction is in progress, false otherwise.
     */
    public boolean isTransactionActive() {
        return transactionActive;
    }

    /**
     * Begins a new transaction if none is active.
     */
    public void beginTransaction() {
        if (transactionActive) {
            System.out.println("Error: A transaction is already in progress.");
        } else {
            transactionActive = true;
            transactionLog.clear();
            System.out.println("Transaction started.");
        }
    }

    /**
     * Adds an operation dynamically to the transaction log.
     *
     * @param operation The SQL-like operation to store.
     */
    public void addOperation(String operation) {
        if (!transactionActive) {
            System.out.println("Error: No active transaction. Use 'BEGIN TRANSACTION' first.");
            return;
        }
        transactionLog.add(operation);
        System.out.println("Queued in transaction: " + operation);
    }

    /**
     * Commits the transaction by executing all stored operations dynamically.
     * Uses write lock to prevent concurrent modifications.
     */
    public void commit() {
        if (!transactionActive) {
            System.out.println("Error: No active transaction to commit.");
            return;
        }

        lock.writeLock().lock(); // Acquire write lock to prevent concurrent writes
        try {
            for (String operation : transactionLog) {
                executeOperation(operation);
            }
            transactionLog.clear();
            transactionActive = false;
            System.out.println("Transaction committed successfully.");
        } finally {
            lock.writeLock().unlock(); // Release write lock after committing
        }
    }

    /**
     * Rolls back the transaction by discarding all pending operations.
     */
    public void rollback() {
        if (!transactionActive) {
            System.out.println("Error: No active transaction to rollback.");
            return;
        }

        transactionLog.clear();
        transactionActive = false;
        System.out.println("Transaction rolled back.");
    }

    /**
     * Executes stored operations on commit dynamically.
     * Uses read lock for read operations, and write lock for insert/update/delete.
     *
     * @param operation The SQL-like operation.
     */
    private void executeOperation(String operation) {
        String[] tokens = operation.trim().split("\\s+");

        if (tokens.length < 3) {
            System.out.println("Error: Invalid transaction operation format.");
            return;
        }

        String command = tokens[0].toUpperCase();
        String tableName = tokens[2];

        switch (command) {
            case "INSERT":
            case "UPDATE":
            case "DELETE":
                if (lockManager.acquireWriteLock(tableName)) {
                    try {
                        processWriteOperation(command, tableName, operation);
                    } finally {
                        lockManager.releaseWriteLock();
                    }
                } else {
                    System.out.println("Error: Could not acquire write lock for table '" + tableName + "'.");
                }
                break;

            case "SELECT":
                if (lockManager.acquireReadLock(tableName)) {
                    try {
                        processReadOperation(tableName, operation);
                    } finally {
                        lockManager.releaseReadLock(tableName);
                    }
                } else {
                    System.out.println("Error: Could not acquire read lock for table '" + tableName + "'.");
                }
                break;

            default:
                System.out.println("Unsupported operation in transaction: " + operation);
        }
    }

    /**
     * Processes read operations within a transaction.
     *
     * @param tableName The table to retrieve data from.
     * @param operation The SELECT statement.
     */
    private void processReadOperation(String tableName, String operation) {
        // Extract database name if present in tableName
        String dbName = "mydb";
        String tableNameOnly = tableName;
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.");
            dbName = parts[0];
            tableNameOnly = parts[1];
        }

        String fullTableName = dbName + "." + tableNameOnly;
        List<String> tableData = storageManager.loadTableData(fullTableName);

        if (tableData.isEmpty()) {
            System.out.println("Error: Table '" + tableNameOnly + "' not found.");
            return;
        }

        System.out.println("\nData in '" + tableNameOnly + "':");
        for (int i = 1; i < tableData.size(); i++) { // Skip schema row
            System.out.println(tableData.get(i));
        }
    }

    /**
     * Processes write operations (INSERT, UPDATE, DELETE).
     *
     * @param command   The SQL command.
     * @param tableName The target table.
     * @param operation The SQL operation string.
     */
    private void processWriteOperation(String command, String tableName, String operation) {
        // Extract database name if present in tableName
        String dbName = "mydb";
        String tableNameOnly = tableName;
        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.");
            dbName = parts[0];
            tableNameOnly = parts[1];
        }

        String fullTableName = dbName + "." + tableNameOnly;
        List<String> tableData = storageManager.loadTableData(fullTableName);

        if (tableData.isEmpty()) {
            System.out.println("Error: Table '" + tableNameOnly + "' not found.");
            return;
        }

        switch (command) {
            case "INSERT":
                String values = operation.substring(operation.indexOf("VALUES") + 6).trim().replaceAll("[()]", "");
                tableData.add(values);
                storageManager.saveTable(fullTableName, tableData);
                System.out.println("Committed: " + operation);
                break;

            case "UPDATE":
                updateTableData(fullTableName, operation, tableData);
                break;

            case "DELETE":
                deleteTableData(fullTableName, operation, tableData);
                break;
        }
    }

    /**
     * Updates table data dynamically.
     *
     * @param tableName The table to update.
     * @param operation The UPDATE statement.
     * @param tableData The current table data.
     */
    private void updateTableData(String tableName, String operation, List<String> tableData) {
        String[] parts = operation.split("WHERE");
        if (parts.length < 2) {
            System.out.println("Error: Missing WHERE condition in UPDATE statement.");
            return;
        }

        String setClause = parts[0].split("SET")[1].trim();
        String whereCondition = parts[1].trim();

        boolean updated = false;
        for (int i = 1; i < tableData.size(); i++) { // Skip schema row
            if (tableData.get(i).contains(whereCondition)) {
                tableData.set(i, setClause);
                updated = true;
            }
        }

        if (updated) {
            storageManager.saveTable(tableName, tableData);
            System.out.println("Committed: " + operation);
        } else {
            System.out.println("No records matched the condition for update.");
        }
    }

    /**
     * Deletes table data dynamically based on condition.
     *
     * @param tableName The table to delete from.
     * @param operation The DELETE statement.
     * @param tableData The current table data.
     */
    private void deleteTableData(String tableName, String operation, List<String> tableData) {
        String condition = operation.contains("WHERE") ? operation.split("WHERE")[1].trim() : "";

        int initialSize = tableData.size();
        if (condition.isEmpty()) {
            // Keep only the schema row
            List<String> schemaOnly = new ArrayList<>();
            schemaOnly.add(tableData.get(0));
            tableData = schemaOnly;
        } else {
            // Remove rows that match the condition
            List<String> newData = new ArrayList<>();
            newData.add(tableData.get(0)); // Keep schema

            for (int i = 1; i < tableData.size(); i++) {
                if (!tableData.get(i).contains(condition)) {
                    newData.add(tableData.get(i));
                }
            }
            tableData = newData;
        }

        storageManager.saveTable(tableName, tableData);
        int rowsDeleted = initialSize - tableData.size();
        System.out.println("Committed: " + operation + " (" + rowsDeleted + " rows affected)");
    }
}
