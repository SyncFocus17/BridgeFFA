package gg.azura.bridges.gui;

import gg.azura.bridges.BlockItem;
import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.utils.CC;
import gg.azura.bridges.utils.GuiMenu;
import gg.azura.bridges.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ShopMenu extends GuiMenu {

    private final BridgePlayer player;
    private final Map<ShopCategory, List<BlockItem>> categoryItems;
    private final Map<Integer, List<ItemStack>> pages;
    private ShopCategory currentCategory;
    private int currentPage;

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final Sound HOVER_SOUND = Sound.BLOCK_NOTE_BLOCK_PLING;
    private static final Sound SELECT_SOUND = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private static final Sound PURCHASE_SOUND = Sound.ENTITY_PLAYER_LEVELUP;
    private static final Sound ERROR_SOUND = Sound.BLOCK_NOTE_BLOCK_BASS;
    private static final Sound NAVIGATE_SOUND = Sound.UI_BUTTON_CLICK;

    public ShopMenu(BridgePlayer player) {
        super(player.getPlayer(), CC.tf("&8⚡ &b&lFUTURISTIC SHOP &8⚡"), 54);
        this.player = player;
        this.categoryItems = new EnumMap<>(ShopCategory.class);
        this.pages = new HashMap<>();
        this.currentCategory = ShopCategory.FEATURED;
        this.currentPage = 1;
        initializeCategories();
        initializeMenu();
    }

    private void initializeCategories() {
        // Featured items (most popular or special items)
        categoryItems.put(ShopCategory.FEATURED, Arrays.asList(
                new BlockItem(Material.BEACON, 11000, "Featured", "Premium light source"),
                new BlockItem(Material.DIAMOND_BLOCK, 90000, "Featured", "Pure diamond block"),
                new BlockItem(Material.EMERALD_BLOCK, 100000, "Featured", "Premium emerald block")
        ));

        // Premium blocks category
        categoryItems.put(ShopCategory.PREMIUM, Arrays.asList(
                new BlockItem(Material.OBSIDIAN, 150000, "Premium", "Indestructible obsidian"),
                new BlockItem(Material.BEDROCK, 200000, "Premium", "The ultimate block"),
                new BlockItem(Material.NETHERITE_BLOCK, 250000, "Premium", "Pure netherite")
        ));

        // Modern building blocks
        categoryItems.put(ShopCategory.MODERN, Arrays.asList(
                new BlockItem(Material.SMOOTH_QUARTZ, 5000, "Modern", "Sleek quartz finish"),
                new BlockItem(Material.WHITE_CONCRETE, 6000, "Modern", "Clean concrete"),
                new BlockItem(Material.GRAY_CONCRETE, 6000, "Modern", "Industrial concrete"),
                new BlockItem(Material.BLACK_CONCRETE, 7500, "Modern", "Elite concrete"),
                new BlockItem(Material.SMOOTH_STONE, 4500, "Modern", "Polished stone")
        ));

        // Nature-themed blocks
        categoryItems.put(ShopCategory.NATURE, Arrays.asList(
                new BlockItem(Material.MOSS_BLOCK, 3500, "Nature", "Living moss"),
                new BlockItem(Material.FLOWERING_AZALEA, 4500, "Nature", "Flowering beauty"),
                new BlockItem(Material.GRASS_BLOCK, 2500, "Nature", "Natural grass"),
                new BlockItem(Material.WARPED_STEM, 5500, "Nature", "Exotic wood")
        ));

        // Tech-themed blocks
        categoryItems.put(ShopCategory.TECH, Arrays.asList(
                new BlockItem(Material.SEA_LANTERN, 8000, "Tech", "Advanced lighting"),
                new BlockItem(Material.END_ROD, 7500, "Tech", "Energy rod"),
                new BlockItem(Material.REDSTONE_LAMP, 6500, "Tech", "Power lamp"),
                new BlockItem(Material.CONDUIT, 15000, "Tech", "Power core")
        ));

        // Decorative blocks
        categoryItems.put(ShopCategory.DECORATIVE, Arrays.asList(
                new BlockItem(Material.AMETHYST_BLOCK, 12000, "Decorative", "Crystal block"),
                new BlockItem(Material.CRYING_OBSIDIAN, 16000, "Decorative", "Energized obsidian"),
                new BlockItem(Material.COPPER_BLOCK, 8000, "Decorative", "Pure copper"),
                new BlockItem(Material.TINTED_GLASS, 9000, "Decorative", "Privacy glass")
        ));
    }

    protected void initializeMenu() {
        updateInventory();
    }

    private void updateInventory() {
        getInventory().clear();
        decorateBackground();
        setupCategoryButtons();
        displayCurrentCategoryItems();
        setupNavigationButtons();
        displayPlayerInfo();
    }

    private void decorateBackground() {
        for (int i = 0; i < getInventory().getSize(); i++) {
            if (!isSlotReserved(i)) {
                Material material = (i % 2 == 0) ? Material.BLACK_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                getInventory().setItem(i, new ItemBuilder(material)
                        .setName(" ")
                        .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                        .build());
            }
        }
    }

    private boolean isSlotReserved(int slot) {
        // Category buttons (top row)
        if (slot < 9) return true;
        // Item slots
        for (int itemSlot : ITEM_SLOTS) {
            if (slot == itemSlot) return true;
        }
        // Navigation and info slots (bottom row)
        return slot >= 45;
    }

    private void setupCategoryButtons() {
        int slot = 0;
        for (ShopCategory category : ShopCategory.values()) {
            ItemStack categoryButton = new ItemBuilder(category.getIcon())
                    .setName(CC.tf(category == currentCategory ? "&b&l%s" : "&7%s", category.getDisplayName()))
                    .setLore(Arrays.asList(
                            CC.t("&7"),
                            CC.t(category.getDescription()),
                            CC.t("&7"),
                            CC.t(category == currentCategory ? "&b➤ Selected" : "&eClick to view!")
                    ))
                    .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                    .build();

            if (category == currentCategory) {
                categoryButton = new ItemBuilder(categoryButton)
                        .addEnchantment(Enchantment.LUCK_OF_THE_SEA, 1, true)
                        .addFlag(ItemFlag.HIDE_ENCHANTS)
                        .build();
            }

            getInventory().setItem(slot++, categoryButton);
        }
    }

    private void displayCurrentCategoryItems() {
        List<BlockItem> items = categoryItems.get(currentCategory);
        int startIndex = (currentPage - 1) * ITEM_SLOTS.length;
        int endIndex = Math.min(startIndex + ITEM_SLOTS.length, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            BlockItem item = items.get(i);
            getInventory().setItem(ITEM_SLOTS[i - startIndex], createShopItem(item));
        }
    }

    private ItemStack createShopItem(BlockItem item) {
        ItemBuilder builder = new ItemBuilder(item.getItem().getType())
                .setName(CC.tf("&b%s", formatName(item.getItem().getType().name())))
                .addFlag(ItemFlag.HIDE_ATTRIBUTES);

        List<String> lore = new ArrayList<>();
        lore.add(CC.t("&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯"));
        lore.add(CC.tf("&7Category: &b%s", item.getCategory()));
        lore.add(CC.t("&7"));
        lore.add(CC.tf("&6✦ Price: &e%d coins", item.getPrice()));
        lore.add(CC.t("&7"));

        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            lore.add(CC.tf("&7%s", item.getDescription()));
            lore.add(CC.t("&7"));
        }

        if (player.getUnlockedBlocks().contains(item)) {
            builder.addEnchantment(Enchantment.LUCK_OF_THE_SEA, 1, true);
            builder.addFlag(ItemFlag.HIDE_ENCHANTS);
            lore.add(CC.t("&a✔ Unlocked"));

            if (player.getSelectedBlockItem() != null &&
                    player.getSelectedBlockItem().equals(item)) {
                lore.add(CC.t("&b➤ Currently Selected"));
            } else {
                lore.add(CC.t("&eClick to select!"));
            }
        } else {
            if (player.canAfford(item)) {
                lore.add(CC.t("&e⚡ Click to purchase!"));
            } else {
                lore.add(CC.t("&c✘ Locked"));
                lore.add(CC.tf("&c Need &e%d &cmore coins",
                        item.getPrice() - player.getCoins()));
            }
        }

        lore.add(CC.t("&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯"));
        builder.setLore(lore);
        return builder.build();
    }

    private void setupNavigationButtons() {
        List<BlockItem> items = categoryItems.get(currentCategory);
        int maxPages = (int) Math.ceil(items.size() / (double) ITEM_SLOTS.length);

        // Previous page button
        if (currentPage > 1) {
            getInventory().setItem(45, new ItemBuilder(Material.ARROW)
                    .setName(CC.t("&b← Previous Page"))
                    .setLore(Arrays.asList(
                            CC.t("&7"),
                            CC.tf("&7Page &b%d&7/&b%d", currentPage, maxPages)
                    ))
                    .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }

        // Next page button
        if (currentPage < maxPages) {
            getInventory().setItem(53, new ItemBuilder(Material.ARROW)
                    .setName(CC.t("&b→ Next Page"))
                    .setLore(Arrays.asList(
                            CC.t("&7"),
                            CC.tf("&7Page &b%d&7/&b%d", currentPage, maxPages)
                    ))
                    .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }
    }

    private void displayPlayerInfo() {
        getInventory().setItem(49, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(CC.tf("&b%s's Profile", player.getPlayer().getName()))
                .setLore(Arrays.asList(
                        CC.t("&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯"),
                        CC.tf("&7Balance: &e%d coins", player.getCoins()),
                        CC.tf("&7Unlocked Blocks: &b%d", player.getUnlockedBlocks().size()),
                        CC.t("&7"),
                        CC.t("&7Browse through categories"),
                        CC.t("&7to find your perfect blocks!"),
                        CC.t("&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯")
                ))
                .build());
    }

    private String formatName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(name);
    }

    @Override
    public void clickHandler(ItemStack item, int slot) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        Player p = player.getPlayer();

        // Category selection
        if (slot < 9) {
            ShopCategory category = ShopCategory.values()[slot];
            if (category != currentCategory) {
                currentCategory = category;
                currentPage = 1;
                p.playSound(p.getLocation(), SELECT_SOUND, 1.0F, 1.2F);
                updateInventory();
            }
            return;
        }

        // Navigation buttons
        if (slot == 45 && currentPage > 1) {
            currentPage--;
            p.playSound(p.getLocation(), NAVIGATE_SOUND, 1.0F, 1.0F);
            updateInventory();
            return;
        }

        if (slot == 53 && currentPage < Math.ceil(categoryItems.get(currentCategory).size() /
                (double) ITEM_SLOTS.length)) {
            currentPage++;
            p.playSound(p.getLocation(), NAVIGATE_SOUND, 1.0F, 1.0F);
            updateInventory();
            return;
        }

        // Block interaction
        BlockItem blockItem = plugin.getSM().getBlockItemsManager()
                .getBlockItem(item.getType());

        if (blockItem == null) return;

        if (player.getUnlockedBlocks().contains(blockItem)) {
            handleBlockSelection(blockItem);
        } else {
            handleBlockPurchase(blockItem);
        }
    }

    private void handleBlockSelection(BlockItem blockItem) {
        player.setSelectedBlockItem(blockItem);
        Player p = player.getPlayer();
        p.playSound(p.getLocation(), SELECT_SOUND, 1.0F, 2.0F);
        p.sendMessage(CC.tf("&8[&b⚡&8] &aSelected &b%s&a!",
                formatName(blockItem.getItem().getType().name())));
        updateInventory();
    }

    private void handleBlockPurchase(BlockItem blockItem) {
        Player p = player.getPlayer();
        if (!player.canAfford(blockItem)) {
            p.playSound(p.getLocation(), ERROR_SOUND, 1.0F, 0.5F);
            p.sendMessage(CC.tf("&8[&c⚡&8] &cYou need &e%d &cmore coins!",
                    blockItem.getPrice() - player.getCoins()));
            return;
        }

        player.getUnlockedBlocks().add(blockItem);
        player.setCoins(player.getCoins() - blockItem.getPrice());
        p.playSound(p.getLocation(), PURCHASE_SOUND, 1.0F, 1.0F);
        p.sendMessage(CC.tf("&8[&b⚡&8] &aUnlocked &b%s &afor &e%d coins&a!",
                formatName(blockItem.getItem().getType().name()),
                blockItem.getPrice()));

        // Automatically select the newly purchased block
        player.setSelectedBlockItem(blockItem);
        p.sendMessage(CC.tf("&8[&b⚡&8] &aAutomatically selected &b%s&a!",
                formatName(blockItem.getItem().getType().name())));

        updateInventory();
    }

    @Override
    public void onClose() {
        // Save player data if needed
        Player p = player.getPlayer();
        p.playSound(p.getLocation(), NAVIGATE_SOUND, 0.5F, 0.5F);
    }
}