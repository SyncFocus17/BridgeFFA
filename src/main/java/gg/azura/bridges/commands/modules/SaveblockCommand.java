package gg.azura.bridges.commands.modules;

import gg.azura.bridges.commands.ICommand;
import gg.azura.bridges.services.BlockItemsManager;
import gg.azura.bridges.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class SaveblockCommand extends ICommand

{

    public SaveblockCommand() {
        super("saveblock", "bridgeffa.saveblock", new String[] { "blocksave", "addblock", "blockadd" });
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    @NotNull
    public String getArgs() {
        return "<name>";
    }

    @NotNull
    public String getDescription() {
        return "Save a block";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        int price;
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.t("&cOnly players can access this command!"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.t("&c/" + mainCommand + " " + getName() + " " + getArgs()));
            return;
        }
        Player player = (Player)sender;
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType().equals(Material.AIR)) {
            sender.sendMessage(CC.t("&cYou have to have a block in your hand!"));
            return;
        }
        try {
            price = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(CC.t("&cInvalid price, please specify a valid number!"));
            return;
        }
        BlockItemsManager blockItemsManager = this.plugin.getSM().getBlockItemsManager();
        blockItemsManager.saveNewBlockItem(item.getType(), price);
        sender.sendMessage(CC.t("&aBlock saved - you can now play with the saved block!"));
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }




}
