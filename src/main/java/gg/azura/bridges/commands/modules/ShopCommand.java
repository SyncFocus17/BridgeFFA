package gg.azura.bridges.commands.modules;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.gui.ShopMenu;
import gg.azura.bridges.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ShopCommand extends gg.azura.bridges.commands.ICommand {

    public ShopCommand() {
        super("shop", "bridgeffa.shop", new String[] { "buy" });
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    public String getArgs() {
        return "";
    }

    public String getDescription() {
        return "Open the Block Shop";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.t("&cOnly players can access this command!"));
            return;
        }
        BridgePlayer player = this.plugin.getSM().getPlayerManager().getPlayer((Player)sender);
        ShopMenu menu = new ShopMenu(player);
        menu.open();
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
