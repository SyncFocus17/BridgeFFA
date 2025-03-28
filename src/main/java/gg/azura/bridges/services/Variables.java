package gg.azura.bridges.services;

import gg.azura.bridges.Bridges;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Variables {

    private final Bridges plugin;

    public String mysqlHost;

    public String mysqlDatabase;

    public String mysqlUsername;

    public String mysqlPassword;

    public int mysqlPort;

    public boolean mysqlSSL;

    public Set<World> worlds;

    public String deathMessageSuffix;

    public boolean healOnKill;

    public Location lobby;

    public double lobbyHRadius;

    public double lobbyVRadius;

    public String lobbyXBounds;

    public String lobbyYBounds;

    public String lobbyZBounds;

    public boolean overrideKillCommand;

    public int deathY;

    public int activateY;

    public String lobbyDetection;

    public boolean disableFallDamage;

    public boolean quickRespawn;

    public Variables(Bridges plugin) {
        this.deathMessageSuffix = "&7(&c%s<3&&)";
        this.healOnKill = true;
        this.lobbyHRadius = 20.0D;
        this.lobbyVRadius = 6.0D;
        this.overrideKillCommand = true;
        this.deathY = 15;
        this.activateY = 60;
        this.lobbyDetection = "ycoord";
        this.disableFallDamage = true;
        this.quickRespawn = true;
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        this.plugin.getConfig().options().copyDefaults(true);
        this.plugin.saveConfig();
        this.plugin.reloadConfig();
        FileConfiguration config = this.plugin.getConfig();
        this.mysqlHost = config.getString("mysql.host");
        this.mysqlPort = config.getInt("mysql.port");
        this.mysqlDatabase = config.getString("mysql.database");
        this.mysqlUsername = config.getString("mysql.username");
        this.mysqlPassword = config.getString("mysql.password");
        this.mysqlSSL = config.getBoolean("mysql.ssl");
        this.worlds = (Set<World>)config.getStringList("worlds").stream().map(s -> this.plugin.getServer().getWorld(s)).filter(Objects::nonNull).collect(Collectors.toSet());
        this.deathMessageSuffix = this.plugin.getConfig().getString("death_message_suffix");
        this.healOnKill = this.plugin.getConfig().getBoolean("heal_on_kill");
        this.disableFallDamage = this.plugin.getConfig().getBoolean("disable_falldamage");
        this.overrideKillCommand = this.plugin.getConfig().getBoolean("override_kill_command");
        this.deathY = this.plugin.getConfig().getInt("death_y");
        this.activateY = this.plugin.getConfig().getInt("activate_y");
        this

                .lobby = new Location(this.plugin.getServer().getWorld(Objects.requireNonNull(this.plugin.getConfig().getString("lobby.world"))), this.plugin.getConfig().getDouble("lobby.x"), this.plugin.getConfig().getDouble("lobby.y"), this.plugin.getConfig().getDouble("lobby.z"), (float)this.plugin.getConfig().getDouble("lobby.yaw"), (float)this.plugin.getConfig().getDouble("lobby.pitch"));
        this.lobbyHRadius = this.plugin.getConfig().getDouble("lobby.hradius");
        this.lobbyVRadius = this.plugin.getConfig().getDouble("lobby.vradius");
        this.lobbyXBounds = this.plugin.getConfig().getString("lobby.xbounds");
        this.lobbyYBounds = this.plugin.getConfig().getString("lobby.ybounds");
        this.lobbyZBounds = this.plugin.getConfig().getString("lobby.zbounds");
        this.lobbyDetection = this.plugin.getConfig().getString("lobby_detection");
        this.quickRespawn = this.plugin.getConfig().getBoolean("quick_respawn", true);
    }
}
