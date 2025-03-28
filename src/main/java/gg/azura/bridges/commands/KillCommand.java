package gg.azura.bridges.commands;

import gg.azura.bridges.Bridges;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KillCommand implements CommandExecutor {

    private final Bridges plugin;

    public KillCommand(Bridges plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            ((Player)sender).performCommand("ffa kill " + ((args.length > 0) ? args[0] : ""));
        } else {
            this.plugin.getServer().dispatchCommand((CommandSender)this.plugin.getServer().getConsoleSender(), "ffa kill " + ((args.length > 0) ? args[0] : ""));
        }
        return true;
    }
}
