package gg.azura.bridges.ffa.listeners;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class ProtectionListener implements Listener {

    private final Bridges plugin;

    public ProtectionListener(Bridges plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItem(PlayerItemDamageEvent event) {
        if (!(this.plugin.getSM().getVariables()).worlds.contains(event.getPlayer().getWorld()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (!event.getCause().equals(EntityDamageEvent.DamageCause.FALL))
            return;
        if ((this.plugin.getSM().getVariables()).disableFallDamage)
            event.setDamage(0.0D);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(this.plugin.getSM().getVariables()).worlds.contains(event.getEntity().getWorld()))
            return;
        event.setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(PlayerPickupItemEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (!(this.plugin.getSM().getVariables()).worlds.contains(event.getEntity().getWorld()))
            return;
        BridgePlayer user = this.plugin.getSM().getPlayerManager().getPlayer((Player)event.getEntity());
        if (user == null)
            return;
        if (user.isInSpawn())
            event.setCancelled(true);
    }

}
