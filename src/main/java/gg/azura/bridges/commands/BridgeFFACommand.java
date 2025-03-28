package gg.azura.bridges.commands;

import gg.azura.bridges.Bridges;
import gg.azura.bridges.commands.modules.*;
import gg.azura.bridges.commands.modules.KillCommand;
import gg.azura.bridges.utils.CC;
import lombok.Getter;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Modern implementation of the BridgeFFA command handler
 *
 * @author SyncFocus17
 * @version 2.0
 * @since 2025-02-04 18:09:29
 */
@Getter
public class BridgeFFACommand implements CommandExecutor, TabCompleter {

    private static final String CURRENT_TIME = "2025-02-04 18:09:29";
    private static final String CURRENT_USER = "SyncFocus17";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Bridges plugin;
    private final Set<ICommand> commands;
    private final Map<String, Long> commandUsage;
    private final Map<String, ICommand> commandCache;

    /**
     * Creates a new BridgeFFA command handler
     * @param plugin The plugin instance
     */
    public BridgeFFACommand(@NotNull Bridges plugin) {
        this.plugin = plugin;
        this.commands = Collections.synchronizedSet(new HashSet<>());
        this.commandUsage = new ConcurrentHashMap<>();
        this.commandCache = new ConcurrentHashMap<>();

        // Initialize commands
        initializeCommands();

        // Log initialization
        logInitialization();
    }

    /**
     * Initializes all available commands
     */
    private void initializeCommands() {
        List<ICommand> commandList = Arrays.asList(
                new SetlobbyCommand(),
                new LobbyCommand(),
                new KillCommand(),
                new SaveblockCommand(),
                new BlockCommand(),
                new SavespawnCommand(),
                new SpawnCommand(),
                new ArmorstandCommand(),
                new ShopCommand(),
                new StatsCommand(),
                new SetcoinsCommand(),
                new MessageShopCommand(),
                new InstaspawnCommand()
        );

        commandList.forEach(this::registerCommand);
    }

    /**
     * Registers a command and caches it
     * @param command The command to register
     */
    private void registerCommand(ICommand command) {
        commands.add(command);
        commandCache.put(command.getName().toLowerCase(), command);
        command.getAliases().forEach(alias ->
                commandCache.put(alias.toLowerCase(), command));

        // Log command registration
        plugin.getLogger().info(String.format(
                "[COMMAND] Registered: %s | Aliases: %s | Time: %s",
                command.getName(),
                String.join(", ", command.getAliases()),
                CURRENT_TIME
        ));
    }

    /**
     * Displays help menu to sender
     * @param sender The command sender
     * @param label The command label
     * @return true if help was shown
     */
    private boolean help(CommandSender sender, String label) {
        sender.sendMessage(CC.t("&8&m                                                  "));
        sender.sendMessage(CC.t("&b&l BRIDGEFFA COMMANDS"));
        sender.sendMessage(CC.t("&8&m                                                  "));
        sender.sendMessage("");

        commands.stream()
                .filter(cmd -> sender.hasPermission(cmd.getPermission()))
                .sorted(Comparator.comparing(ICommand::getName))
                .forEach(cmd -> {
                    String usage = String.format("&7▸ &b/%s %s %s",
                            label, cmd.getName(), cmd.getArgs());
                    String desc = String.format("&8- &7%s", cmd.getDescription());
                    sender.sendMessage(CC.t(usage));
                    sender.sendMessage(CC.t(desc));
                    sender.sendMessage("");
                });

        sender.sendMessage(CC.t("&8&m                                                  "));

        // Log help access
        logCommandAccess("help", sender, new String[]{});
        return true;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String[] args
    ) {
        try {
            // Show help if no args
            if (args.length == 0) {
                return help(sender, label);
            }

            // Get command from cache
            String cmdName = args[0].toLowerCase();
            ICommand cmd = commandCache.get(cmdName);

            // Show help if command not found
            if (cmd == null) {
                logCommandAccess("unknown", sender, args);
                return help(sender, label);
            }

            // Check permissions
            if (!cmd.hasPermission(sender)) {
                cmd.noPermission(sender);
                logCommandAccess("no_permission", sender, args);
                return true;
            }

            // Track command usage
            commandUsage.merge(cmdName, 1L, Long::sum);

            // Execute command
            long startTime = System.nanoTime();
            cmd.execute(label, sender, args);
            long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms

            // Log execution
            logCommandExecution(cmd, sender, args, duration);
            return true;

        } catch (Exception e) {
            // Log error
            plugin.getLogger().severe(String.format(
                    "[ERROR] Command execution failed: %s | User: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));

            // Inform sender
            sender.sendMessage(CC.t("&c⚠ An error occurred while executing the command!"));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            String[] args
    ) {
        try {
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                return commands.stream()
                        .filter(cmd -> sender.hasPermission(cmd.getPermission()))
                        .map(ICommand::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .sorted()
                        .collect(Collectors.toList());
            }

            ICommand cmd = commandCache.get(args[0].toLowerCase());
            if (cmd == null) {
                return Collections.emptyList();
            }

            return cmd.tabComplete(sender, command, alias, args);

        } catch (Exception e) {
            plugin.getLogger().warning(String.format(
                    "[TAB] Tab completion failed: %s | User: %s | Time: %s",
                    e.getMessage(),
                    CURRENT_USER,
                    CURRENT_TIME
            ));
            return Collections.emptyList();
        }
    }

    /**
     * Logs command initialization
     */
    private void logInitialization() {
        plugin.getLogger().info(String.format(
                "[INIT] BridgeFFA Commands Initialized | Count: %d | User: %s | Time: %s",
                commands.size(),
                CURRENT_USER,
                CURRENT_TIME
        ));
    }

    /**
     * Logs command execution
     */
    private void logCommandExecution(
            ICommand cmd,
            CommandSender sender,
            String[] args,
            long duration
    ) {
        plugin.getLogger().info(String.format(
                "[EXECUTION] %s executed %s | Args: %s | Duration: %dms | Time: %s",
                sender.getName(),
                cmd.getName(),
                String.join(" ", args),
                duration,
                CURRENT_TIME
        ));
    }

    /**
     * Logs command access
     */
    private void logCommandAccess(
            String status,
            CommandSender sender,
            String[] args
    ) {
        plugin.getLogger().info(String.format(
                "[ACCESS] %s | Status: %s | User: %s | Args: %s | Time: %s",
                sender.getName(),
                status,
                CURRENT_USER,
                String.join(" ", args),
                CURRENT_TIME
        ));
    }
}