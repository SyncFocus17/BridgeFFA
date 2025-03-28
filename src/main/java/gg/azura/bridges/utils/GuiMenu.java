package gg.azura.bridges.utils;

import gg.azura.bridges.Bridges;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public abstract class GuiMenu implements Listener {

    public final Bridges plugin = Bridges.get();

    public Player player;

    private Inventory inventory;

    private String title;

    private int size;

    private List<ItemStack> items;

    private boolean closeOnClick = true;

    public GuiMenu(Player player, String title, int size) {
        this(player, title, size, new ArrayList<>());
    }

    public GuiMenu(Player player, String title, int size, List<ItemStack> items) {
        this.player = player;
        this.title = title;
        if (size <= 9) {
            this.size = 9;
        } else if (size > 9 && size <= 18) {
            this.size = 18;
        } else if (size > 18 && size <= 27) {
            this.size = 27;
        } else if (size > 27 && size <= 36) {
            this.size = 36;
        } else if (size > 36 && size <= 45) {
            this.size = 45;
        } else {
            this.size = 54;
        }
        this.items = items;
        this.inventory = this.plugin.getServer().createInventory(null, this.size, title);
        items.forEach(item -> this.inventory.addItem(new ItemStack[] { item }));
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public String getTitle() {
        return this.title;
    }

    public void open() {
        this.plugin.getServer().getPluginManager().registerEvents(this, (Plugin) this.plugin);
        this.player.openInventory(this.inventory);
    }

    public void close(boolean event) {
        if (!this.player.getOpenInventory().getTopInventory().equals(this.inventory))
            return;
        if (!event)
            this.player.closeInventory();
        HandlerList.unregisterAll(this);
        onClose();
    }

    public void setCloseOnClick(boolean closeOnClick) {
        this.closeOnClick = closeOnClick;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null)
            return;
        if (!event.getClickedInventory().equals(this.inventory))
            return;
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE)
            return;
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
        clickHandler(event.getCurrentItem(), event.getSlot());
        if (this.closeOnClick)
            close(false);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(this.inventory))
            close(true);
    }

    public abstract void clickHandler(ItemStack paramItemStack, int paramInt);

    public abstract void onClose();
}
