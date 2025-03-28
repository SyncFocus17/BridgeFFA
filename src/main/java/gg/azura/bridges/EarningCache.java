package gg.azura.bridges;

import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class EarningCache {
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    private final Map<UUID, CachedValue<Double>> cache;
    private final Plugin plugin;
    private final long defaultTTL;
    private final long maxTTL;

    public EarningCache(Plugin plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.defaultTTL = TimeUnit.MINUTES.toMillis(5);  // 5 minutes default
        this.maxTTL = TimeUnit.MINUTES.toMillis(30);     // 30 minutes maximum
    }

    /**
     * Gets the cached earnings for a player, calculating if necessary
     */
    public double getEarnings(UUID playerId, String username) {
        String currentTime = getCurrentTime();

        try {
            // Check existing cache
            CachedValue<Double> cached = cache.get(playerId);
            if (cached != null && !cached.isExpired()) {
                logDebug(String.format(
                        "[CACHE] Hit for player %s | Value: %.2f | Age: %dms | Time: %s",
                        playerId,
                        cached.getValue(),
                        System.currentTimeMillis() - cached.getTimestamp(),
                        currentTime
                ));
                return cached.getValue();
            }

            // Cache miss - calculate new value
            long startTime = System.nanoTime();
            double total = calculateEarnings(playerId, username, currentTime);

            // Update cache
            updateCache(playerId, total, username, currentTime);

            // Log performance
            logPerformance(playerId, username, startTime, total, currentTime);

            return total;

        } catch (Exception e) {
            logError(playerId, username, e, currentTime);
            return getCachedOrDefault(playerId, currentTime);
        }
    }

    /**
     * Calculates earnings from transactions
     */
    private double calculateEarnings(UUID playerId, String username, String currentTime) {
        List<Transaction> transactions = (List<Transaction>) Transaction.getTransactions(playerId);
        validateTransactions(transactions, playerId, username, currentTime);

        return transactions.parallelStream()
                .filter(Objects::nonNull)
                .filter(t -> t.getType() == TransactionType.EARN)
                .mapToDouble(this::getValidAmount)
                .sum();
    }

    /**
     * Validates transaction amount
     */
    private double getValidAmount(Transaction transaction) {
        double amount = transaction.getAmount();
        if (amount < 0) {
            logWarning(String.format(
                    "Negative transaction amount: %.2f | Transaction: %s",
                    amount,
                    transaction.getPlayerId()
            ));
            return 0.0;
        }
        return amount;
    }

    /**
     * Updates the cache with new value
     */
    private void updateCache(UUID playerId, double value, String username, String currentTime) {
        try {
            CachedValue<Double> newCache = new CachedValue<>(
                    value,
                    System.currentTimeMillis(),
                    calculateDynamicTTL(value)
            );
            cache.put(playerId, newCache);
        } catch (Exception e) {
            logWarning(String.format(
                    "Failed to update cache | Player: %s | User: %s | Error: %s | Time: %s",
                    playerId,
                    username,
                    e.getMessage(),
                    currentTime
            ));
        }
    }

    /**
     * Calculates dynamic TTL based on value
     */
    private long calculateDynamicTTL(double value) {
        // Larger values get longer cache times
        long additionalTime = TimeUnit.MINUTES.toMillis((long) (value / 10000));
        return Math.min(defaultTTL + additionalTime, maxTTL);
    }

    /**
     * Validates transactions list
     */
    private void validateTransactions(List<Transaction> transactions, UUID playerId, String username, String currentTime) {
        if (transactions == null || transactions.isEmpty()) {
            logWarning(String.format(
                    "No transactions found | Player: %s | User: %s | Time: %s",
                    playerId,
                    username,
                    currentTime
            ));
        }
    }

    /**
     * Gets cached value or returns default
     */
    private double getCachedOrDefault(UUID playerId, String currentTime) {
        CachedValue<Double> cached = cache.get(playerId);
        if (cached != null) {
            logWarning(String.format(
                    "[RECOVERY] Using cached value | Player: %s | Cache Age: %dms | Time: %s",
                    playerId,
                    System.currentTimeMillis() - cached.getTimestamp(),
                    currentTime
            ));
            return cached.getValue();
        }
        return 0.0;
    }

    /**
     * Logs performance metrics
     */
    private void logPerformance(UUID playerId, String username, long startTime, double total, String currentTime) {
        long duration = System.nanoTime() - startTime;
        CachedValue<Double> cached = cache.get(playerId);
        long ttl = cached != null ? cached.getTTL() : 0L;

        plugin.getLogger().info(String.format(
                "[METRICS] %s | Action: get_earnings | Player: %s | Duration: %dms | Result: %.2f | Cache TTL: %ds | Time: %s",
                username,
                playerId,
                duration / 1_000_000,
                total,
                ttl / 1000,
                currentTime
        ));
    }

    /**
     * Gets current UTC time
     */
    private String getCurrentTime() {
        return UTC_FORMATTER.format(Instant.now());
    }

    /**
     * Logs debug message
     */
    private void logDebug(String message) {
        plugin.getLogger().fine(message);
    }

    /**
     * Logs warning message
     */
    private void logWarning(String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * Logs error message
     */
    private void logError(UUID playerId, String username, Exception e, String currentTime) {
        plugin.getLogger().severe(String.format(
                "[ERROR] Failed to calculate earnings | Player: %s | User: %s | Error: %s | Time: %s",
                playerId,
                username,
                e.getMessage(),
                currentTime
        ));
    }

    /**
     * Cache value wrapper class
     */
    private static class CachedValue<T> {
        private final T value;
        private final long timestamp;
        private final long ttl;

        public CachedValue(T value, long timestamp, long ttl) {
            this.value = Objects.requireNonNull(value, "Cached value cannot be null");
            this.timestamp = timestamp;
            this.ttl = ttl;
        }

        public T getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getTTL() {
            return ttl;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ttl;
        }
    }

    /**
     * Clears all cached values
     */
    public void clearCache() {
        cache.clear();
        logDebug("[CACHE] Cleared all cached values");
    }

    /**
     * Removes cached value for specific player
     */
    public void clearCache(UUID playerId) {
        cache.remove(playerId);
        logDebug(String.format("[CACHE] Cleared cache for player %s", playerId));
    }
}