package gg.azura.bridges.commands.modules;

import gg.azura.bridges.commands.ICommand;
import gg.azura.bridges.ffa.services.SpawnManager;
import gg.azura.bridges.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class SavespawnCommand extends ICommand {
    public SavespawnCommand() {
        super("savespawn", "bridgeffa.savespawn", new String[] { "spawnsave", "addspawn", "spawnadd" });
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
        return "Save a spawnpoint";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.t("&cOnly players can access this command!"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(CC.t("&c/" + mainCommand + " " + getName() + " " + getArgs()));
            return;
        }
        Player player = (Player)sender;
        String name = args[1];
        SpawnManager spawnManager = this.plugin.getSM().getSpawnManager();
        if (spawnManager.exists(name)) {
            sender.sendMessage(CC.t("&cA spawn with that name already exists!"));
            return;
        }
        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) {
            sender.sendMessage(CC.t("&cYou have to have an item in your hand (will be the gui item - DEV/BETA - 1.0)"));
            return;
        }
        try {
            spawnManager.saveNewSpawn(player, name);
        } catch (IOException e) {
            e.printStackTrace();
            sender.sendMessage(CC.t("&cFailed to save spawn, please try again!"));
            return;
        }
        sender.sendMessage(CC.t("&aSpawn saved!"));
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

}
