package gg.azura.bridges.ffa.listeners;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import gg.azura.bridges.ffa.Spawn;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MiscListener implements Listener {

    private final Bridges plugin;

    public MiscListener(Bridges plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuickRespawn(PlayerInteractEvent event) {
        Spawn spawn;
        if (!(this.plugin.getSM().getVariables()).quickRespawn)
            return;
        if (!event.getAction().name().contains("CLICK"))
            return;
        if (!event.hasItem() || !event.getItem().getType().equals(Material.CLOCK))
            return;
        ItemStack item = event.getItem();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName() || !item.getItemMeta().getDisplayName().toLowerCase().contains("respawn"))
            return;
        BridgePlayer user = this.plugin.getSM().getPlayerManager().getPlayer(event.getPlayer());
        if (event.getAction().name().contains("RIGHT")) {
            spawn = this.plugin.getSM().getSpawnManager().getDefaultSpawn();
        } else if (event.getAction().name().contains("LEFT")) {
            spawn = user.getLastSpawn();
        } else {
            return;
        }
        if (spawn == null)
            return;
        user.getSelectedBlockItem().give(user.getPlayer());
        spawn.teleport(user.getPlayer());
    }
}
