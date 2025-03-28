package gg.azura.bridges.utils;


import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PAPIExpansion extends PlaceholderExpansion {
    private final Bridges plugin;

    public PAPIExpansion(Bridges plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public String getIdentifier() {
        return "bridgeffa";
    }

    @NotNull
    public String getAuthor() {
        return "Pafias";
    }

    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null)
            return "";
        BridgePlayer p = this.plugin.getSM().getPlayerManager().getPlayer(player);
        if (p == null)
            return "";
        switch (params) {
            case "name":
                return p.getName();
            case "coins":
                return String.valueOf(p.getCoins());
            case "block":
                return (p.getSelectedBlockItem() == null) ? "None" : p.getSelectedBlockItem().getItem().getItemMeta().getItemName();
        }
        return null;
    }

}
