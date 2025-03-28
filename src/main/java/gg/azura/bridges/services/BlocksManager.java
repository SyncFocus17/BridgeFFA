package gg.azura.bridges.services;

import gg.azura.bridges.BridgeBlock;
import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class BlocksManager {

    private final Bridges plugin;

    private final Set<BridgeBlock> blocks;

    public BlocksManager(Bridges plugin) {
        this.plugin = plugin;
        this.blocks = new HashSet<>();
    }

    public BridgeBlock placedBlock(Block block, Player player) {
        BridgeBlock bb = new BridgeBlock(block, this.plugin.getSM().getPlayerManager().getPlayer(player));
        this.blocks.add(bb);
        return bb;
    }

    public BridgeBlock brokeBlock(Block block, Player player) {
        BridgeBlock bb = getBlock(block);
        bb.cancelRemoval();
        this.blocks.remove(bb);
        return bb;
    }

    public Set<BridgeBlock> getBlocks() {
        return this.blocks;
    }

    public BridgeBlock getBlock(Block block) {
        return this.blocks.stream().filter(b -> b.getBlock().equals(block)).findAny().orElse(null);
    }

    public BridgeBlock getBlock(Player player) {
        return getBlock(this.plugin.getSM().getPlayerManager().getPlayer(player));
    }

    public BridgeBlock getBlock(BridgePlayer player) {
        return this.blocks.stream().filter(b -> b.getOwner().equals(player)).findAny().orElse(null);
    }
}
