package gg.azura.bridges.events;

import gg.azura.bridges.BridgeBlock;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

public class BridgeBlockRemoveEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final BridgeBlock block;

    private boolean cancelled;

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public BridgeBlockRemoveEvent(@NotNull BridgeBlock block) {
        super(block.getBlock());
        this.block = block;
    }

    public BridgeBlock getBridgeBlock() {
        return this.block;
    }

    public boolean isCancelled() {
        return false;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
