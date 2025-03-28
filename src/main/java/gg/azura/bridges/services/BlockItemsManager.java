package gg.azura.bridges.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;

import gg.azura.bridges.BlockItem;
import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.Bridges;
import gg.azura.bridges.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BlockItemsManager {

    private final Bridges plugin;
    private final List<BlockItem> blockItems;
    private final File configFile;
    private final Gson gson;

    public BlockItemsManager(Bridges plugin) {
        this.plugin = plugin;
        this.blockItems = new CopyOnWriteArrayList<>();
        this.configFile = new File(plugin.getDataFolder(), "blocks.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadBlockItems();
    }

    public List<BlockItem> getBlockItems() {
        return new ArrayList<>(blockItems);
    }

    public boolean exists(Material material) {
        return material != null && blockItems.stream()
                .anyMatch(bi -> bi.getItem().getType() == material);
    }

    public BlockItem getBlockItem(Material material) {
        if (material == null) return null;
        return blockItems.stream()
                .filter(bi -> bi.getItem().getType() == material)
                .findFirst()
                .orElse(null);
    }

    public Set<BlockItem> getBlockItems(int price) {
        return blockItems.stream()
                .filter(bi -> bi.getPrice() == price)
                .collect(Collectors.toSet());
    }

    public BlockItem getBlockItem(Player player) {
        if (player == null) return null;
        BridgePlayer bridgePlayer = plugin.getSM().getPlayerManager().getPlayer(player);
        return bridgePlayer != null ? bridgePlayer.getSelectedBlockItem() : null;
    }

    public BlockItem getBlockItem(BridgePlayer player) {
        return player != null ? player.getSelectedBlockItem() : null;
    }

    public List<BlockItem> fromJson(String blocks) {
        if (blocks == null || blocks.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<BlockItem> list = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(blocks).getAsJsonObject();
            for (String materialName : json.keySet()) {
                try {
                    Material material = Material.getMaterial(materialName.toUpperCase().trim());
                    if (material == null) {
                        plugin.getLogger().warning("Invalid material name in blocks.json: " + materialName);
                        continue;
                    }
                    int price = json.get(materialName).getAsInt();
                    list.add(new BlockItem(material, price, materialName, "Default Description"));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error parsing block item: " + materialName, e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error parsing JSON string: " + blocks, e);
        }
        return list;
    }

    public JsonObject toJson(List<BlockItem> unlockedBlocks) {
        JsonObject json = new JsonObject();
        if (unlockedBlocks != null) {
            unlockedBlocks.forEach(block -> {
                if (block != null && block.getItem() != null && block.getItem().getType() != null) {
                    json.addProperty(block.getItem().getType().name(), block.getPrice());
                }
            });
        }
        return json;
    }

    public synchronized void saveNewBlockItem(Material material, int price) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        try {
            JsonObject json = loadJsonConfig();
            json.addProperty(material.name(), price);

            try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
                gson.toJson(json, writer);
            }

            blockItems.add(new BlockItem(material, price, material.name(), "Default Description"));
            plugin.getLogger().info("Successfully saved new block item: " + material.name());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save block item: " + material.name(), e);
            throw new RuntimeException("Failed to save block item", e);
        }
    }

    private JsonObject loadJsonConfig() throws IOException {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();
            return new JsonObject();
        }

        try (Reader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error reading config file", e);
            return new JsonObject();
        }
    }

    public void loadBlockItems() {
        blockItems.clear();
        try {
            if (!configFile.exists()) {
                plugin.getLogger().info("No blocks.json found, creating empty configuration");
                return;
            }

            String jsonText = readFileContent(configFile);
            List<BlockItem> loadedItems = fromJson(jsonText);
            blockItems.addAll(loadedItems);
            plugin.getLogger().info("Successfully loaded " + loadedItems.size() + " block items");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load block items", e);
            plugin.getServer().getLogger().log(Level.WARNING, "");
            plugin.getServer().getLogger().log(Level.WARNING, CC.t("&cUnable to parse blocks json. Read stacktrace above."));
            plugin.getServer().getLogger().log(Level.WARNING, "");
        }
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    public void reloadBlockItems() {
        loadBlockItems();
    }

    public boolean removeBlockItem(Material material) {
        if (material == null) return false;

        boolean removed = blockItems.removeIf(item -> item.getItem().getType() == material);
        if (removed) {
            saveBlockItems();
        }
        return removed;
    }

    private void saveBlockItems() {
        try {
            JsonObject json = toJson(new ArrayList<>(blockItems));
            try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save block items", e);
        }
    }
}