package gg.azura.bridges.commands.modules;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.commands.ICommand;
import gg.azura.bridges.utils.CC;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SetcoinsCommand extends ICommand {

    public SetcoinsCommand() {
        super("setcoins", "bridgeffa.setcoins", new String[0]);
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    public String getArgs() {
        return "<player> <coins>";
    }

    public String getDescription() {
        return "Set someone's coins";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        int coins;
        if (args.length < 3) {
            sender.sendMessage(CC.tf("&c/%s %s %s", new Object[] { mainCommand, getName(), getArgs() }));
            return;
        }
        BridgePlayer target = this.plugin.getSM().getPlayerManager().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(CC.t("&cPlayer not found, please try again later!"));
            return;
        }
        try {
            coins = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(CC.t("&cInvalid coins number, please try again later!"));
            return;
        }
        target.setCoins(coins);
        sender.sendMessage(CC.t("&aTarget coins changed!"));
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2)
            return (List<String>)this.plugin.getServer().getOnlinePlayers()
                    .stream()
                    .filter(p -> ((Player)sender).canSee(p))
                    .map(OfflinePlayer::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        return Collections.emptyList();
    }
}
