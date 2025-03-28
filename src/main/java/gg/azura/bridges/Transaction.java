package gg.azura.bridges;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Modern implementation of Transaction management
 * @author SyncFocus17
 * @since 2025-02-04 18:19:13
 */
@Getter
@Setter
public class Transaction {

    private static final String CURRENT_TIME = "2025-02-04 18:19:13";
    private static final String CURRENT_USER = "SyncFocus17";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Cache for transactions to prevent frequent database queries
    private static final Map<UUID, CachedTransactions> transactionCache =
            new ConcurrentHashMap<>();

    private static final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(5);

    private final UUID playerId;
    private final TransactionType type;
    private final double amount;
    private final long timestamp;
    private final Bridges plugin;

    /**
     * Creates a new transaction
     */
    public Transaction(@NotNull UUID playerId, TransactionType type, double amount,
                       Bridges plugin) {
        this.playerId = playerId;
        this.type = type;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
        this.plugin = plugin;

        // Log creation
        logTransaction("CREATE", "New transaction created");
    }

    /**
     * Record for caching transactions
     */
    private record CachedTransactions(
            Collection<Transaction> transactions,
            long timestamp
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION;
        }
    }

    /**
     * Gets all transactions for a player
     * @param playerId The UUID of the player
     * @return Collection of transactions
     */
    public static Collection<Transaction> getTransactions(UUID playerId) {
        if (playerId == null) {
            Bridges.get().getLogger().warning(String.format(
                    "[TRANSACTION] Attempted to get transactions for null UUID | Time: %s",
                    CURRENT_TIME
            ));
            return Collections.emptyList();
        }

        try {
            // Check cache first
            CachedTransactions cached = transactionCache.get(playerId);
            if (cached != null && !cached.isExpired()) {
                logAccess("CACHE_HIT", playerId);
                return cached.transactions();
            }

            // Get from database
            Collection<Transaction> transactions = loadTransactionsFromDb(playerId);

            // Update cache
            transactionCache.put(playerId, new CachedTransactions(
                    transactions,
                    System.currentTimeMillis()
            ));

            logAccess("DB_FETCH", playerId);
            return transactions;

        } catch (Exception e) {
            handleError("Failed to get transactions", e, playerId);
            return Collections.emptyList();
        }
    }

    /**
     * Loads transactions from database
     */
    private static Collection<Transaction> loadTransactionsFromDb(UUID playerId)
            throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String query = """
            SELECT type, amount, timestamp 
            FROM transactions 
            WHERE player_id = ? 
            ORDER BY timestamp DESC
            """;

        try (Connection conn = (Connection) Bridges.get().getServicesManager()
                .getDBManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, playerId.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new Transaction(
                            playerId,
                            TransactionType.valueOf(rs.getString("type")),
                            rs.getDouble("amount"),
                            Bridges.get()
                    ));
                }
            }
        }

        return transactions;
    }

    /**
     * Saves the transaction to database
     */
    public void save() {
        try {
            String query = """
                INSERT INTO transactions (player_id, type, amount, timestamp) 
                VALUES (?, ?, ?, ?)
                """;

            try (Connection conn = (Connection) plugin.getServicesManager()
                    .getDBManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, playerId.toString());
                stmt.setString(2, type.name());
                stmt.setDouble(3, amount);
                stmt.setLong(4, timestamp);

                stmt.executeUpdate();

                // Update cache
                updateCache();

                logTransaction("SAVE", "Transaction saved to database");
            }
        } catch (Exception e) {
            handleError("Failed to save transaction", e, playerId);
        }
    }

    /**
     * Updates the cache with the new transaction
     */
    private void updateCache() {
        CachedTransactions cached = transactionCache.get(playerId);
        if (cached != null) {
            List<Transaction> updated = new ArrayList<>(cached.transactions());
            updated.add(this);
            transactionCache.put(playerId, new CachedTransactions(
                    updated,
                    System.currentTimeMillis()
            ));
        }
    }

    /**
     * Clears the cache for a player
     */
    public static void clearCache(UUID playerId) {
        transactionCache.remove(playerId);
        Bridges.get().getLogger().info(String.format(
                "[TRANSACTION] Cache cleared for %s | Time: %s",
                playerId,
                CURRENT_TIME
        ));
    }

    /**
     * Logs transaction operations
     */
    private void logTransaction(String operation, String details) {
        plugin.getLogger().info(String.format(
                "[TRANSACTION] %s | Player: %s | Type: %s | Amount: %.2f | Details: %s | Time: %s",
                operation,
                playerId,
                type,
                amount,
                details,
                CURRENT_TIME
        ));
    }

    /**
     * Logs access operations
     */
    private static void logAccess(String source, UUID playerId) {
        Bridges.get().getLogger().info(String.format(
                "[TRANSACTION] Access from %s | Player: %s | Time: %s",
                source,
                playerId,
                CURRENT_TIME
        ));
    }

    /**
     * Handles errors
     */
    private static void handleError(String message, Exception e, UUID playerId) {
        Bridges.get().getLogger().severe(String.format(
                "[ERROR] %s for %s: %s | User: %s | Time: %s",
                message,
                playerId,
                e.getMessage(),
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    @Override
    public String toString() {
        return String.format(
                "Transaction[player=%s, type=%s, amount=%.2f, time=%s]",
                playerId,
                type,
                amount,
                LocalDateTime.now().format(DATE_FORMATTER)
        );
    }
}