package gg.azura.bridges.commands.modules;

import gg.azura.bridges.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SetlobbyCommand extends gg.azura.bridges.commands.ICommand {

    public SetlobbyCommand() {
        super("setlobby", "bridgeffa.setlobby", new String[0]);
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
        return "Set the lobby (spawn)location";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.t("&cOnly players can access this command!"));
            return;
        }
        Player player = (Player)sender;
        (this.plugin.getSM().getVariables()).lobby = player.getLocation();
        this.plugin.getConfig().set("lobby.world", player.getLocation().getWorld().getName());
        this.plugin.getConfig().set("lobby.x", Double.valueOf(player.getLocation().getBlockX() + 0.5D));
        this.plugin.getConfig().set("lobby.y", Double.valueOf(player.getLocation().getBlockY() + 0.1D));
        this.plugin.getConfig().set("lobby.z", Double.valueOf(player.getLocation().getBlockZ() + 0.5D));
        this.plugin.getConfig().set("lobby.yaw", Float.valueOf(player.getLocation().getYaw()));
        this.plugin.getConfig().set("lobby.pitch", Float.valueOf(player.getLocation().getPitch()));
        this.plugin.saveConfig();
        sender.sendMessage(CC.t("&aLobby set - game is playable as of right now!"));
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
