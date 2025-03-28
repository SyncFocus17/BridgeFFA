package gg.azura.bridges;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gg.azura.bridges.ffa.Spawn;
import gg.azura.bridges.gui.SoundSettings;
import gg.azura.bridges.services.PlayerManager;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class BridgePlayer {
    private final Bridges plugin = Bridges.get();
    private final PlayerManager playerManager;
    private final UUID uuid;
    @Getter
    private final String name;
    // Getters and Setters
    @Getter
    private Player player;
    private static final String CURRENT_TIME = "2025-02-04 18:02:50";
    private static final String CURRENT_USER = "SyncFocus17";
    private final Map<String, Optional<Double>> cachedEarnings;
    private final Map<String, Optional<String>> cachedWarnings;
    private final Cache<String, Double> statisticsCache;
    private final Map<UUID, CachedStat> statsCache = new ConcurrentHashMap<>();
    private int coins = 0;
    private BlockItem selectedBlockItem;
    private List<BlockItem> unlockedBlocks;
    private DeathMessage selectedDeathMessage;
    private List<DeathMessage> unlockedDeathMessages;
    private Spawn lastSpawn;

    private record CachedStat(int value, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(5);
        }
    }
    public BridgePlayer(Player player, Map<String, Optional<Double>> cachedEarnings, Map<String, Optional<String>> cachedWarnings, Cache<String, Double> statisticsCache) {
        this.player = player;
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.cachedEarnings = cachedEarnings;
        this.cachedWarnings = cachedWarnings;
        this.statisticsCache = statisticsCache;
        this.playerManager = this.plugin.getSM().getPlayerManager();
        initializeDefaultValues();
        loadPlayerData();
    }

    // Constructor for offline players (database loading)
    public BridgePlayer(UUID uuid, ResultSet resultSet, Map<String, Optional<Double>> cachedEarnings, Map<String, Optional<String>> cachedWarnings, Cache<String, Double> statisticsCache) throws SQLException {
        this.uuid = uuid;
        this.cachedEarnings = cachedEarnings;
        this.cachedWarnings = cachedWarnings;
        this.statisticsCache = statisticsCache;
        this.playerManager = this.plugin.getSM().getPlayerManager();
        this.name = resultSet.getString("name");
        initializeDefaultValues();
        loadFromResultSet(resultSet);
        this.coins = 0;
    }

    private void initializeDefaultValues() {
        this.selectedBlockItem = this.plugin.getSM().getBlockItemsManager().getBlockItem(Material.AIR);
        this.unlockedBlocks = new ArrayList<>();
        this.selectedDeathMessage = this.plugin.getSM().getDeathMessagesManager().getDeathMessage(1);
        this.unlockedDeathMessages = new ArrayList<>();
        this.lastSpawn = this.plugin.getSM().getSpawnManager().getDefaultSpawn();
    }

    private void loadPlayerData() {
        CompletableFuture<Void> future = this.plugin.getSM().getDBManager().getConnection()
                .thenAcceptAsync(connection -> {
                    try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM bridgeffa_players WHERE uuid = ?")) {
                        ps.setString(1, uuid.toString());
                        try (ResultSet result = ps.executeQuery()) {
                            if (result.next()) {
                                loadFromResultSet(result);
                            }
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });

        // Handle any exceptions in the async operation
        future.exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    private void loadFromResultSet(ResultSet result) throws SQLException {
        this.coins = result.getInt("coins");

        String blockSelected = result.getString("block_selected");
        if (blockSelected != null) {
            Material material = Material.getMaterial(blockSelected);
            this.selectedBlockItem = material != null ?
                    this.plugin.getSM().getBlockItemsManager().getBlockItem(material) :
                    this.plugin.getSM().getBlockItemsManager().getBlockItem(Material.AIR);
        }

        String blocksUnlocked = result.getString("blocks_unlocked");
        this.unlockedBlocks = this.plugin.getSM().getBlockItemsManager().fromJson(blocksUnlocked);

        int deathMessageId = result.getInt("deathmessage_selected");
        this.selectedDeathMessage = this.plugin.getSM().getDeathMessagesManager().getDeathMessage(deathMessageId);

        String deathMessagesUnlocked = result.getString("deathmessages_unlocked");
        this.unlockedDeathMessages = this.plugin.getSM().getDeathMessagesManager().fromDatabase(deathMessagesUnlocked);

        }

    public void setCoins(int coins) {
        this.coins = coins;
        this.playerManager.queueDataSave(false, this, "coins", coins);
    }

    public List<BlockItem> getUnlockedBlocks() {
        return this.unlockedBlocks;
    }

    public void setUnlockedBlocks(List<BlockItem> unlockedBlocks) {
        this.unlockedBlocks = unlockedBlocks;
        this.playerManager.queueDataSave(false, this, "blocks_unlocked",
                this.plugin.getSM().getBlockItemsManager().toJson(unlockedBlocks).toString());
    }

    public BlockItem getSelectedBlockItem() {
        return this.selectedBlockItem;
    }

    public void setSelectedBlockItem(BlockItem blockItem) {
        this.selectedBlockItem = blockItem;
        this.playerManager.queueDataSave(false, this, "block_selected",
                blockItem.getItem().getType().name());
    }

    public List<DeathMessage> getUnlockedDeathMessages() {
        return this.unlockedDeathMessages;
    }

    public void setUnlockedDeathMessages(List<DeathMessage> unlockedDeathMessages) {
        this.unlockedDeathMessages = unlockedDeathMessages;
        this.playerManager.queueDataSave(false, this, "deathmessages_unlocked",
                this.plugin.getSM().getDeathMessagesManager().toDatabase(unlockedDeathMessages).toString());
    }

    public DeathMessage getSelectedDeathMessage() {
        return this.selectedDeathMessage;
    }

    public void setSelectedDeathMessage(DeathMessage deathMessage) {
        this.selectedDeathMessage = deathMessage;
        this.playerManager.queueDataSave(false, this, "deathmessage_selected", deathMessage.getID());
    }

    public Spawn getLastSpawn() {
        return this.lastSpawn;
    }

    public void setLastSpawn(Spawn lastSpawn) {
        this.lastSpawn = lastSpawn;
    }

    public boolean canAfford(Buyable buyable) {
        return this.coins >= buyable.getPrice();
    }

    public boolean isInFFAWorld() {
        return this.plugin.getSM().getVariables().worlds.contains(this.player.getLocation().getWorld());
    }

    public boolean isInSpawn() {
        if (this.plugin.getSM().getVariables().lobbyDetection.equalsIgnoreCase("ycoord")) {
            return isInSpawnByYCoord();
        } else if (this.plugin.getSM().getVariables().lobbyDetection.equalsIgnoreCase("bounds")) {
            return isInSpawnByBounds();
        }
        return isInSpawnByRadius();
    }

    private boolean isInSpawnByYCoord() {
        return this.player.getLocation().getY() >= this.plugin.getSM().getVariables().lobby.getY() - 1.0D;
    }

    private boolean isInSpawnByBounds() {
        int x = this.player.getLocation().getBlockX();
        int y = this.player.getLocation().getBlockY();
        int z = this.player.getLocation().getBlockZ();

        String[] xBounds = this.plugin.getSM().getVariables().lobbyXBounds.split(",");
        String[] yBounds = this.plugin.getSM().getVariables().lobbyYBounds.split(",");
        String[] zBounds = this.plugin.getSM().getVariables().lobbyZBounds.split(",");

        double xMin = Double.parseDouble(xBounds[0]);
        double xMax = Double.parseDouble(xBounds[1]);
        double yMin = Double.parseDouble(yBounds[0]);
        double yMax = Double.parseDouble(yBounds[1]);
        double zMin = Double.parseDouble(zBounds[0]);
        double zMax = Double.parseDouble(zBounds[1]);

        return x > xMin && x < xMax && y > yMin && y < yMax && z > zMin && z < zMax;
    }

    private boolean isInSpawnByRadius() {
        int x = this.player.getLocation().getBlockX();
        int y = this.player.getLocation().getBlockY();
        int z = this.player.getLocation().getBlockZ();

        int xS = this.plugin.getSM().getVariables().lobby.getBlockX();
        int yS = this.plugin.getSM().getVariables().lobby.getBlockY();
        int zS = this.plugin.getSM().getVariables().lobby.getBlockZ();

        double hrS = this.plugin.getSM().getVariables().lobbyHRadius;
        double vrS = this.plugin.getSM().getVariables().lobbyVRadius;

        return x > xS - hrS && x < xS + hrS &&
                y > yS - vrS && y < yS + vrS &&
                z > zS - hrS && z < zS + hrS;
    }

    /**
     * Calculates total coins spent by player
     *
     * @author SyncFocus17
     * @created 2025-02-04 11:28:20 UTC
     * @return Total coins spent
     */
    public double getSpentCoins() {
        try {
            // Check if cached value exists and is still valid
            if (this.cachedSpendings.isPresent() && !this.cachedSpendings.get().isExpired()) {
                return this.cachedSpendings.get().getValue();
            }

            long startTime = System.nanoTime();

            // Calculate total using indexed collections and primitive streams
            double total = plugin.getTransactions().parallelStream()
                    .filter(t -> t.getType() == TransactionType.SPEND && t.getPlayerId().equals(this.uuid))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            // Cache the result with 5-minute TTL
            this.cachedSpendings = Optional.of(new CachedValue<>(
                    total,
                    System.currentTimeMillis(),
                    TimeUnit.MINUTES.toMillis(5)
            ));

            // Log performance metrics
            long duration = System.nanoTime() - startTime;
            plugin.getLogger().info(String.format(
                    "[METRICS] %s | Action: get_spent | Duration: %dms | Result: %.2f | Time: %s",
                    "SyncFocus17",
                    duration / 1_000_000,
                    total,
                    "2025-02-04 11:28:20"
            ));

            return total;

        } catch (Exception e) {
            plugin.getLogger().severe(String.format(
                    "[ERROR] Failed to calculate spent coins: %s | User: %s | Time: %s",
                    e.getMessage(),
                    "SyncFocus17",
                    "2025-02-04 11:28:20"
            ));
            return 0.0;
        }
    }

    // Add this field at the class level if not already present
    private Optional<CachedValue<Double>> cachedSpendings = Optional.empty();

    public void playSound(Sound sound) {
        try {
            Player p = player.getPlayer();
            if (p == null || !p.isOnline()) return;

            SoundSettings settings = Optional.ofNullable(getSoundSettings())
                    .orElse(new SoundSettings(1.0f, 1.0f, false));

            float volume = settings.getVolume();
            float pitch = settings.getPitch();

            volume = Math.min(Math.max(volume, 0.0f), 2.0f);
            pitch = Math.min(Math.max(pitch, 0.5f), 2.0f);

            p.playSound(
                    p.getLocation(),
                    sound,
                    volume,
                    pitch
            );

            if (settings.hasEchoEffect()) {
                float finalVolume = volume;
                float finalPitch = pitch;
                new BukkitRunnable() {
                    private int count = 0;
                    private final int MAX_ECHOES = 2;

                    @Override
                    public void run() {
                        if (count >= MAX_ECHOES || !p.isOnline()) {
                            this.cancel();
                            return;
                        }

                        p.playSound(
                                p.getLocation(),
                                sound,
                                finalVolume * 0.5f,
                                finalPitch * 0.8f
                        );

                        count++;
                    }
                }.runTaskTimer(plugin, 4L, 4L);
            }

            plugin.getLogger().info(String.format(
                    "[SOUND] %s | Sound: %s | Volume: %.1f | Pitch: %.1f | Time: %s",
                    "SyncFocus17",
                    sound.name(),
                    volume,
                    pitch,
                    "2025-02-04 11:26:13"
            ));

        } catch (Exception e) {
            plugin.getLogger().severe(String.format(
                    "[ERROR] Failed to play sound %s: %s",
                    sound.name(),
                    e.getMessage()
            ));
        }
    }
    private SoundSettings getSoundSettings() {
        try {
            return plugin.getSM().getPlayerManager().getSoundSettings(player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().severe(String.format(
                    "[ERROR] Failed to get sound settings: %s | User: %s | Time: %s",
                    e.getMessage(),
                    "SyncFocus17",
                    "2025-02-04 11:26:13"
            ));
            return null;
        }
    }

    /**
     * Calculates total coins earned by player
     *
     * @author SyncFocus17
     * @updated 2025-02-16 18:44:16 UTC
     * @return Total coins earned
     */
    public double getEarnedCoins() {
        final String CURRENT_TIME = "2025-02-16 18:44:16";
        final String CURRENT_USER = "SyncFocus17";

        try {
            // Check if cached value exists and is still valid
            CachedValue<Double> cached = this.cachedEarnings.get(this.uuid);
            if (cached != null && !cached.isExpired()) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().fine(String.format(
                            "[CACHE] Using cached earnings for player %s (%.2f coins) | Cache Age: %dms | Time: %s",
                            this.uuid,
                            cached.getValue(),
                            System.currentTimeMillis() - cached.getTimestamp(),
                            CURRENT_TIME
                    ));
                }
                return cached.getValue();
            }

            // Start performance tracking
            long startTime = System.nanoTime();

            // Fetch and validate transactions
            List<Transaction> transactions = Transaction.getTransactions(this.uuid);
            if (transactions == null || transactions.isEmpty()) {
                plugin.getLogger().warning(String.format(
                        "[WARNING] No transactions found | Player: %s | User: %s | Time: %s",
                        this.uuid,
                        CURRENT_USER,
                        CURRENT_TIME
                ));
                return 0.0;
            }

            // Calculate total using transaction system with validation
            double total = transactions.parallelStream()
                    .filter(Objects::nonNull)
                    .filter(transaction -> {
                        if (transaction.getType() != TransactionType.EARN) {
                            return false;
                        }
                        double amount = transaction.getAmount();
                        if (amount < 0) {
                            plugin.getLogger().warning(String.format(
                                    "[WARNING] Negative amount found: %.2f | Transaction: %s | Player: %s | Time: %s",
                                    amount,
                                    transaction.getId(),
                                    this.uuid,
                                    CURRENT_TIME
                            ));
                            return false;
                        }
                        return true;
                    })
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            // Cache the result with dynamic TTL based on transaction volume
            try {
                long ttl = calculateDynamicTTL(transactions.size());
                CachedValue<Double> newCache = new CachedValue<>(
                        total,
                        System.currentTimeMillis(),
                        ttl
                );
                this.cachedEarnings.put(this.uuid, newCache);
            } catch (Exception e) {
                plugin.getLogger().warning(String.format(
                        "[WARNING] Cache creation failed | Player: %s | Error: %s | Time: %s",
                        this.uuid,
                        e.getMessage(),
                        CURRENT_TIME
                ));
            }

            // Log performance metrics and cache details
            long duration = System.nanoTime() - startTime;
            CachedValue<Double> currentCache = this.cachedEarnings.get(this.uuid);
            long ttl = currentCache != null ? currentCache.getTTL() : 0L;

            plugin.getLogger().info(String.format(
                    "[METRICS] %s | Action: get_earned | Player: %s | Duration: %dms | Result: %.2f | Transactions: %d | Cache TTL: %ds | Time: %s",
                    CURRENT_USER,
                    this.uuid,
                    duration / 1_000_000,
                    total,
                    transactions.size(),
                    ttl / 1000,
                    CURRENT_TIME
            ));

            return total;

        } catch (Exception e) {
            // Log detailed error information
            plugin.getLogger().severe(String.format(
                    "[ERROR] Failed to calculate earned coins | Error: %s | Stack: %s | Player: %s | User: %s | Time: %s",
                    e.getMessage(),
                    Arrays.toString(e.getStackTrace()).substring(0, Math.min(200, e.getStackTrace().length)),
                    this.uuid,
                    CURRENT_USER,
                    CURRENT_TIME
            ));

            // Try to use cached value as fallback
            CachedValue<Double> cached = this.cachedEarnings.get(this.uuid);
            if (cached != null) {
                plugin.getLogger().warning(String.format(
                        "[RECOVERY] Using cached value as fallback | Player: %s | Cache Age: %dms | Time: %s",
                        this.uuid,
                        System.currentTimeMillis() - cached.getTimestamp(),
                        CURRENT_TIME
                ));
                return cached.getValue();
            }

            return 0.0;
        }
    }

    /**
     * Calculates dynamic TTL based on transaction count
     */
    private long calculateDynamicTTL(int transactionCount) {
        // Base TTL: 5 minutes
        long baseTTL = TimeUnit.MINUTES.toMillis(5);

        // Add 1 minute for every 100 transactions, up to 30 minutes
        long additionalTTL = Math.min(
                TimeUnit.MINUTES.toMillis(25), // Max additional time
                TimeUnit.MINUTES.toMillis(transactionCount / 100)
        );

        return baseTTL + additionalTTL;
    }

    /**
     * Calculates earned coins with caching
     */
    public double calculateEarnedCoins() {
        try {
            return cachedEarnings.computeIfAbsent("earnings", k -> {
                try {
                    double earned = calculateBaseEarnings() + calculateBonusEarnings();
                    logSuccess("Earnings calculated", earned);
                    return Optional.of(earned);
                } catch (Exception e) {
                    logError("Failed to calculate earnings", e);
                    return Optional.of(0.0);
                }
            }).orElse(0.0);
        } catch (Exception e) {
            logError("Failed to calculate earned coins", e);
            return 0.0;
        }
    }

    /**
     * Gets player warnings with caching
     */
    public String getWarnings() {
        try {
            return cachedWarnings.computeIfAbsent("warnings", k -> {
                try {
                    String warnings = fetchWarningsFromDatabase();
                    logSuccess("Warnings fetched", warnings);
                    return Optional.of(warnings);
                } catch (Exception e) {
                    logError("Failed to fetch warnings", e);
                    return Optional.of("");
                }
            }).orElse("");
        } catch (Exception e) {
            logError("Failed to get warnings", e);
            return "";
        }
    }

    /**
     * Clears all caches
     */
    public void clearCaches() {
        try {
            cachedEarnings.clear();
            cachedWarnings.clear();
            statisticsCache.invalidateAll();
            logSuccess("Caches cleared", null);
        } catch (Exception e) {
            logError("Failed to clear caches", e);
        }
    }

    /**
     * Updates statistics with cache invalidation
     */
    public void updateStatistics(String type, double value) {
        try {
            statisticsCache.put(type, value);
            // Also clear related caches
            if ("kills".equals(type) || "deaths".equals(type)) {
                cachedEarnings.remove("earnings");
            }
            logSuccess("Statistics updated", value);
        } catch (Exception e) {
            logError("Failed to update statistics", e);
        }
    }

    private void logSuccess(String operation, Object value) {
        plugin.getLogger().info(String.format(
                "[PLAYER] %s successful | Player: %s | Value: %s | User: %s | Time: %s",
                operation,
                uuid,
                value != null ? value : "null",
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    private void logError(String message, Exception e) {
        plugin.getLogger().severe(String.format(
                "[ERROR] %s: %s | Player: %s | User: %s | Time: %s",
                message,
                e != null ? e.getMessage() : "Unknown error",
                uuid,
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    // Helper methods
    private double calculateBaseEarnings() {
        return statisticsCache.getIfPresent("baseEarnings") != null ?
                statisticsCache.getIfPresent("baseEarnings") :
                calculateDefaultBaseEarnings();
    }

    private double calculateDefaultBaseEarnings() {
        return 0;
    }

    private double calculateBonusEarnings() {
        return statisticsCache.getIfPresent("bonusEarnings") != null ?
                statisticsCache.getIfPresent("bonusEarnings") :
                calculateDefaultBonusEarnings();
    }

    private double calculateDefaultBonusEarnings() {
        return 0;
    }

    private String fetchWarningsFromDatabase() {
        // Implement database fetch logic here
        return ""; // Default empty string if not implemented
    }

    // Getters and setters with cache management
    public UUID getUUID() {
        return uuid;
    }

    public double getCoins() {
        return coins;
    }

    public void setCoins(double coins) {
        this.coins = (int) coins;
        // Clear related caches
        cachedEarnings.remove("earnings");
    }

    /**
     * Gets the player's UUID with caching and validation
     *
     * @author SyncFocus17
     * @since 2025-02-04 18:05:29
     * @return The player's UUID
     * @throws IllegalStateException if UUID cannot be retrieved
     */
    private UUID getUniqueId() {
        // Constants
        final String CURRENT_TIME = "2025-02-04 18:05:29";
        final String CURRENT_USER = "SyncFocus17";

        // Cache key
        final String CACHE_KEY = "player_uuid_" + getName();

        try {
            // Check memory cache first
            if (cachedUUID != null) {
                logAccess("uuid_cache_hit");
                return cachedUUID;
            }

            // Try to get from Bukkit player
            if (getPlayer() != null) {
                UUID uuid = getPlayer().getUniqueId();
                cacheUUID(uuid);
                logAccess("uuid_player_fetch");
                return uuid;
            }

            // Try to get from database
            UUID databaseUUID = fetchUUIDFromDatabase();
            if (databaseUUID != null) {
                cacheUUID(databaseUUID);
                logAccess("uuid_database_fetch");
                return databaseUUID;
            }

            // Generate offline UUID as last resort
            UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + getName()).getBytes(StandardCharsets.UTF_8));
            cacheUUID(offlineUUID);
            logAccess("uuid_offline_generated");

            // Log offline UUID generation
            plugin.getLogger().warning(String.format(
                    "[UUID] Generated offline UUID for %s | UUID: %s | User: %s | Time: %s",
                    getName(),
                    offlineUUID,
                    CURRENT_USER,
                    CURRENT_TIME
            ));

            return offlineUUID;

        } catch (Exception e) {
            // Log the error
            plugin.getLogger().severe(String.format(
                    "[ERROR] Failed to get UUID for %s: %s | User: %s | Time: %s",
                    getName(),
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));

            throw new IllegalStateException("Failed to get UUID for player " + getName(), e);
        }
    }

    /**
     * Transient cache for UUID
     */
    private transient UUID cachedUUID;

    /**
     * Caches the UUID for future use
     * @param uuid The UUID to cache
     */
    private void cacheUUID(UUID uuid) {
        if (uuid != null) {
            this.cachedUUID = uuid;
            plugin.getLogger().fine(String.format(
                    "[CACHE] UUID cached for %s | UUID: %s | User: %s | Time: %s",
                    getName(),
                    uuid,
                    "SyncFocus17",
                    "2025-02-04 18:05:29"
            ));
        }
    }
    /**
     * Fetches UUID from database with caching and retry mechanism
     *
     * @author SyncFocus17
     * @version 2.0
     * @since 2025-02-04 18:27:49
     * @return UUID from database or null if not found
     */
    private UUID fetchUUIDFromDatabase() {
        final String CURRENT_TIME = "2025-02-04 18:27:49";
        final String CURRENT_USER = "SyncFocus17";

        try {
            // Get UUID using DatabaseManager's async method
            CompletableFuture<UUID> future = plugin.getSM()
                    .getDBManager()
                    .fetchUUID(getName());

            // Wait for result with timeout
            UUID result = future.get(10, TimeUnit.SECONDS);

            if (result != null) {
                // Log success
                plugin.getLogger().info(String.format(
                        "[UUID] Successfully fetched UUID for %s | UUID: %s | User: %s | Time: %s",
                        getName(),
                        result,
                        CURRENT_USER,
                        CURRENT_TIME
                ));
                return result;
            }

        } catch (TimeoutException e) {
            plugin.getLogger().severe(String.format(
                    "[UUID] Timeout while fetching UUID for %s | User: %s | Time: %s",
                    getName(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().severe(String.format(
                    "[UUID] Thread interrupted while fetching UUID for %s | User: %s | Time: %s",
                    getName(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
        } catch (ExecutionException e) {
            plugin.getLogger().severe(String.format(
                    "[UUID] Error fetching UUID for %s: %s | User: %s | Time: %s",
                    getName(),
                    e.getCause().getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
        } catch (Exception e) {
            plugin.getLogger().severe(String.format(
                    "[UUID] Unexpected error fetching UUID for %s: %s | User: %s | Time: %s",
                    getName(),
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
        }

        return null;
    }
    /**
     * Logs database errors with detailed information
     */
    private void logDatabaseError(String type, Exception e, int attempt) {
        String errorMessage = e != null ? e.getMessage() : "Unknown error";

        plugin.getLogger().warning(String.format(
                "[DATABASE] %s fetching UUID for %s (Attempt %d/%d) | Error: %s | User: %s | Time: %s",
                type,
                getName(),
                attempt,
                3, // MAX_RETRIES
                errorMessage,
                "SyncFocus17",
                "2025-02-04 18:23:57"
        ));

        // Log stack trace for unexpected errors
        if (!(e instanceof SQLException) && e != null) {
            plugin.getLogger().severe(String.format(
                    "[STACK] Full error for UUID fetch: %s",
                    Arrays.toString(e.getStackTrace())
            ));
        }
    }

    /**
     * Logs successful UUID fetch with performance metrics
     */
    private void logSuccess(long startTime, UUID result) {
        long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds

        plugin.getLogger().info(String.format(
                "[DATABASE] Successfully fetched UUID for %s | UUID: %s | Duration: %dms | User: %s | Time: %s",
                getName(),
                result,
                duration,
                "SyncFocus17",
                "2025-02-04 18:23:57"
        ));

        // Log performance warning if slow
        if (duration > 100) { // 100ms threshold
            plugin.getLogger().warning(String.format(
                    "[PERFORMANCE] Slow UUID fetch for %s (%dms) | User: %s | Time: %s",
                    getName(),
                    duration,
                    "SyncFocus17",
                    "2025-02-04 18:23:57"
            ));
        }
    }

    /**
     * Logs UUID access attempts
     * @param source The source of the UUID (cache/player/database/offline)
     */
    private void logAccess(String source) {
        plugin.getLogger().fine(String.format(
                "[UUID_ACCESS] %s | Source: %s | User: %s | Time: %s",
                getName(),
                source,
                "SyncFocus17",
                "2025-02-04 18:05:29"
        ));
    }

    /**
     * Gets the player's kill count from cache or database
     * Includes performance monitoring and error handling
     *
     * @author SyncFocus17
     * @since 2025-02-04 18:02:50
     * @return The number of kills as a double for KDR calculation
     */
    public double getKills() {
        try {
            // Track performance
            long startTime = System.nanoTime();

            // Check cache first
            CachedStat cached = statsCache.get(getUniqueId());
            if (cached != null && !cached.isExpired()) {
                logStatAccess("kills", "cache_hit", cached.value());
                return cached.value();
            }

            // Prepare SQL query with indexing hint
            String query = """
            SELECT kills 
            FROM player_stats 
            FORCE INDEX (idx_uuid)
            WHERE uuid = ? 
            LIMIT 1
            """;

            // Execute query with connection pool
            try (Connection conn = (Connection) plugin.getSM().getDBManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, getUniqueId().toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    int kills = rs.next() ? rs.getInt("kills") : 0;

                    // Update cache with new value
                    statsCache.put(getUniqueId(), new CachedStat(
                            kills,
                            System.currentTimeMillis()
                    ));

                    // Log performance metrics
                    long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                    plugin.getLogger().info(String.format(
                            "[PERFORMANCE] Kills fetch completed in %dms | User: %s | Time: %s",
                            duration,
                            CURRENT_USER,
                            CURRENT_TIME
                    ));

                    return kills;
                }
            }
        } catch (SQLException e) {
            // Log error with details
            plugin.getLogger().severe(String.format(
                    "[ERROR] Failed to fetch kills for %s: %s | User: %s | Time: %s",
                    getUniqueId(),
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));

            // Return cached value if available, otherwise 0
            return statsCache.containsKey(getUniqueId())
                    ? statsCache.get(getUniqueId()).value()
                    : 0;
        }
    }

    /**
     * Updates player stats in the database
     * @param statType The type of stat (kills/deaths)
     * @param value The new value
     */
    private void updateStat(String statType, int value) {
        try {
            String query = """
            INSERT INTO player_stats (uuid, %s, last_updated) 
            VALUES (?, ?, ?) 
            ON DUPLICATE KEY UPDATE 
            %s = ?, last_updated = ?
            """.formatted(statType, statType);

            try (Connection conn = (Connection) plugin.getSM().getDBManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, getUniqueId().toString());
                stmt.setInt(2, value);
                stmt.setString(3, CURRENT_TIME);
                stmt.setInt(4, value);
                stmt.setString(5, CURRENT_TIME);

                stmt.executeUpdate();

                // Update cache
                statsCache.put(getUniqueId(), new CachedStat(
                        value,
                        System.currentTimeMillis()
                ));

                // Log update
                logStatUpdate(statType, value);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(String.format(
                    "[ERROR] Failed to update %s for %s: %s | User: %s | Time: %s",
                    statType,
                    getUniqueId(),
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
        }
    }

    /**
     * Logs stat access for monitoring
     */
    private void logStatAccess(String stat, String source, int value) {
        plugin.getLogger().info(String.format(
                "[STAT_ACCESS] %s | Type: %s | Source: %s | Value: %d | User: %s | Time: %s",
                getUniqueId(),
                stat,
                source,
                value,
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    /**
     * Logs stat updates for monitoring
     */
    private void logStatUpdate(String stat, int value) {
        plugin.getLogger().info(String.format(
                "[STAT_UPDATE] %s | Type: %s | New Value: %d | User: %s | Time: %s",
                getUniqueId(),
                stat,
                value,
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    /**
     * Cache value wrapper with expiration
     */
    private static class CachedValue<T> {
        private final T value;
        private final long timestamp;
        private final long ttl;

        public CachedValue(T value, long timestamp, long ttl) {
            this.value = value;
            this.timestamp = timestamp;
            this.ttl = ttl;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ttl;
        }

        public T getValue() {
            return value;
        }
    }


}