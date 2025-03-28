package gg.azura.bridges.ffa.services;

import java.util.Random;

import gg.azura.bridges.BlockItem;
import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import gg.azura.bridges.ffa.Spawn;
import gg.azura.bridges.gui.BlocksMenu;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArmorstandManager {

    private final Bridges plugin;

    public ArmorstandManager(Bridges plugin) {
        this.plugin = plugin;
    }

    public void trigger(ArmorStand as, Player player, boolean leftclick) throws NullPointerException {
        BridgePlayer p = this.plugin.getSM().getPlayerManager().getPlayer(player);
        if (as.isCustomNameVisible() && as.getCustomName() != null && this.plugin.getSM().getSpawnManager().exists(as.getCustomName())) {
            Spawn spawn = this.plugin.getSM().getSpawnManager().getSpawn(as.getCustomName());
            if (!leftclick) {
                BlocksMenu menu = new BlocksMenu(p, p.getUnlockedBlocks().size());
                menu.open((item, slot) -> {
                    if (item == null)
                        return;
                    BlockItem blockitem = this.plugin.getSM().getBlockItemsManager().getBlockItem(item.getType());
                    if (blockitem == null) {
                        menu.setCloseOnClick(false);
                        return;
                    }
                    blockitem.give(player);
                    if (spawn != null)
                        spawn.teleport(player);
                    p.setLastSpawn(spawn);
                });
            } else {
                BlockItem blockItem = this.plugin.getSM().getBlockItemsManager().getBlockItem(player);
                if (blockItem != null)
                    blockItem.give(player);
                spawn.teleport(player);
            }
            p.setLastSpawn(spawn);
        } else {
            Spawn spawn = this.plugin.getSM().getSpawnManager().getSpawns().get((new Random()).nextInt(this.plugin.getSM().getSpawnManager().getSpawns().size()));
            if (!leftclick) {
                BlocksMenu menu = new BlocksMenu(p, p.getUnlockedBlocks().size());
                menu.open((item, slot) -> {
                    if (item == null)
                        return;
                    BlockItem blockitem = this.plugin.getSM().getBlockItemsManager().getBlockItem(item.getType());
                    if (blockitem == null) {
                        menu.setCloseOnClick(false);
                        return;
                    }
                    blockitem.give(player);
                    if (spawn != null)
                        spawn.teleport(player);
                    p.setLastSpawn(spawn);
                });
            } else {
                BlockItem blockItem = p.getSelectedBlockItem();
                spawn.teleport(player);
                if (blockItem != null)
                    blockItem.give(player);
            }
            p.setLastSpawn(spawn);
        }
    }
}
