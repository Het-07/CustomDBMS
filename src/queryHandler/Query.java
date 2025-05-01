package queryHandler;

import storage.StorageManager;
import transactionHandling.TransactionManager;
import storage.IndexManager;

import java.util.*;

/**
 * Handles SQL-like queries for the lightweight DBMS.
 * Implements SHOW, USE, CREATE, DESCRIBE, INSERT, SELECT, and transaction operations.
 */
public class Query {
    private final StorageManager storageManager;
    private final TransactionManager transactionManager;
    private final IndexManager indexManager;
    private String activeDatabase = null; // Stores the selected database

    /**
     * Constructor initializes the Query Processor with StorageManager and TransactionManager instances.
     */
    public Query() {
        this.storageManager = new StorageManager();
        this.indexManager = new IndexManager();
        this.transactionManager = new TransactionManager(storageManager);
    }

    /**
     * Processes a SQL-like query entered by the user.
     * 
     * @param query The SQL query to process.
     */
    public void processQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            System.out.println("Error: Empty query.");
            return;
        }

        // Remove trailing semicolon if present
        if (query.trim().endsWith(";")) {
            query = query.trim().substring(0, query.trim().length() - 1);
        }

        String[] tokens = query.trim().split("\\s+", 3);
        String command = tokens[0].toUpperCase();

        switch (command) {
            case "SHOW":
                if (tokens.length > 1 && tokens[1].equalsIgnoreCase("DATABASES")) {
                    showDatabases();
                } else {
                    System.out.println("Error: Invalid SHOW command. Use 'SHOW DATABASES'.");
                }
                break;

            case "USE":
                if (tokens.length > 1) {
                    useDatabase(tokens[1]);
                } else {
                    System.out.println("Error: Database name required. Use 'USE database_name'.");
                }
                break;

            case "CREATE":
                if (tokens.length > 1) {
                    if (tokens[1].equalsIgnoreCase("DATABASE") && tokens.length > 2) {
                        createDatabase(tokens[2]);
                    } else if (tokens[1].equalsIgnoreCase("TABLE") && tokens.length > 2) {
                        String tableInfo = tokens[2];
                        int openParenIndex = tableInfo.indexOf('(');
                        if (openParenIndex > 0) {
                            String tableName = tableInfo.substring(0, openParenIndex).trim();
                            String columns = tableInfo.substring(openParenIndex).trim();
                            createTable(tableName, columns);
                        } else {
                            System.out.println("Error: Invalid CREATE TABLE syntax. Use 'CREATE TABLE table_name (column1 type, column2 type, ...)'.");
                        }
                    } else {
                        System.out.println("Error: Invalid CREATE command. Use 'CREATE DATABASE db_name' or 'CREATE TABLE table_name (columns)'.");
                    }
                } else {
                    System.out.println("Error: Invalid CREATE command. Use 'CREATE DATABASE db_name' or 'CREATE TABLE table_name (columns)'.");
                }
                break;

            case "DESCRIBE":
                if (tokens.length > 1) {
                    describeTable(tokens[1]);
                } else {
                    System.out.println("Error: Table name required. Use 'DESCRIBE table_name'.");
                }
                break;

            case "INSERT":
                if (tokens.length > 1 && tokens[1].equalsIgnoreCase("INTO") && tokens.length > 2) {
                    String tableInfo = tokens[2];
                    int valuesIndex = tableInfo.toUpperCase().indexOf("VALUES");
                    if (valuesIndex > 0) {
                        String tableName = tableInfo.substring(0, valuesIndex).trim();
                        String values = tableInfo.substring(valuesIndex + 6).trim();
                        // Remove parentheses
                        values = values.replaceAll("[()]", "").trim();
                        insertData(tableName, values);
                    } else {
                        System.out.println("Error: Invalid INSERT syntax. Use 'INSERT INTO table_name VALUES (value1, value2, ...)'.");
                    }
                } else {
                    System.out.println("Error: Invalid INSERT command. Use 'INSERT INTO table_name VALUES (value1, value2, ...)'.");
                }
                break;

            case "SELECT":
                if (tokens.length > 1) {
                    String selectQuery = query.substring(7).trim(); // Remove "SELECT "
                    String fromKeyword = "FROM";
                    int fromIndex = selectQuery.toUpperCase().indexOf(fromKeyword);
                    
                    if (fromIndex > 0) {
                        String columns = selectQuery.substring(0, fromIndex).trim();
                        String remaining = selectQuery.substring(fromIndex + fromKeyword.length()).trim();
                        
                        String tableName;
                        String condition = "";
                        
                        int whereIndex = remaining.toUpperCase().indexOf("WHERE");
                        if (whereIndex > 0) {
                            tableName = remaining.substring(0, whereIndex).trim();
                            condition = remaining.substring(whereIndex + 5).trim();
                        } else {
                            tableName = remaining.trim();
                        }
                        
                        selectData(tableName, condition);
                    } else {
                        System.out.println("Error: Invalid SELECT syntax. Use 'SELECT columns FROM table_name [WHERE condition]'.");
                    }
                } else {
                    System.out.println("Error: Invalid SELECT command. Use 'SELECT columns FROM table_name [WHERE condition]'.");
                }
                break;

            case "BEGIN":
                if (tokens.length > 1 && tokens[1].equalsIgnoreCase("TRANSACTION")) {
                    beginTransaction();
                } else {
                    System.out.println("Error: Invalid BEGIN command. Use 'BEGIN TRANSACTION'.");
                }
                break;

            case "COMMIT":
                commitTransaction();
                break;

            case "ROLLBACK":
                rollbackTransaction();
                break;

            default:
                System.out.println("Error: Unsupported command. Supported commands: SHOW, USE, CREATE, DESCRIBE, INSERT, SELECT, BEGIN, COMMIT, ROLLBACK.");
        }
    }

    /**
     * Executes the CREATE DATABASE command.
     *
     * @param dbName The name of the database to create.
     */
    public void createDatabase(String dbName) {
        storageManager.createDatabase(dbName);
        activeDatabase = dbName; // Set active DB after creation
    }

    /**
     * Displays all available databases in the system.
     */
    public void showDatabases() {
        Map<String, List<String>> database = storageManager.loadDatabase(); // Load DBs

        if (database.isEmpty()) {
            System.out.println("No databases found.");
        } else {
            System.out.println("\nAvailable Databases:");
            for (String dbName : database.keySet()) {
                System.out.println("- " + dbName);
            }
        }
    }

    /**
     * Selects a database for subsequent operations.
     *
     * @param dbName The database name to use.
     */
    public void useDatabase(String dbName) {
        Map<String, List<String>> database = storageManager.loadDatabase();

        if (database.containsKey(dbName)) {
            activeDatabase = dbName;
            System.out.println("Database '" + dbName + "' is now in use.");
        } else {
            System.out.println("Error: Database '" + dbName + "' not found.");
        }
    }

    /**
     * Creates a new table in the selected database.
     *
     * @param tableName The name of the table to create.
     * @param columns   The column names (e.g., "id INT, name STRING").
     */
    public void createTable(String tableName, String columns) {
        if (activeDatabase == null) {
            System.out.println("Error: No database selected. Use 'USE database_name' first.");
            return;
        }

        Map<String, List<String>> database = storageManager.loadDatabase();
        if (!database.containsKey(activeDatabase)) {
            System.out.println("Error: Selected database does not exist.");
            return;
        }

        List<String> tables = database.get(activeDatabase);
        if (tables.contains(tableName)) {
            System.out.println("Error: Table '" + tableName + "' already exists.");
            return;
        }

        // Store table schema as the first entry in the table data
        List<String> tableData = new ArrayList<>();
        tableData.add("SCHEMA: " + columns);
        storageManager.saveTable(activeDatabase + "." + tableName, tableData);

        System.out.println("Table '" + tableName + "' created successfully in database '" + activeDatabase + "'.");
    }

    /**
     * Displays the schema of a specified table.
     *
     * @param tableName The table to describe.
     */
    public void describeTable(String tableName) {
        if (activeDatabase == null) {
            System.out.println("Error: No database selected. Use 'USE database_name' first.");
            return;
        }

        String fullTableName = activeDatabase + "." + tableName;
        List<String> tableData = storageManager.loadTableData(fullTableName);

        if (tableData.isEmpty()) {
            System.out.println("Error: Table '" + tableName + "' not found in database '" + activeDatabase + "'.");
            return;
        }

        System.out.println("\nTable Structure: " + tableName);
        System.out.println(tableData.get(0)); // Schema information
    }

    /**
     * Inserts a new record into a table.
     *
     * @param tableName The target table.
     * @param record    The record to insert (comma-separated values).
     */
    public void insertData(String tableName, String record) {
        if (activeDatabase == null) {
            System.out.println("Error: No database selected. Use 'USE database_name' first.");
            return;
        }

        String fullTableName = activeDatabase + "." + tableName;
        String operation = "INSERT INTO " + tableName + " VALUES (" + record + ")";

        if (transactionManager.isTransactionActive()) {
            transactionManager.addOperation(operation);
        } else {
            List<String> tableData = storageManager.loadTableData(fullTableName);
            
            if (tableData.isEmpty()) {
                System.out.println("Error: Table '" + tableName + "' not found.");
                return;
            }
            
            // Check if the number of values matches the schema
            String schema = tableData.get(0);
            String[] columnDefs = schema.substring(schema.indexOf(":") + 1).trim().split(",");
            String[] values = record.split(",");
            
            if (columnDefs.length != values.length) {
                System.out.println("Error: Column mismatch: expected " + columnDefs.length + " values but got " + values.length + ".");
                return;
            }
            
            tableData.add(record);
            storageManager.saveTable(fullTableName, tableData);
            
            // Update index
            try {
                int id = Integer.parseInt(values[0].trim().replace("'", ""));
                indexManager.addToIndex(fullTableName, id, record);
            } catch (NumberFormatException e) {
                // If first column is not an integer, skip indexing
            }
            
            System.out.println("Data inserted successfully into '" + tableName + "'.");
        }
    }

    /**
     * Selects data from a table with optional filtering conditions.
     *
     * @param tableName The table to retrieve data from.
     * @param condition The condition for filtering records (e.g., "id=1").
     */
    public void selectData(String tableName, String condition) {
        if (activeDatabase == null) {
            System.out.println("Error: No database selected. Use 'USE database_name' first.");
            return;
        }

        String fullTableName = activeDatabase + "." + tableName;
        List<String> tableData = storageManager.loadTableData(fullTableName);

        if (tableData.isEmpty()) {
            System.out.println("Error: Table '" + tableName + "' not found.");
            return;
        }

        System.out.println("\nData in '" + tableName + "':");
        
        // If no condition, return all rows
        if (condition == null || condition.trim().isEmpty()) {
            for (int i = 1; i < tableData.size(); i++) { // Skip schema row
                System.out.println(tableData.get(i));
            }
            return;
        }
        
        // Parse the schema to get column names and types
        String schema = tableData.get(0);
        String schemaContent = schema.substring(schema.indexOf(":") + 1).trim();
        String[] columnDefs = schemaContent.split(",");
        
        Map<String, Integer> columnIndexMap = new HashMap<>();
        Map<String, String> columnTypeMap = new HashMap<>();
        
        for (int i = 0; i < columnDefs.length; i++) {
            String[] parts = columnDefs[i].trim().split("\\s+");
            if (parts.length >= 2) {
                String colName = parts[0].trim();
                String colType = parts[1].trim().toUpperCase();
                columnIndexMap.put(colName, i);
                columnTypeMap.put(colName, colType);
            }
        }
        
        // Parse the condition
        String[] operators = {">=", "<=", "!=", "=", ">", "<"};
        String operator = null;
        String columnName = null;
        String value = null;
        
        for (String op : operators) {
            if (condition.contains(op)) {
                String[] parts = condition.split(op, 2);
                if (parts.length == 2) {
                    columnName = parts[0].trim();
                    value = parts[1].trim();
                    operator = op;
                    break;
                }
            }
        }
        
        if (columnName == null || operator == null || value == null) {
            System.out.println("Error: Invalid condition format. Use 'column operator value'.");
            return;
        }
        
        // Check if column exists
        if (!columnIndexMap.containsKey(columnName)) {
            System.out.println("Error: Column '" + columnName + "' not found in table.");
            return;
        }
        
        int columnIndex = columnIndexMap.get(columnName);
        String columnType = columnTypeMap.get(columnName);
        
        // Process each row
        boolean found = false;
        for (int i = 1; i < tableData.size(); i++) { // Skip schema row
            String row = tableData.get(i);
            String[] values = row.split(",");
            
            if (values.length <= columnIndex) {
                continue; // Skip rows with insufficient columns
            }
            
            String cellValue = values[columnIndex].trim().replace("'", "");
            
            boolean matches = false;
            
            // Compare based on column type
            if (columnType.equals("INT") || columnType.equals("FLOAT")) {
                try {
                    double cellNum = Double.parseDouble(cellValue);
                    double valueNum = Double.parseDouble(value.replace("'", ""));
                    
                    switch (operator) {
                        case "=": matches = cellNum == valueNum; break;
                        case ">": matches = cellNum > valueNum; break;
                        case "<": matches = cellNum < valueNum; break;
                        case ">=": matches = cellNum >= valueNum; break;
                        case "<=": matches = cellNum <= valueNum; break;
                        case "!=": matches = cellNum != valueNum; break;
                    }
                } catch (NumberFormatException e) {
                    // Skip rows with invalid number format
                    continue;
                }
            } else {
                // String comparison - remove quotes if present
                String valueStr = value.replace("'", "");
                
                switch (operator) {
                    case "=": matches = cellValue.equals(valueStr); break;
                    case "!=": matches = !cellValue.equals(valueStr); break;
                    case ">": matches = cellValue.compareTo(valueStr) > 0; break;
                    case "<": matches = cellValue.compareTo(valueStr) < 0; break;
                    case ">=": matches = cellValue.compareTo(valueStr) >= 0; break;
                    case "<=": matches = cellValue.compareTo(valueStr) <= 0; break;
                }
            }
            
            if (matches) {
                System.out.println(row);
                found = true;
            }
        }
        
        if (!found) {
            System.out.println("No records found matching the condition.");
        }
    }

    /**
     * Begins a transaction.
     */
    public void beginTransaction() {
        transactionManager.beginTransaction();
    }

    /**
     * Commits the active transaction.
     */
    public void commitTransaction() {
        transactionManager.commit(activeDatabase);
    }

    /**
     * Rolls back the active transaction.
     */
    public void rollbackTransaction() {
        transactionManager.rollback();
    }
}