package gg.azura.bridges.utils;

import org.bukkit.ChatColor;

public class CC {

    public static String t(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String tf(String s, Object... o) {
        return t(String.format(s, o));
    }

}
