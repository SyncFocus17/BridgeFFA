package gg.azura.bridges.ffa.gui;

import gg.azura.bridges.BlockItem;
import gg.azura.bridges.ffa.Spawn;
import gg.azura.bridges.utils.CC;
import gg.azura.bridges.utils.GuiMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SpawnMenu extends GuiMenu {

    private final BlockItem blockitem;

    public SpawnMenu(Player player, int size, BlockItem blockitemToGive) {
        super(player, CC.t("&6Spawn selection"), size);
        this.blockitem = blockitemToGive;
        this.plugin.getSM().getSpawnManager().getSpawns().forEach(spawn -> {
            ItemStack guiItem = spawn.getGUIItem();
            if (guiItem != null) {
                getInventory().addItem(guiItem);
            }
        });
    }

    @Override
    public void clickHandler(ItemStack item, int slot) {
        Spawn spawn = this.plugin.getSM().getSpawnManager().getSpawn(item);
        if (spawn == null) {
            setCloseOnClick(false);
            return;
        }
        spawn.teleport(this.player);
        if (this.blockitem != null) {
            this.blockitem.give(this.player);
        }
    }

    @Override
    public void onClose() {}
}