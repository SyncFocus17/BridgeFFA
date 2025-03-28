package gg.azura.bridges.tasks;

import gg.azura.bridges.Bridges;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

public class ArmorstandBlockingTask extends BukkitRunnable {

    private final Bridges plugin;

    public ArmorstandBlockingTask(Bridges plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        this.plugin.getServer().getOnlinePlayers().forEach(player -> {
            try {
                // Get nearby entities within a small radius
                player.getNearbyEntities(0.1D, 0.1D, 0.1D).forEach(entity -> {
                    // Check if the entity is an ArmorStand
                    if (entity.getType() == EntityType.ARMOR_STAND) {
                        handleNearbyArmorStand(player, (ArmorStand) entity);
                    }
                });
            } catch (Exception e) {
                // Log any exceptions for debugging purposes
                plugin.getLogger().severe("Error while processing nearby entities for player: " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void handleNearbyArmorStand(Player player, ArmorStand armorStand) {
        armorStand.remove();
        player.sendMessage(ChatColor.RED + "An ArmorStand near you has been removed!");
    }
}