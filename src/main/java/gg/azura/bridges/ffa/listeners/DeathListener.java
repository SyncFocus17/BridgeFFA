package gg.azura.bridges.ffa.listeners;

import java.util.Objects;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import gg.azura.bridges.utils.CC;
import gg.azura.bridges.utils.ItemBuilder;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class DeathListener implements Listener {

    private final Bridges plugin;

    public DeathListener(Bridges plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.getPlayer().getGameMode().equals(GameMode.SURVIVAL) && !event.getPlayer().getGameMode().equals(GameMode.ADVENTURE))
            return;
        if (event.getTo().getY() > (this.plugin.getSM().getVariables()).deathY)
            return;
        BridgePlayer user = this.plugin.getSM().getPlayerManager().getPlayer(event.getPlayer());
        if (user == null)
            return;
        if (!user.isInFFAWorld())
            return;
        event.getPlayer().damage(event.getPlayer().getMaxHealth());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!(this.plugin.getSM().getVariables()).worlds.contains(event.getEntity().getLocation().getWorld()))
            return;
        if (this.plugin.parseVersion() <= 16.5D || (this.plugin.parseVersion() > 16.5D && Objects.equals(event.getEntity().getWorld().getGameRuleValue(GameRule.DO_IMMEDIATE_RESPAWN), Boolean.valueOf(false))))
            event.getEntity().setHealth(event.getEntity().getMaxHealth());
        if (event.getEntity().getKiller() != null && (this.plugin.getSM().getVariables()).healOnKill)
            event.getEntity().getKiller().setHealth(event.getEntity().getKiller().getMaxHealth());
        event.getDrops().clear();
        event.getEntity().teleport((this.plugin.getSM().getVariables()).lobby);
        event.getEntity().getInventory().clear();
        event.getEntity().getActivePotionEffects().forEach(pe -> event.getEntity().removePotionEffect(pe.getType()));
        if (event.getEntity().getKiller() != null) {
            if ((this.plugin.getSM().getVariables()).healOnKill)
                event.getEntity().getKiller().setHealth(event.getEntity().getKiller().getMaxHealth());
            BridgePlayer player = this.plugin.getSM().getPlayerManager().getPlayer(event.getEntity().getKiller());
            player.setCoins(player.getCoins() + 2);
        }
    }

    private static final ItemStack QUICK_RESPAWN_ITEM = (new ItemBuilder(Material.CLOCK))
            .setName(CC.t("&e&lQuick Respawn"))
            .setLore(new String[] { "", CC.t("&6Nidocraft Network!"),
                    CC.t(""), "" }).build();

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        BridgePlayer user = this.plugin.getSM().getPlayerManager().getPlayer(event.getPlayer());
        if (user == null)
            return;
        if (!user.isInFFAWorld())
            return;
        event.setRespawnLocation((this.plugin.getSM().getVariables()).lobby);
        event.getPlayer().setFoodLevel(20);
        event.getPlayer().setSaturation(0.0F);
        if ((this.plugin.getSM().getVariables()).quickRespawn)
            event.getPlayer().getInventory().addItem(new ItemStack[] { QUICK_RESPAWN_ITEM });
    }
}
