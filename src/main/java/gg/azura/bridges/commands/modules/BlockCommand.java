package gg.azura.bridges.commands.modules;

import gg.azura.bridges.BlockItem;
import gg.azura.bridges.commands.ICommand;
import gg.azura.bridges.services.BlockItemsManager;
import gg.azura.bridges.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlockCommand extends ICommand {

    public BlockCommand() {
        super("block", "bridgeffa.block", new String[0]);
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
        return "Get a block kit";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.t("&cOnly players can access this command!"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.t("&c/" + mainCommand + " " + getName() + " " + getArgs() + (sender.hasPermission("bridgeffa.block.others") ? " [player]" : "")));
            return;
        }
        Player player = (Player)sender;
        Material material = Material.getMaterial(args[1].toUpperCase());
        if (material == null || material.equals(Material.AIR)) {
            sender.sendMessage(CC.t("&cInvalid material, please specify a valid material!"));
            return;
        }
        Player target = player;
        if (args.length >= 3)
            target = this.plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(CC.t("&cPlayer not found."));
            return;
        }
        BlockItemsManager blockItemsManager = this.plugin.getSM().getBlockItemsManager();
        if (!blockItemsManager.exists(material)) {
            sender.sendMessage(CC.t("&cInvalid block, please specify a valid block!"));
            return;
        }
        BlockItem blockitem = blockItemsManager.getBlockItem(material);
        blockitem.give(target);
        sender.sendMessage(CC.t("&aBlocks given" + ((sender != target) ? (" to " + target.getName()) : "")));
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2)
            return (List<String>)this.plugin.getSM().getBlockItemsManager().getBlockItems()
                    .stream()
                    .map(blockItem -> blockItem.getItem().getType().name())
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        return Collections.emptyList();
    }
}
