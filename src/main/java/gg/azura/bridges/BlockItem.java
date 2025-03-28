package gg.azura.bridges;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BlockItem implements Buyable {

    private final ItemStack item;
    private final int price;
    private final String category;
    private final String description;

    // Constructor using material name
    public BlockItem(String materialName, int price, String category, String description) {
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material name: " + materialName);
        }
        this.item = new ItemStack(material, 64);
        this.price = price;
        this.category = category;
        this.description = description;
    }

    // Constructor using material
    public BlockItem(Material material, int price, String category, String description) {
        this.item = new ItemStack(material, 64);
        this.price = price;
        this.category = category;
        this.description = description;
    }

    // Constructor using ItemStack
    public BlockItem(ItemStack itemStack, int price, String category, String description) {
        this.item = itemStack.clone();
        this.price = price;
        this.category = category;
        this.description = description;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public int getPrice() {
        return this.price;
    }

    public String getCategory() {
        return this.category;
    }

    public String getDescription() {
        return this.description;
    }

    public void give(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.addItem(this.item);
    }

    public boolean canAfford(Player player) {
        int playerCoins = getPlayerCoins(player);
        return playerCoins >= this.price;
    }

    public void purchase(Player player) {
        if (canAfford(player)) {
            deductPlayerCoins(player, this.price);
            give(player);
            player.sendMessage(ChatColor.GREEN + "You have purchased " + this.item.getType() + " for " + this.price + " coins.");
        } else {
            player.sendMessage(ChatColor.RED + "You cannot afford this item.");
        }
    }

    private int getPlayerCoins(Player player) {
        BridgePlayer bridgePlayer = Bridges.get().getSM().getPlayerManager().getBridgePlayer(player.getUniqueId());
        return bridgePlayer.getCoins();
    }

    private void setPlayerCoins(Player player, int coins) {
        BridgePlayer bridgePlayer = Bridges.get().getSM().getPlayerManager().getBridgePlayer(player.getUniqueId());
        bridgePlayer.setCoins(coins);
    }

    private void deductPlayerCoins(Player player, int price) {
        int currentCoins = getPlayerCoins(player);
        int newBalance = currentCoins - price;
        setPlayerCoins(player, newBalance);
        player.sendMessage(ChatColor.GREEN + "Your new balance is " + newBalance + " coins.");
    }
}