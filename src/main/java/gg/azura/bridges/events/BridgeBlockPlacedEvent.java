package gg.azura.bridges.events;

import gg.azura.bridges.BridgeBlock;
import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

public class BridgeBlockPlacedEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    private final BridgeBlock block;

    private final BridgePlayer placer;

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public BridgeBlockPlacedEvent(@NotNull BridgeBlock block, BridgePlayer placer) {
        super(block.getBlock());
        this.block = block;
        this.placer = placer;
    }

    public BridgeBlockPlacedEvent(@NotNull BridgeBlock block, Player player) {
        super(block.getBlock());
        this.block = block;
        this.placer = Bridges.get().getSM().getPlayerManager().getPlayer(player);
    }

    public BridgeBlock getBridgeBlock() {
        return this.block;
    }

    public BridgePlayer getWhoPlaced() {
        return this.placer;
    }
}
