package gg.azura.bridges;

import gg.azura.bridges.ffa.services.ArmorstandManager;
import gg.azura.bridges.ffa.services.SpawnManager;
import gg.azura.bridges.gui.SoundSettings;
import gg.azura.bridges.services.*;
import gg.azura.bridges.utils.PAPIExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ServicesManager {

    private final Variables variables;
    private final DatabaseManager databaseManager;
    private final PlayerManager playerManager;
    private final BlocksManager blocksManager;
    private final BlockItemsManager blockItemsManager;
    private final SpawnManager spawnManager;
    private final ArmorstandManager armorstandManager;
    private final DeathMessagesManager deathMessagesManager;
    private final Transaction transaction;
    private PAPIExpansion papiExpansion;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LocalDateTime CURRENT_TIME = LocalDateTime.parse("2025-02-04 17:49:54", DATE_FORMATTER);
    private static final String CURRENT_USER = "Administrator";

    public ServicesManager(Bridges plugin, Transaction transaction) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin instance cannot be null");
        }

        try {
            // Initialize transaction with a default UUID if none provided
            this.transaction = transaction != null ? transaction :
                    new Transaction(UUID.randomUUID(), TransactionType.EARN, 0.0, plugin);

            // Initialize other services
            this.variables = new Variables(plugin);
            this.databaseManager = new DatabaseManager(this.variables, plugin);
            this.blocksManager = new BlocksManager(plugin);
            this.blockItemsManager = new BlockItemsManager(plugin);
            this.spawnManager = new SpawnManager(plugin);
            this.armorstandManager = new ArmorstandManager(plugin);
            this.deathMessagesManager = new DeathMessagesManager(plugin);
            this.playerManager = new PlayerManager(plugin, new SoundSettings());

            // Initialize PAPI expansion if available
            if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                this.papiExpansion = new PAPIExpansion(plugin);
                this.papiExpansion.register();
            }

            // Log successful initialization
            plugin.getLogger().info(String.format(
                    "[SERVICES] Initialized ServicesManager | User: %s | Time: %s",
                    CURRENT_USER,
                    CURRENT_TIME.format(DATE_FORMATTER)
            ));
        } catch (Exception e) {
            plugin.getLogger().severe(String.format(
                    "[ERROR] Failed to initialize ServicesManager: %s | User: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME.format(DATE_FORMATTER)
            ));
            throw new RuntimeException("Failed to initialize ServicesManager", e);
        }
    }

    public static Transaction createTransactionForPlayer(String playerName, Bridges plugin) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return new Transaction(UUID.randomUUID(), TransactionType.EARN, 0.0, plugin);
        }

        Player player = Bukkit.getPlayer(playerName);
        UUID playerId = player != null ? player.getUniqueId() : UUID.nameUUIDFromBytes(playerName.getBytes());
        return new Transaction(playerId, TransactionType.EARN, 0.0, plugin);
    }

    public Variables getVariables() {
        checkInitialized(variables, "Variables");
        return variables;
    }

    public DatabaseManager getDBManager() {
        checkInitialized(databaseManager, "DatabaseManager");
        return databaseManager;
    }

    public PlayerManager getPlayerManager() {
        checkInitialized(playerManager, "PlayerManager");
        return playerManager;
    }

    public BlocksManager getBlocksManager() {
        checkInitialized(blocksManager, "BlocksManager");
        return blocksManager;
    }

    public BlockItemsManager getBlockItemsManager() {
        checkInitialized(blockItemsManager, "BlockItemsManager");
        return blockItemsManager;
    }

    public SpawnManager getSpawnManager() {
        checkInitialized(spawnManager, "SpawnManager");
        return spawnManager;
    }

    public ArmorstandManager getArmorstandManager() {
        checkInitialized(armorstandManager, "ArmorstandManager");
        return armorstandManager;
    }

    public DeathMessagesManager getDeathMessagesManager() {
        checkInitialized(deathMessagesManager, "DeathMessagesManager");
        return deathMessagesManager;
    }

    public PAPIExpansion getPAPIExpansion() {
        return papiExpansion;
    }

    public Transaction getTransactionManager() {
        checkInitialized(transaction, "Transaction");
        return transaction;
    }

    private void checkInitialized(Object service, String serviceName) {
        if (service == null) {
            String errorMsg = String.format(
                    "%s has not been properly initialized | User: %s | Time: %s",
                    serviceName,
                    CURRENT_USER,
                    CURRENT_TIME.format(DATE_FORMATTER)
            );
            throw new IllegalStateException(errorMsg);
        }
    }

    public boolean isFullyInitialized() {
        boolean initialized = variables != null &&
                databaseManager != null &&
                playerManager != null &&
                blocksManager != null &&
                blockItemsManager != null &&
                spawnManager != null &&
                armorstandManager != null &&
                deathMessagesManager != null &&
                transaction != null;

        if (!initialized && Bridges.get() != null) {
            Bridges.get().getLogger().warning(String.format(
                    "[SERVICES] ServicesManager not fully initialized | User: %s | Time: %s",
                    CURRENT_USER,
                    CURRENT_TIME.format(DATE_FORMATTER)
            ));
        }

        return initialized;
    }
}