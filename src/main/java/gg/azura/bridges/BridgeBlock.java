package gg.azura.bridges;

import gg.azura.bridges.events.BridgeBlockRemoveEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BridgeBlock {

    private final Bridges plugin = Bridges.get();

    private final Block block;

    private final BridgePlayer owner;

    private BukkitTask removalTask;

    private BukkitTask glassTask;

    public BridgeBlock(Block block, BridgePlayer owner) {
        this.block = block;
        this.owner = owner;
        setRemovalAfter(10);
    }

    public Block getBlock() {
        return this.block;
    }

    public BridgePlayer getOwner() {
        return this.owner;
    }

    public void remove() {
        BridgeBlockRemoveEvent event = new BridgeBlockRemoveEvent(this);
        this.plugin.getServer().getPluginManager().callEvent((Event) event);
        if (event.isCancelled())
            return;
        this.block.setType(Material.AIR);
    }

    public void setRemovalAfter(int seconds) {
        this

                .removalTask = (new BukkitRunnable() {
            public void run() {
                BridgeBlock.this.remove();
            }
        }).runTaskLater((Plugin)this.plugin, seconds * 20L);
        this

                .glassTask = (new BukkitRunnable() {
            public void run() {
                BridgeBlock.this.block.setType(Material.WHITE_STAINED_GLASS);
            }
        }).runTaskLater((Plugin) this.plugin, Math.max(1, seconds - 2) * 20L);
    }

    public void cancelRemoval() {
        if (this.glassTask != null)
            this.glassTask.cancel();
        if (this.removalTask != null)
            this.removalTask.cancel();
    }
}
