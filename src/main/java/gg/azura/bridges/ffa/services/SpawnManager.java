package gg.azura.bridges.ffa.services;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.azura.bridges.Bridges;
import gg.azura.bridges.ffa.Spawn;
import gg.azura.bridges.utils.CC;
import gg.azura.bridges.utils.SpawnUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class SpawnManager {

    private final Bridges plugin;

    private final List<Spawn> spawns;

    public SpawnManager(Bridges plugin) {
        this.spawns = new ArrayList<>();
        this.plugin = plugin;
        (new BukkitRunnable() {
            public void run() {
                SpawnManager.this.loadSpawns();
            }
        }).runTaskLaterAsynchronously((Plugin) plugin, 40L);
    }

    public List<Spawn> getSpawns() {
        return this.spawns;
    }

    public boolean exists(String name) {
        return this.spawns.stream().anyMatch(spawn -> (spawn.getName().equalsIgnoreCase(name) || spawn.getName().toLowerCase().contains(name.toLowerCase()) || name.toLowerCase().contains(spawn.getName().toLowerCase())));
    }

    public Spawn getSpawn(String name) {
        return this.spawns.stream().filter(spawn -> (spawn.getName().equalsIgnoreCase(name) || spawn.getName().toLowerCase().contains(name.toLowerCase()) || name.toLowerCase().contains(spawn.getName().toLowerCase()))).findAny().orElse(null);
    }

    public Spawn getSpawn(ItemStack guiItem) {
        return this.spawns.stream().filter(spawn -> spawn.getGUIItem().equals(guiItem)).findAny().orElse(null);
    }

    public Spawn getDefaultSpawn() {
        if (this.spawns.isEmpty())
            return null;
        return this.spawns.get(0);
    }

    public void saveNewSpawn(Player player, String name) throws IOException {
        if (exists(name))
            return;
        File file = new File(this.plugin.getDataFolder() + "/spawns", name + ".json");
        if (file.exists())
            return;
        file.createNewFile();
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.add("gui_item", (JsonElement)SpawnUtils.guiItemToJson(player.getItemInHand()));
        json.add("location", (JsonElement)SpawnUtils.locationToJson(player.getLocation()));
        FileWriter writer = new FileWriter(file);
        writer.write(json.toString());
        writer.close();
        loadSpawn(file);
    }

    public void loadSpawn(File file) {
        try {
            String jsonText = readAll(new FileReader(file));
            JsonObject json = (new JsonParser()).parse(jsonText).getAsJsonObject();
            String name = json.get("name").getAsString();
            ItemStack gui_item = SpawnUtils.jsonToGuiItem(json.get("gui_item").getAsJsonObject());
            Location location = SpawnUtils.jsonToLocation(json.get("location").getAsJsonObject());
            this.spawns.add(new Spawn(name, gui_item, location));
        } catch (IOException ex) {
            ex.printStackTrace();
            this.plugin.getServer().getLogger().log(Level.WARNING, "");
            this.plugin.getServer().getLogger().log(Level.WARNING, CC.t("&cUnable to parse spawn json. Read stacktrace above."));
            this.plugin.getServer().getLogger().log(Level.WARNING, "");
        }
    }

    public void loadSpawns() {
        File dir = new File(this.plugin.getDataFolder() + "/spawns");
        if (!dir.exists())
            dir.mkdirs();
        File[] files = dir.listFiles();
        if (files.length == 0)
            return;
        Arrays.sort((Object[])files);
        for (File file : files) {
            if (!file.isDirectory())
                loadSpawn(file);
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1)
            sb.append((char)cp);
        return sb.toString();
    }
}
