package gg.azura.bridges.commands.modules;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.commands.ICommand;
import java.util.List;

import gg.azura.bridges.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KillCommand extends ICommand {

    public KillCommand() {
        super("kill", "bridgeffa.kill", new String[] { "suicide", "die" });
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    @NotNull
    public String getArgs() {
        return "";
    }

    @NotNull
    public String getDescription() {
        return "Kill yourself!";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.t("&cOnly players can access this command!"));
                return;
            }
            BridgePlayer user = this.plugin.getSM().getPlayerManager().getPlayer((Player)sender);
            if (user.isInSpawn())
                return;
            user.getPlayer().setHealth(0.0D);
        } else if (args.length > 1) {
            if (this.plugin.getServer().getPluginManager().isPluginEnabled("Essentials")) {
                if (sender instanceof Player) {
                    ((Player)sender).performCommand("essentials:kill " + args[1]);
                } else {
                    this.plugin.getServer().dispatchCommand((CommandSender)this.plugin.getServer().getConsoleSender(), "essentials:kill " + args[1]);
                }
            } else if (sender instanceof Player) {
                ((Player)sender).performCommand("minecraft:kill " + args[1]);
            } else {
                this.plugin.getServer().dispatchCommand((CommandSender)this.plugin.getServer().getConsoleSender(), "minecraft:kill " + args[1]);
            }
        }
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
