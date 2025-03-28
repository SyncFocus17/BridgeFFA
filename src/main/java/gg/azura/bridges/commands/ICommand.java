package gg.azura.bridges.commands;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gg.azura.bridges.Bridges;
import gg.azura.bridges.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;


public abstract class ICommand {

    public Bridges plugin = Bridges.get();
    String name;

    Set<String> aliases;

    String permission;

    String args;

    String description;

    public ICommand(String name) {
        this.name = name;
        this.aliases = new HashSet<>();
        this.permission = null;
    }

    public ICommand(String name, String permission, String... aliases) {
        this.name = name;
        this.aliases = new HashSet<>(Arrays.asList(aliases));
        this.permission = permission;
    }

    public abstract boolean hasPermission(CommandSender paramCommandSender);

    public String getName() {
        return this.name;
    }

    public Set<String> getAliases() {
        return this.aliases;
    }

    public String getPermission() {
        return this.permission;
    }

    public abstract String getArgs();

    public abstract String getDescription();

    public abstract void execute(String paramString, CommandSender paramCommandSender, String[] paramArrayOfString);

    public abstract List<String> tabComplete(CommandSender paramCommandSender, Command paramCommand, String paramString, String[] paramArrayOfString);

    public void noPermission(CommandSender sender) {
        sender.sendMessage(CC.t("&cYou do not have the right permissions to access this command!"));
    }

}