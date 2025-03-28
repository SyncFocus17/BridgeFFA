package gg.azura.bridges.events;

import gg.azura.bridges.BridgeBlock;
import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

public class BridgeBlockBrokenEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    private final BridgeBlock block;

    private final BridgePlayer breaker;

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public BridgeBlockBrokenEvent(@NotNull BridgeBlock block, BridgePlayer breaker) {
        super(block.getBlock());
        this.block = block;
        this.breaker = breaker;
    }

    public BridgeBlockBrokenEvent(@NotNull BridgeBlock block, Player player) {
        super(block.getBlock());
        this.block = block;
        this.breaker = Bridges.get().getSM().getPlayerManager().getPlayer(player);
    }

    public BridgeBlock getBridgeBlock() {
        return this.block;
    }

    public BridgePlayer getWhoBroke() {
        return this.breaker;

    }
}
