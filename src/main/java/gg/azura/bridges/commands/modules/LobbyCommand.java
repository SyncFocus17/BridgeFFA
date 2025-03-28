package gg.azura.bridges.commands.modules;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.utils.CC;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LobbyCommand extends gg.azura.bridges.commands.ICommand {

    public LobbyCommand() {
        super("lobby", "bridgeffa.lobby", new String[0]);
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
        return "Go to the lobby";
    }

    public void execute(String mainCommand, CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!sender.hasPermission("ffa.lobby.others")) {
                noPermission(sender);
                return;
            }
            String targetName = args[1];
            if (this.plugin.getServer().getPlayer(targetName) == null) {
                sender.sendMessage(CC.t("&cTarget is currently not online, please try again later!"));
                return;
            }
            try {
                this.plugin.getServer().getPlayer(targetName).teleport((this.plugin.getSM().getVariables()).lobby);
            } catch (NullPointerException ex) {
                ex.printStackTrace();
                sender.sendMessage(CC.t("&cUnable to teleport - Lobby not set!"));
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(CC.t("&cOnly players can access this command!"));
                return;
            }
            Player player = (Player)sender;
            BridgePlayer user = this.plugin.getSM().getPlayerManager().getPlayer(player);
            if (user.isInFFAWorld() && !user.isInSpawn() &&
                    !player.getGameMode().equals(GameMode.CREATIVE) && !player.getGameMode().equals(GameMode.SPECTATOR)) {
                sender.sendMessage(CC.t("&cYou cannot do that here!"));
                return;
            }
            try {
                player.teleport((this.plugin.getSM().getVariables()).lobby);
            } catch (NullPointerException ex) {
                ex.printStackTrace();
                player.sendMessage(CC.t("&cUnable to teleport - Lobby not set!"));
            }
        }
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
