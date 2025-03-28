package gg.azura.bridges.services;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import gg.azura.bridges.gui.SoundSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerManager {

    private final Bridges plugin;
    private final Set<BridgePlayer> players;
    private final Map<UUID, Map<Object, Object>> savingQueue;
    private SoundSettings soundSettings;

    public PlayerManager(Bridges plugin, SoundSettings soundSettings) {
        this.plugin = plugin;
        this.soundSettings = soundSettings;
        this.players = new HashSet<>();
        this.savingQueue = new HashMap<>();
        startSaveTask();
    }

    public BridgePlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public BridgePlayer getPlayer(UUID uuid) {
        return this.players.stream()
                .filter(p -> p.getUUID().equals(uuid))
                .findAny()
                .orElse(null);
    }

    public BridgePlayer getPlayer(String name) {
        return this.players.stream()
                .filter(p -> p.getName().toLowerCase().startsWith(name.toLowerCase()))
                .findAny()
                .orElse(null);
    }

    public BridgePlayer getBridgePlayer(UUID uniqueId) {
        return this.players.stream()
                .filter(bridgePlayer -> bridgePlayer.getUUID().equals(uniqueId))
                .findFirst()
                .orElseGet(() -> {
                    BridgePlayer bridgePlayer = loadBridgePlayer(uniqueId);
                    if (bridgePlayer != null) {
                        this.players.add(bridgePlayer);
                    }
                    return bridgePlayer;
                });
    }

    private BridgePlayer loadBridgePlayer(UUID uniqueId) {
        // Load player data from database if player is not in cache
        try {
            return this.plugin.getSM().getDBManager().getConnection().thenApply(connection -> {
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM bridgeffa_players WHERE uuid = ?")) {
                    ps.setString(1, uniqueId.toString());
                    var resultSet = ps.executeQuery();
                    if (resultSet.next()) {
                        return new BridgePlayer(uniqueId, resultSet);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return null;
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addPlayer(Player player) {
        this.players.add(new BridgePlayer(player));
    }

    public void removePlayer(Player player) {
        BridgePlayer bridgePlayer = getPlayer(player);
        if (bridgePlayer != null) {
            saveNow(false); // Save any pending changes
            this.players.remove(bridgePlayer);
        }
    }

    public void queueDataSave(boolean forceSave, BridgePlayer player, Object key, Object value) {
        if (forceSave) {
            save(player.getUUID(), key, value);
        } else {
            Map<Object, Object> data = this.savingQueue.getOrDefault(player.getUUID(), new HashMap<>());
            data.put(key, value);
            this.savingQueue.put(player.getUUID(), data);
        }
    }

    private void save(UUID uuid, Object key, Object value) {
        this.plugin.getSM().getDBManager().getConnection().thenAccept(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    String.format("INSERT INTO bridgeffa_players(uuid, %s) VALUES(?, ?) ON DUPLICATE KEY UPDATE %s = ?;",
                            key, key))) {
                ps.setString(1, uuid.toString());
                ps.setObject(2, value);
                ps.setObject(3, value);
                ps.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void saveNow(boolean async) {
        Runnable saveTask = () -> {
            this.savingQueue.forEach((uuid, data) -> {
                data.forEach((key, value) -> save(uuid, key, value));
            });
            this.savingQueue.clear();
        };

        if (async) {
            new Thread(saveTask).start();
        } else {
            saveTask.run();
        }
    }

    private void startSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (PlayerManager.this.savingQueue.isEmpty()) {
                    return;
                }
                PlayerManager.this.saveNow(true);
            }
        }.runTaskTimerAsynchronously((Plugin) this.plugin, 200L, 1200L);
    }

    public SoundSettings getSoundSettings(UUID uniqueId) {
        return soundSettings;
    }
}