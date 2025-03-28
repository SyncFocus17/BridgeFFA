package gg.azura.bridges.commands.modules;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.commands.ICommand;
import gg.azura.bridges.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modern implementation of the stats command with async data loading
 * @author SyncFocus17
 * @since 2025-02-04 18:14:37
 */
public class StatsCommand extends ICommand {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CURRENT_TIME = "2025-02-04 18:14:37";
    private static final String CURRENT_USER = "SyncFocus17";

    // Cache for player stats to prevent frequent database queries
    private final Map<UUID, CachedStats> statsCache = new ConcurrentHashMap<>();

    private record CachedStats(
            double kills,
            int deaths,
            double coins,
            double earnedCoins,
            double spentCoins,
            long timestamp
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 300000; // 5 minutes
        }
    }

    public StatsCommand() {
        super("stats", "bridgeffa.stats", new String[]{"statistics"});
    }

    @Override
    public boolean hasPermission(CommandSender paramCommandSender) {
        return true;
    }

    @Override
    public String getArgs() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Stats!";
    }

    @Override
    public void execute(String mainCommand, CommandSender sender, String[] args) {
        try {
            // Validate sender
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.t("&c⚠ Only players can use this command!"));
                return;
            }

            // Get target player
            Player targetPlayer = args.length > 1 ?
                    plugin.getServer().getPlayer(args[1]) :
                    (Player) sender;

            if (targetPlayer == null) {
                sender.sendMessage(CC.t("&c⚠ Player not found! Please try again."));
                logCommand("fail", "player_not_found", sender.getName(), args);
                return;
            }

            // Load player stats asynchronously
            loadPlayerStats(sender, targetPlayer);

        } catch (Exception e) {
            handleError(sender, e);
        }
    }

    private void loadPlayerStats(CommandSender sender, Player targetPlayer) {
        CompletableFuture.runAsync(() -> {
            try {
                // Check cache first
                CachedStats cached = statsCache.get(targetPlayer.getUniqueId());
                if (cached != null && !cached.isExpired()) {
                    sendStatistics(sender, targetPlayer, cached);
                    return;
                }

                // Get bridge player data
                BridgePlayer bridgePlayer = plugin.getServicesManager()
                        .getPlayerManager()
                        .getBridgePlayer(targetPlayer.getUniqueId());

                if (bridgePlayer == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(CC.t("&c⚠ Player data not available!"));
                        logCommand("fail", "no_player_data", sender.getName(),
                                new String[]{targetPlayer.getName()});
                    });
                    return;
                }

                // Create new cached stats
                CachedStats stats = new CachedStats(
                        bridgePlayer.getKills(),
                        bridgePlayer.getDeaths(),
                        bridgePlayer.getCoins(),
                        bridgePlayer.getEarnedCoins(),
                        bridgePlayer.getSpentCoins(),
                        System.currentTimeMillis()
                );

                // Update cache
                statsCache.put(targetPlayer.getUniqueId(), stats);

                // Send stats on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sendStatistics(sender, targetPlayer, stats);
                    logCommand("success", "stats_shown", sender.getName(),
                            new String[]{targetPlayer.getName()});
                });

            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        handleError(sender, e));
            }
        });
    }

    private void sendStatistics(CommandSender sender, Player target, CachedStats stats) {
        NumberFormat formatter = NumberFormat.getInstance();

        // Get bridge player for additional data
        BridgePlayer bridgePlayer = plugin.getServicesManager()
                .getPlayerManager()
                .getBridgePlayer(target.getUniqueId());

        // Get selected items safely
        String selectedBlock = Optional.ofNullable(bridgePlayer)
                .map(BridgePlayer::getSelectedBlockItem)
                .map(block -> block.getItem().getType().name())
                .orElse("None");

        String selectedMessage = Optional.ofNullable(bridgePlayer)
                .map(BridgePlayer::getSelectedDeathMessage)
                .map(msg -> "#" + msg.getID())
                .orElse("None");

        // Create and send statistics message
        List<String> statsMessage = createStatsMessage(
                target.getName(),
                stats,
                formatter,
                selectedBlock,
                selectedMessage
        );

        statsMessage.forEach(sender::sendMessage);

        // Play sound if sender is a player
        if (sender instanceof Player) {
            ((Player) sender).playSound(
                    ((Player) sender).getLocation(),
                    org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    1.0f,
                    1.0f
            );
        }
    }

    private List<String> createStatsMessage(
            String playerName,
            CachedStats stats,
            NumberFormat formatter,
            String selectedBlock,
            String selectedMessage
    ) {
        List<String> message = new ArrayList<>();
        message.add(CC.t("&8&m                                                  "));
        message.add(CC.tf("&b&l STATISTICS FOR %s", playerName));
        message.add(CC.t("&8&m                                                  "));
        message.add("");
        message.add(CC.tf("&7▸ &bCoins: &f%s", formatter.format(stats.coins())));
        message.add(CC.tf("&7▸ &bKills: &f%s", formatter.format(stats.kills())));
        message.add(CC.tf("&7▸ &bDeaths: &f%s", formatter.format(stats.deaths())));
        message.add(CC.tf("&7▸ &bKDR: &f%.2f", calculateKDR(stats)));
        message.add("");
        message.add(CC.t("&b&l LOADOUT"));
        message.add(CC.tf("&7▸ &bSelected Block: &f%s", selectedBlock));
        message.add(CC.tf("&7▸ &bSelected Message: &f%s", selectedMessage));
        message.add("");
        message.add(CC.t("&b&l ECONOMY"));
        message.add(CC.tf("&7▸ &bEarned Coins: &f%s", formatter.format(stats.earnedCoins())));
        message.add(CC.tf("&7▸ &bSpent Coins: &f%s", formatter.format(stats.spentCoins())));
        message.add("");
        message.add(CC.t("&8&m                                                  "));
        return message;
    }

    private double calculateKDR(CachedStats stats) {
        return stats.deaths() == 0 ? stats.kills() : stats.kills() / stats.deaths();
    }

    private void handleError(CommandSender sender, Exception e) {
        plugin.getLogger().severe(String.format(
                "[ERROR] Stats command failed: %s | User: %s | Time: %s",
                e.getMessage(),
                CURRENT_USER,
                CURRENT_TIME
        ));
        sender.sendMessage(CC.t("&c⚠ An error occurred while fetching statistics!"));
    }

    private void logCommand(String status, String reason, String executor, String[] args) {
        plugin.getLogger().info(String.format(
                "[COMMAND] %s | Status: %s | Reason: %s | Executor: %s | Args: %s | Time: %s",
                "stats",
                status,
                reason,
                executor,
                String.join(" ", args),
                CURRENT_TIME
        ));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command,
                                    String alias, String[] args) {
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .toList();
        }
        return Collections.emptyList();
    }
}