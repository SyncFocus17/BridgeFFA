package gg.azura.bridges;

import gg.azura.bridges.commands.BridgeFFACommand;
import gg.azura.bridges.commands.KillCommand;
import gg.azura.bridges.ffa.listeners.*;
import gg.azura.bridges.listeners.*;
import gg.azura.bridges.tasks.ArmorstandBlockingTask;
import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Modern implementation of the Bridges plugin main class
 *
 * @author SyncFocus17
 * @version 2.0
 * @since 2025-02-04 18:12:56
 */
@Getter
public final class Bridges extends JavaPlugin {

    private static final String CURRENT_TIME = "2025-02-04 18:12:56";
    private static final String CURRENT_USER = "SyncFocus17";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Bridges plugin;
    private ServicesManager servicesManager;
    private final Map<UUID, Transaction> transactionCache;
    private final List<Transaction> transactions;

    public Bridges() {
        this.transactionCache = new ConcurrentHashMap<>();
        this.transactions = new CopyOnWriteArrayList<>();
    }

    /**
     * Gets the plugin instance
     * @return The plugin instance
     */
    public static Bridges get() {
        return plugin;
    }

    @Override
    public void onEnable() {
        try {
            // Initialize plugin
            plugin = this;

            // Initialize services
            initializeServices();

            // Load online players
            loadOnlinePlayers();

            // Register listeners and commands
            registerListenersAndCommands();

            // Start tasks
            startTasks();

            // Log successful enable
            getLogger().info(String.format(
                    "[ENABLE] Plugin enabled successfully | Version: %s | Time: %s",
                    getDescription().getVersion(),
                    CURRENT_TIME
            ));

        } catch (Exception e) {
            // Log error and disable plugin
            getLogger().severe(String.format(
                    "[ERROR] Failed to enable plugin: %s | User: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initializes the services manager
     */
    private void initializeServices() {
        try {
            this.servicesManager = new ServicesManager(this, null);
            getLogger().info(String.format(
                    "[SERVICES] Initialized ServicesManager | Time: %s",
                    CURRENT_TIME
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ServicesManager", e);
        }
    }

    /**
     * Loads all online players
     */
    private void loadOnlinePlayers() {
        getServer().getOnlinePlayers().forEach(this::loadPlayer);
    }

    /**
     * Loads a player into the system
     */
    private void loadPlayer(Player player) {
        try {
            servicesManager.getPlayerManager().addPlayer(player);
            getLogger().info(String.format(
                    "[PLAYER] Loaded player %s | Time: %s",
                    player.getName(),
                    CURRENT_TIME
            ));
        } catch (Exception e) {
            getLogger().warning(String.format(
                    "[WARNING] Failed to load player %s: %s | Time: %s",
                    player.getName(),
                    e.getMessage(),
                    CURRENT_TIME
            ));
        }
    }

    /**
     * Registers all listeners and commands
     */
    private void registerListenersAndCommands() {
        // Register listeners
        PluginManager pm = getServer().getPluginManager();
        Arrays.asList(
                new JoinQuitListener(this),
                new GameListener(this),
                new ArmorstandListener(this),
                new DeathListener(this),
                new ProtectionListener(this)
        ).forEach(listener -> {
            pm.registerEvents(listener, this);
            logListenerRegistration(listener);
        });

        // Register commands
        getCommand("bridgeffa").setExecutor(new BridgeFFACommand(this));
        getCommand("kill").setExecutor(new KillCommand(this));

        getLogger().info(String.format(
                "[REGISTER] Completed registration | Time: %s",
                CURRENT_TIME
        ));
    }

    /**
     * Starts scheduled tasks
     */
    private void startTasks() {
        new ArmorstandBlockingTask(this).runTaskTimer(this, 100L, 60L);
        getLogger().info(String.format(
                "[TASKS] Started scheduled tasks | Time: %s",
                CURRENT_TIME
        ));
    }

    @Override
    public void onDisable() {
        try {
            // Cancel tasks
            getServer().getScheduler().cancelTasks(this);

            // Close inventories
            getServer().getOnlinePlayers().forEach(HumanEntity::closeInventory);

            // Save player data
            servicesManager.getPlayerManager().saveNow(false);

            // Clean up blocks
            servicesManager.getBlocksManager().getBlocks().forEach(BridgeBlock::remove);

            // Unregister PAPI expansion
            if (servicesManager.getPAPIExpansion() != null) {
                servicesManager.getPAPIExpansion().unregister();
            }

            // Clear caches
            transactionCache.clear();
            transactions.clear();

            // Log disable
            getLogger().info(String.format(
                    "[DISABLE] Plugin disabled successfully | Time: %s",
                    CURRENT_TIME
            ));

        } catch (Exception e) {
            getLogger().severe(String.format(
                    "[ERROR] Error during plugin disable: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_TIME
            ));
        } finally {
            plugin = null;
        }
    }

    /**
     * Gets all transactions for the player
     * @return Collection of transactions
     */
    public Collection<Transaction> getTransactions() {
        try {
            UUID playerId = servicesManager.getTransactionManager().getPlayerId();
            if (playerId == null) {
                getLogger().warning(String.format(
                        "[TRANSACTIONS] No player ID available | Time: %s",
                        CURRENT_TIME
                ));
                return Collections.emptyList();
            }

            // Try to get from cache first
            if (transactionCache.containsKey(playerId)) {
                return Collections.singletonList(transactionCache.get(playerId));
            }

            // Get from database
            Collection<Transaction> playerTransactions =
                    Transaction.getTransactions(playerId);

            // Cache the results
            playerTransactions.forEach(t ->
                    transactionCache.put(playerId, t));

            return playerTransactions;

        } catch (Exception e) {
            getLogger().severe(String.format(
                    "[ERROR] Failed to get transactions: %s | User: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
            return Collections.emptyList();
        }
    }

    /**
     * Logs listener registration
     */
    private void logListenerRegistration(Listener listener) {
        getLogger().info(String.format(
                "[LISTENER] Registered %s | Time: %s",
                listener.getClass().getSimpleName(),
                CURRENT_TIME
        ));
    }

    /**
     * Parses the server version
     * @return The server version as a double
     */
    public double parseVersion() {
        try {
            String version = getServer().getBukkitVersion();
            String[] parts = version.split("\\.", 2);
            String[] subParts = parts[1].split("-");
            return Double.parseDouble(subParts[0]);
        } catch (Exception e) {
            getLogger().warning(String.format(
                    "[VERSION] Failed to parse version: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_TIME
            ));
            return 0.0;
        }
    }

    public ServicesManager getSM() {
        return servicesManager;
    }
}