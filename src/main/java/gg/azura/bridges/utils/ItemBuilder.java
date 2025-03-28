package gg.azura.bridges.utils;

import gg.azura.bridges.Bridges;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class ItemBuilder {

    private final Material material;
    private int amount;
    private short data;
    private String name;
    private List<String> lore;
    private Map<Enchantment, Integer> enchantments;
    private Set<ItemFlag> flags;
    private boolean unbreakable;
    private Map<String, String> nbtTags;
    private boolean glowing;
    private Consumer<ItemMeta> metaConsumer;

    /**
     * Creates a new ItemBuilder with the specified material
     * @param material The material to use
     * @throws IllegalArgumentException if material is null
     */
    public ItemBuilder(Material material) {
        this.material = Objects.requireNonNull(material, "Material cannot be null");
        this.amount = 1;
        this.data = 0;
        this.enchantments = new HashMap<>();
        this.flags = EnumSet.noneOf(ItemFlag.class);
        this.nbtTags = new HashMap<>();
    }


    /**
     * Creates a new ItemBuilder from an existing ItemStack with modern features
     * @param item The ItemStack to copy
     */
    public ItemBuilder(ItemStack item) {
        Objects.requireNonNull(item, "ItemStack cannot be null");
        this.material = item.getType();
        this.amount = item.getAmount();
        this.data = item.getDurability();
        this.enchantments = new HashMap<>(item.getEnchantments());
        this.flags = EnumSet.noneOf(ItemFlag.class);
        this.nbtTags = new HashMap<>();

        Optional.ofNullable(item.getItemMeta()).ifPresent(meta -> {
            this.name = meta.hasDisplayName() ? meta.getDisplayName() : null;
            this.lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : null;
            this.flags.addAll(meta.getItemFlags());
            this.unbreakable = meta.isUnbreakable();
            this.glowing = meta.hasEnchants() && meta.getItemFlags().contains(ItemFlag.HIDE_ENCHANTS);
        });
    }


    /**
     * Sets the amount of the item
     * @param amount The amount to set
     * @return The ItemBuilder instance
     * @throws IllegalArgumentException if amount is less than 1
     */
    public ItemBuilder setAmount(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        this.amount = amount;
        return this;
    }

    /**
     * Sets the data value of the item
     * @param data The data value to set
     * @return The ItemBuilder instance
     */
    public ItemBuilder setData(int data) {
        this.data = (short) data;
        return this;
    }

    /**
     * Sets the display name of the item
     * @param name The name to set
     * @return The ItemBuilder instance
     */
    public ItemBuilder setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the lore of the item
     * @param lore The lore lines to set
     * @return The ItemBuilder instance
     */
    public ItemBuilder setLore(String... lore) {
        this.lore = Arrays.asList(lore);
        return this;
    }

    /**
     * Sets the lore of the item
     * @param lore The list of lore lines to set
     * @return The ItemBuilder instance
     */
    public ItemBuilder setLore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
        return this;
    }

    /**
     * Adds lines to the lore
     * @param lines The lines to add
     * @return The ItemBuilder instance
     */
    public ItemBuilder addLore(String... lines) {
        if (this.lore == null) {
            this.lore = new ArrayList<>();
        }
        this.lore.addAll(Arrays.asList(lines));
        return this;
    }

    /**
     * Adds an enchantment to the item
     *
     * @param enchantment The enchantment to add
     * @param level       The level of the enchantment
     * @param b
     * @return The ItemBuilder instance
     */
    public ItemBuilder addEnchantment(Enchantment enchantment, int level, boolean b) {
        if (enchantment != null) {
            this.enchantments.put(enchantment, level);
        }
        return this;
    }

    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchants) {
        this.enchantments.putAll(enchants);
        return this;
    }

    /**
     * Adds an ItemFlag to the item
     * @param itemFlag The ItemFlag to add
     * @return The ItemBuilder instance
     */
    public ItemBuilder addFlag(ItemFlag itemFlag) {
        if (itemFlag != null) {
            this.flags.add(itemFlag);
        }
        return this;
    }

    /**
     * Adds multiple ItemFlags to the item
     * @param itemFlags The ItemFlags to add
     * @return The ItemBuilder instance
     */
    public ItemBuilder addFlags(ItemFlag... itemFlags) {
        this.flags.addAll(Arrays.asList(itemFlags));
        return this;
    }

    /**
     * Sets whether the item is unbreakable
     * @param unbreakable Whether the item should be unbreakable
     * @return The ItemBuilder instance
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    /**
     * Builds the final ItemStack
     * @return The built ItemStack
     */
    public ItemStack build() {
        ItemStack item = new ItemStack(this.material, this.amount, this.data);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (this.name != null) {
                meta.setDisplayName(this.name);
            }

            if (this.lore != null) {
                meta.setLore(this.lore);
            }

            if (!this.enchantments.isEmpty()) {
                this.enchantments.forEach((enchantment, level) ->
                        meta.addEnchant(enchantment, level, true));
            }

            if (!this.flags.isEmpty()) {
                this.flags.forEach(meta::addItemFlags);
            }

            meta.setUnbreakable(this.unbreakable);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Gets the material of the item
     * @return The material
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Gets the current amount
     * @return The amount
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Gets the current data value
     * @return The data value
     */
    public short getData() {
        return data;
    }

    /**
     * Gets the current display name
     * @return The display name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the current lore
     * @return The lore
     */
    public List<String> getLore() {
        return lore != null ? new ArrayList<>(lore) : new ArrayList<>();
    }

    public ItemBuilder applyIf(boolean condition, Consumer<ItemBuilder> consumer) {
        if (condition) consumer.accept(this);
        return this;
    }
    /**
     * Adds a glowing effect to the item
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:52:51 UTC
     * @return ItemBuilder instance for chaining
     */
    public ItemBuilder addGlow() {
        this.glowing = true;
        this.flags.add(ItemFlag.HIDE_ENCHANTS);

        // Add modern glow effect metadata
        return this.addEnchantment(
                Enchantment.EFFICIENCY,
                1, false
        ).addPersistentData(
                Bridges.get(),
                "glow_effect",
                PersistentDataType.STRING,
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }

    /**
     * Adds persistent data to the item
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:52:51 UTC
     * @param plugin The plugin instance
     * @param key The key for the data
     * @param type The data type
     * @param value The value to store
     * @return ItemBuilder instance for chaining
     */
    public <T, Z> ItemBuilder addPersistentData(
            Plugin plugin,
            String key,
            PersistentDataType<T, Z> type,
            Z value
    ) {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

        // Create metadata consumer
        this.metaConsumer = meta -> {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            meta.getPersistentDataContainer().set(namespacedKey, type, value);

            // Add metadata timestamp
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, key + "_timestamp"),
                    PersistentDataType.STRING,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            );
        };

        return this;
    }

    /**
     * Modifies item metadata with modern functionality
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:54:53 UTC
     * @param consumer The metadata consumer function
     * @return ItemBuilder instance for chaining
     * @throws IllegalArgumentException if consumer is null
     */
    public ItemBuilder modifyMeta(Consumer<ItemMeta> consumer) {
        Objects.requireNonNull(consumer, "Metadata consumer cannot be null");

        this.metaConsumer = meta -> {
            // Apply consumer modifications
            consumer.accept(meta);

            // Track modification timestamp
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(Bridges.get(), "last_modified"),
                    PersistentDataType.STRING,
                    "2025-02-04 10:54:53"
            );

            // Track modifier
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(Bridges.get(), "modified_by"),
                    PersistentDataType.STRING,
                    "Admin"
            );
        };

        return this;
    }
}