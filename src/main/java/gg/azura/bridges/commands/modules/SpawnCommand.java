package gg.azura.bridges.commands.modules;

import gg.azura.bridges.commands.ICommand;
import gg.azura.bridges.ffa.Spawn;
import gg.azura.bridges.ffa.services.SpawnManager;
import gg.azura.bridges.utils.CC;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Modern implementation of the spawn command
 * @author SyncFocus17
 * @since 2025-02-04 18:08:04
 */
public class SpawnCommand extends ICommand {

    private static final String CURRENT_TIME = "2025-02-04 18:08:04";
    private static final String CURRENT_USER = "SyncFocus17";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SpawnCommand() {
        super("spawn", "bridgeffa.spawn", new String[]{"sp", "spawnpoint"});
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    @NotNull
    @Override
    public String getArgs() {
        return "<spawn_name> [player]";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Teleport yourself or another player to a spawn point";
    }

    @Override
    public void execute(String mainCommand, CommandSender sender, String[] args) {
        try {
            // Validate sender
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.t("&c⚠ Only players can use this command!"));
                logCommand("fail", "not_player", sender.getName(), args);
                return;
            }

            // Check arguments
            if (args.length < 2) {
                sendUsageMessage(mainCommand, sender);
                logCommand("fail", "insufficient_args", sender.getName(), args);
                return;
            }

            Player player = (Player) sender;
            String spawnName = args[1].toLowerCase();

            // Get target player
            Player target = args.length >= 3 ?
                    Optional.ofNullable(plugin.getServer().getPlayer(args[2]))
                            .orElse(player) : player;

            // Validate target
            if (target == null || !target.isOnline()) {
                sender.sendMessage(CC.t("&c⚠ Target player not found or offline!"));
                logCommand("fail", "target_not_found", sender.getName(), args);
                return;
            }

            // Check permissions for teleporting others
            if (target != player && !sender.hasPermission("bridgeffa.spawn.others")) {
                sender.sendMessage(CC.t("&c⚠ You don't have permission to teleport other players!"));
                logCommand("fail", "no_permission_others", sender.getName(), args);
                return;
            }

            // Get spawn manager and validate spawn
            SpawnManager spawnManager = plugin.getSM().getSpawnManager();
            if (!spawnManager.exists(spawnName)) {
                sender.sendMessage(CC.t("&c⚠ Spawn point '&e" + spawnName + "&c' does not exist!"));
                logCommand("fail", "spawn_not_found", sender.getName(), args);
                return;
            }

            // Get spawn and teleport
            Spawn spawn = spawnManager.getSpawn(spawnName);
            Location previousLocation = target.getLocation();

            // Perform teleport
            spawn.teleport(target);

            // Play effects
            playTeleportEffects(target);

            // Send success messages
            sendTeleportMessages(sender, target, spawnName);

            // Log success
            logTeleport(sender, target, spawnName, previousLocation, spawn.getLocation());

        } catch (Exception e) {
            // Log error
            plugin.getLogger().severe(String.format(
                    "[ERROR] Spawn command failed: %s | User: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
            sender.sendMessage(CC.t("&c⚠ An error occurred while executing the command!"));
        }
    }

    private void sendUsageMessage(String mainCommand, CommandSender sender) {
        sender.sendMessage(CC.t("&8&m                                                  "));
        sender.sendMessage(CC.t("&b&l SPAWN COMMAND USAGE"));
        sender.sendMessage(CC.t("&8&m                                                  "));
        sender.sendMessage("");
        sender.sendMessage(CC.t("&7▸ &b/" + mainCommand + " spawn <name>"));
        if (sender.hasPermission("bridgeffa.spawn.others")) {
            sender.sendMessage(CC.t("&7▸ &b/" + mainCommand + " spawn <name> <player>"));
        }
        sender.sendMessage("");
        sender.sendMessage(CC.t("&7Available spawns: &b" +
                String.join("&7, &b", plugin.getSM().getSpawnManager().getSpawns().toString())));
        sender.sendMessage(CC.t("&8&m                                                  "));
    }

    private void playTeleportEffects(Player target) {
        target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        target.spawnParticle(org.bukkit.Particle.PORTAL, target.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);
    }

    private void sendTeleportMessages(CommandSender sender, Player target, String spawnName) {
        if (sender == target) {
            sender.sendMessage(CC.t("&a✓ Teleported to spawn point '&e" + spawnName + "&a'!"));
        } else {
            sender.sendMessage(CC.t("&a✓ Teleported &e" + target.getName() + " &ato spawn point '&e" + spawnName + "&a'!"));
            target.sendMessage(CC.t("&a✓ You were teleported to spawn point '&e" + spawnName + "&a'!"));
        }
    }

    private void logCommand(String status, String reason, String executor, String[] args) {
        plugin.getLogger().info(String.format(
                "[COMMAND] %s | Status: %s | Reason: %s | Executor: %s | Args: %s | Time: %s",
                "spawn",
                status,
                reason,
                executor,
                String.join(" ", args),
                CURRENT_TIME
        ));
    }

    private void logTeleport(CommandSender sender, Player target, String spawnName,
                             Location from, Location to) {
        plugin.getLogger().info(String.format(
                "[TELEPORT] %s teleported %s to spawn '%s' | From: %s | To: %s | Time: %s",
                sender.getName(),
                target.getName(),
                spawnName,
                formatLocation(from),
                formatLocation(to),
                CURRENT_TIME
        ));
    }

    private String formatLocation(Location loc) {
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            return plugin.getSM().getSpawnManager().getSpawns().stream()
                    .map(Spawn::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && sender.hasPermission("bridgeffa.spawn.others")) {
            String partial = args[2].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}