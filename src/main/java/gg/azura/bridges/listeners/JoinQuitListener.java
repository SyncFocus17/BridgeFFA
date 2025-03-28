package gg.azura.bridges.listeners;

import gg.azura.bridges.Bridges;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class JoinQuitListener implements Listener {

    private final Bridges plugin;

    public JoinQuitListener(Bridges plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSpawn(PlayerSpawnLocationEvent event) {
        if ((this.plugin.getSM().getVariables()).lobby != null)
            event.setSpawnLocation((this.plugin.getSM().getVariables()).lobby);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.plugin.getSM().getPlayerManager().addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.getSM().getPlayerManager().removePlayer(event.getPlayer());
    }
}
