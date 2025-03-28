package gg.azura.bridges.commands.modules;

import gg.azura.bridges.BlockItem;
import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.commands.ICommand;
import java.util.List;
import java.util.Random;

import gg.azura.bridges.ffa.Spawn;
import gg.azura.bridges.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InstaspawnCommand extends ICommand
{
    public InstaspawnCommand() {
        super("instaspawn", "bridgeffa.instaspawn", new String[0]);
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    public String getArgs() {
        return "[player]";
    }

    public String getDescription() {
        return "Instantly spawn (with your kit)";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        BridgePlayer player = null;
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.t("&cOnly players. &oYou can instaspawn a player - by adding their name to the command."));
                return;
            }
            player = this.plugin.getSM().getPlayerManager().getPlayer((Player)sender);
        } else if (args.length >= 2 && sender.hasPermission("bridgeffa.instaspawn.others")) {
            player = this.plugin.getSM().getPlayerManager().getPlayer(args[1]);
        }
        if (player == null)
            return;
        if (!player.isInSpawn())
            return;
        Spawn spawn = this.plugin.getSM().getSpawnManager().getSpawns().get((new Random()).nextInt(this.plugin.getSM().getSpawnManager().getSpawns().size()));
        BlockItem blockItem = player.getSelectedBlockItem();
        spawn.teleport(player.getPlayer());
        if (blockItem != null)
            blockItem.give(player.getPlayer());
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

}
