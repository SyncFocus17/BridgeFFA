package gg.azura.bridges.commands.modules;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.commands.ICommand;
import gg.azura.bridges.gui.MessagesShopMenu;
import gg.azura.bridges.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageShopCommand extends ICommand {

    public MessageShopCommand() {
        super("messageshop", "bridgeffa.messageshop", new String[0]);
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    public String getArgs() {
        return "";
    }

    public String getDescription() {
        return "Open the Death Messages Shop!";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.t("&cOnly players can access this command!"));
            return;
        }
        BridgePlayer player = this.plugin.getSM().getPlayerManager().getPlayer((Player)sender);
        MessagesShopMenu menu = new MessagesShopMenu(player);
        menu.open();
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
