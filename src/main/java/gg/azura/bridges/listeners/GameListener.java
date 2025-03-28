package gg.azura.bridges.listeners;

import gg.azura.bridges.*;
import gg.azura.bridges.events.BridgeBlockBrokenEvent;
import gg.azura.bridges.events.BridgeBlockPlacedEvent;
import org.bukkit.GameMode;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class GameListener implements Listener {

    private final Bridges plugin;

    public GameListener(Bridges plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void captureHealth(PlayerDeathEvent event) {
        if (event.getEntity().getKiller() == null)
            return;
        BridgePlayer player = this.plugin.getSM().getPlayerManager().getPlayer(event.getEntity());
        if (player == null)
            return;
        if (player.getSelectedDeathMessage() == null)
            return;
        DeathMessage dm = player.getSelectedDeathMessage();
        if (dm == null)
            return;
        dm.setHealth(event.getEntity().getKiller().getHealth());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (event.isAsynchronous() || event.getEntity().getKiller() == null)
            return;
        BridgePlayer player = this.plugin.getSM().getPlayerManager().getPlayer(event.getEntity());
        if (player == null)
            return;
        DeathMessage dm = player.getSelectedDeathMessage();
        if (dm == null)
            return;
        dm.setVictim(event.getEntity().getName());
        if (event.getEntity().getKiller() != null)
            dm.setKiller(event.getEntity().getKiller().getName());
        event.setDeathMessage(dm.getDeathMessage());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.getPlayer().getGameMode().equals(GameMode.SURVIVAL) && !event.getPlayer().getGameMode().equals(GameMode.ADVENTURE))
            return;
        if (event.getFrom().getY() >= (this.plugin.getSM().getVariables()).activateY && event.getTo().getY() < (this.plugin.getSM().getVariables()).activateY) {
            BridgePlayer user = this.plugin.getSM().getPlayerManager().getPlayer(event.getPlayer());
            if (user == null)
                return;
            if (!user.isInFFAWorld())
                return;
            BlockItem blockItem = this.plugin.getSM().getPlayerManager().getPlayer(event.getPlayer()).getSelectedBlockItem();
            if (blockItem == null)
                return;
            blockItem.give(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled())
            return;
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE))
            return;
        if (!(this.plugin.getSM().getVariables()).worlds.contains(event.getBlock().getWorld()))
            return;
        BridgePlayer player = this.plugin.getSM().getPlayerManager().getPlayer(event.getPlayer());
        if (player.isInSpawn())
            event.setCancelled(true);
        if (!event.isCancelled()) {
            BridgeBlock bb = this.plugin.getSM().getBlocksManager().placedBlock(event.getBlock(), event.getPlayer());
            this.plugin.getServer().getPluginManager().callEvent((Event)new BridgeBlockPlacedEvent(bb, event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE))
            return;
        if (!(this.plugin.getSM().getVariables()).worlds.contains(event.getBlock().getWorld()))
            return;
        BridgeBlock bb = this.plugin.getSM().getBlocksManager().getBlock(event.getBlock());
        if (bb == null) {
            event.setCancelled(true);
            return;
        }
        BridgePlayer player = this.plugin.getSM().getPlayerManager().getPlayer(event.getPlayer());
        if (player.isInSpawn())
            event.setCancelled(true);
        if (!event.isCancelled()) {
            this.plugin.getSM().getBlocksManager().brokeBlock(event.getBlock(), event.getPlayer());
            this.plugin.getServer().getPluginManager().callEvent((Event) new BridgeBlockBrokenEvent(bb, event.getPlayer()));
        }
    }
}
