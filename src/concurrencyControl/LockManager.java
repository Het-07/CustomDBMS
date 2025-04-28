package concurrencyControl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages read/write locks to ensure concurrency control.
 * - Allows multiple read operations at the same time.
 * - Ensures only one write operation can happen at a time.
 */
public class LockManager {
    private final Map<String, Set<String>> readLocks; // Table -> Set of transaction IDs
    private final Map<String, String> writeLocks; // Table -> Transaction ID
    private static final String DEFAULT_TRANSACTION = "default";

    /**
     * Constructor initializes lock manager.
     */
    public LockManager() {
        this.readLocks = new HashMap<>();
        this.writeLocks = new HashMap<>();
    }

    /**
     * Attempts to acquire a read lock.
     *
     * @param tableName The table to read.
     * @return True if read lock is acquired, false if blocked by a write lock.
     */
    public synchronized boolean acquireReadLock(String tableName) {
        // If there's a write lock on this table by another transaction, block
        if (writeLocks.containsKey(tableName) && !writeLocks.get(tableName).equals(DEFAULT_TRANSACTION)) {
            System.out.println("Read blocked! Write lock active on table: " + tableName +
                    " by transaction: " + writeLocks.get(tableName));
            return false;
        }

        // Add read lock
        readLocks.computeIfAbsent(tableName, k -> new HashSet<>()).add(DEFAULT_TRANSACTION);
        System.out.println("Read lock acquired on table: " + tableName);
        return true;
    }

    /**
     * Releases a read lock.
     *
     * @param tableName The table to release lock from.
     */
    public synchronized void releaseReadLock(String tableName) {
        if (readLocks.containsKey(tableName)) {
            readLocks.get(tableName).remove(DEFAULT_TRANSACTION);
            if (readLocks.get(tableName).isEmpty()) {
                readLocks.remove(tableName);
            }
            System.out.println("Read lock released on table: " + tableName);
        }
    }

    /**
     * Attempts to acquire a write lock.
     *
     * @param tableName The table to write.
     * @return True if write lock is acquired, false if blocked by other transactions.
     */
    public synchronized boolean acquireWriteLock(String tableName) {
        // If there are read locks or a write lock on this table, block
        if ((readLocks.containsKey(tableName) && !readLocks.get(tableName).isEmpty()) ||
                writeLocks.containsKey(tableName)) {
            System.out.println("Write blocked! Active locks on table: " + tableName);
            return false;
        }

        // Add write lock
        writeLocks.put(tableName, DEFAULT_TRANSACTION);
        System.out.println("Write lock acquired on table: " + tableName);
        return true;
    }

    /**
     * Releases a write lock.
     */
    public synchronized void releaseWriteLock() {
        for (String tableName : writeLocks.keySet()) {
            if (DEFAULT_TRANSACTION.equals(writeLocks.get(tableName))) {
                System.out.println("Write lock released on table: " + tableName);
            }
        }
        writeLocks.values().removeIf(DEFAULT_TRANSACTION::equals);
    }

    /**
     * Acquires a read lock for a specific transaction.
     *
     * @param tableName     The table to read.
     * @param transactionId The transaction ID.
     * @return True if read lock is acquired, false if blocked.
     */
    public synchronized boolean acquireReadLock(String tableName, String transactionId) {
        // If there's a write lock on this table by another transaction, block
        if (writeLocks.containsKey(tableName) && !writeLocks.get(tableName).equals(transactionId)) {
            System.out.println("Read blocked for transaction " + transactionId +
                    "! Write lock active on table: " + tableName +
                    " by transaction: " + writeLocks.get(tableName));
            return false;
        }

        // Add read lock
        readLocks.computeIfAbsent(tableName, k -> new HashSet<>()).add(transactionId);
        System.out.println("Read lock acquired on table: " + tableName + " by transaction: " + transactionId);
        return true;
    }

    /**
     * Acquires a write lock for a specific transaction.
     *
     * @param tableName     The table to write.
     * @param transactionId The transaction ID.
     * @return True if write lock is acquired, false if blocked.
     */
    public synchronized boolean acquireWriteLock(String tableName, String transactionId) {
        // Check for read locks by other transactions
        if (readLocks.containsKey(tableName)) {
            Set<String> readers = readLocks.get(tableName);
            if (readers.size() > 1 || (readers.size() == 1 && !readers.contains(transactionId))) {
                System.out.println("Write blocked for transaction " + transactionId +
                        "! Read locks active on table: " + tableName);
                return false;
            }
        }

        // Check for write lock by another transaction
        if (writeLocks.containsKey(tableName) && !writeLocks.get(tableName).equals(transactionId)) {
            System.out.println("Write blocked for transaction " + transactionId +
                    "! Write lock active on table: " + tableName +
                    " by transaction: " + writeLocks.get(tableName));
            return false;
        }

        // Add write lock
        writeLocks.put(tableName, transactionId);
        System.out.println("Write lock acquired on table: " + tableName + " by transaction: " + transactionId);
        return true;
    }

    /**
     * Releases all locks held by a transaction.
     *
     * @param transactionId The transaction ID.
     */
    public synchronized void releaseAllLocks(String transactionId) {
        // Release read locks
        for (Map.Entry<String, Set<String>> entry : readLocks.entrySet()) {
            entry.getValue().remove(transactionId);
            System.out.println("Read lock released on table: " + entry.getKey() + " by transaction: " + transactionId);
        }

        // Clean up empty read lock sets
        readLocks.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Release write locks
        for (Map.Entry<String, String> entry : writeLocks.entrySet()) {
            if (transactionId.equals(entry.getValue())) {
                System.out.println("Write lock released on table: " + entry.getKey() + " by transaction: " + transactionId);
            }
        }

        writeLocks.entrySet().removeIf(entry -> transactionId.equals(entry.getValue()));
    }
}
