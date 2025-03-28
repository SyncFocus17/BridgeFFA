package gg.azura.bridges.commands.modules;

import gg.azura.bridges.commands.ICommand;
import gg.azura.bridges.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.List;

public class ArmorstandCommand extends ICommand {

    public ArmorstandCommand() {
        super("armorstand", "bridgeffa.armorstand", new String[] { "as" });
    }

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }

    public String getArgs() {
        return "<name>";
    }

    public String getDescription() {
        return "Get kit or spawn armorstand";
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
        String name = CC.t(args[1]);
        Player player = (Player)sender;
        ArmorStand armorstand = null;
        if (player.getWorld().getEntities().stream().anyMatch(e -> (e instanceof ArmorStand && e.getLocation().getBlock() == player.getLocation().getBlock())))
            armorstand = (ArmorStand) player.getWorld().getEntities().stream().filter(e -> (e instanceof ArmorStand && e.getLocation().getBlock() == player.getLocation().getBlock())).findAny().orElse(null);
        if (armorstand == null) {
            armorstand = (ArmorStand) player.getWorld().spawn(player.getLocation(), ArmorStand.class);
            prepAs(armorstand, name);
        } else {
            prepAs(armorstand, name);
        }
    }

    public List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    private void prepAs(ArmorStand as, String name) {
        as.setBasePlate(false);
        as.setArms(true);
        as.setCustomNameVisible(true);
        as.setCustomName(name);
    }
}
