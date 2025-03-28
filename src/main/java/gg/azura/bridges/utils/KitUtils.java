package gg.azura.bridges.utils;

import com.google.gson.*;
import gg.azura.bridges.Bridges;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class KitUtils {

    private static final Bridges plugin = Bridges.get();

    public static ItemStack jsonToGuiItem(JsonObject json) {
        ItemStack is = new ItemStack(getMaterial(json.get("material").getAsString()));
        ItemMeta meta = is.getItemMeta();
        if (json.has("displayname"))
            meta.setDisplayName(CC.t(json.get("displayname").getAsString()));
        List<String> lore = new ArrayList<>();
        if (json.has("lore")) {
            for (int i = 0; i < json.get("lore").getAsJsonArray().size(); i++)
                lore.add(json.get("lore").getAsJsonArray().get(i).getAsString());
            meta.setLore(lore);
        }
        is.setItemMeta(meta);
        return is;
    }

    public static JsonObject guiItemToJson(ItemStack item) {
        JsonObject json = new JsonObject();
        json.addProperty("material", item.getType().name());
        json.addProperty("displayname", item.getItemMeta().getDisplayName());
        json.add("lore", (new JsonParser()).parse((new GsonBuilder()).create().toJson(Optional.<List>ofNullable(item.getItemMeta().getLore()).orElse(new ArrayList()))));
        return json;
    }

    public static ItemStack jsonToInvItem(JsonObject json) {
        ItemStack is = new ItemStack(getMaterial(json.get("material").getAsString()), json.get("amount").getAsInt());
        if (json.has("enchantments")) {
            ItemMeta meta = is.getItemMeta();
            List<JsonObject> enchantments = new ArrayList<>();
            for (int i = 0; i < json.get("enchantments").getAsJsonArray().size(); i++)
                enchantments.add(json.get("enchantments").getAsJsonArray().get(i).getAsJsonObject());
            for (JsonObject enchantment : enchantments)
                meta.addEnchant(Objects.requireNonNull(Enchantment.getByName(enchantment.get("name").getAsString())), enchantment.get("level").getAsInt(), true);
            is.setItemMeta(meta);
        }
        return is;
    }

    public static JsonObject invItemToJson(int slot, ItemStack item) {
        JsonObject json = new JsonObject();
        json.addProperty("slot", Integer.valueOf(slot));
        json.addProperty("material", item.getType().name());
        json.addProperty("amount", Integer.valueOf(item.getAmount()));
        JsonArray array = new JsonArray();
        item.getEnchantments().keySet().forEach(enchantment -> {
            JsonObject json2 = new JsonObject();
            int level = ((Integer)item.getEnchantments().get(enchantment)).intValue();
            json2.addProperty("name", enchantment.getName());
            json2.addProperty("level", Integer.valueOf(level));
            array.add((JsonElement) json2);
        });
        json.add("enchantments", (JsonElement)array);
        return json;
    }

    private static Material getMaterial(String name) {
        Material material = Material.getMaterial(name);
        if (plugin.parseVersion() >= 1.13D && material == null)
            return Material.getMaterial("LEGACY_" + name);
        return material;
    }
}
