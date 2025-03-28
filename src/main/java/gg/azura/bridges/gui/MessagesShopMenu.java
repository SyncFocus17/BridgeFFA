package gg.azura.bridges.gui;

import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.DeathMessage;
import gg.azura.bridges.utils.CC;
import gg.azura.bridges.utils.GuiMenu;
import gg.azura.bridges.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class MessagesShopMenu extends GuiMenu {

    /**
     * Handles inventory click events with modern interaction tracking
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:56:44 UTC
     * @param clickedItem The clicked ItemStack
     * @param slot The clicked inventory slot
     */
    @Override
    public void clickHandler(ItemStack clickedItem, int slot) {
        // Track click analytics
        final long clickTimestamp = System.currentTimeMillis();
        final String timeFormatted = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        // Validate click
        if (clickedItem == null ||
                clickedItem.getType() == Material.AIR ||
                clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            logInteraction("Invalid click", slot, timeFormatted);
            return;
        }

        // Handle navigation clicks
        handleNavigationClick(slot).ifPresent(page -> {
            currentPage = page;
            player.playSound(Sound.UI_BUTTON_CLICK);
            loadCurrentPage();
            logInteraction("Page navigation", slot, timeFormatted);
            return;
        });

        // Process item selection
        Optional.ofNullable(clickedItem.getItemMeta())
                .map(ItemMeta::getDisplayName)
                .map(this::extractMessageId)
                .ifPresent(id -> {
                    handleMessageSelection(id, clickedItem);
                    logInteraction("Message selection", slot, timeFormatted);
                });

        // Update interaction metrics
        updateMetrics(clickedItem, slot, clickTimestamp);
    }

    /**
     * Handles message selection with modern tracking
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:57:41 UTC
     */
    private void handleMessageSelection(Integer id, ItemStack clickedItem) {
        DeathMessage message = plugin.getSM().getDeathMessagesManager().getDeathMessage(id);

        if (player.getUnlockedDeathMessages().contains(message)) {
            selectMessage(message);
            logMessageAction("select", id);
        } else if (player.canAfford(message)) {
            unlockMessage(message, clickedItem);
            logMessageAction("unlock", id);
        } else {
            showInsufficientFundsMessage();
            logMessageAction("insufficient_funds", id);
        }
    }

    /**
     * Displays insufficient funds message
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:58:15 UTC
     */
    private void showInsufficientFundsMessage() {
        player.getPlayer().sendMessage(CC.t("&c⚠ You do not have enough coins!"));
        player.playSound(Sound.BLOCK_ANVIL_PLACE);
        setCloseOnClick(true);

        plugin.getLogger().info(String.format(
                "[FUNDS] %s | Balance: %d | Time: %s",
                "SyncFocus17",
                player.getCoins(),
                "2025-02-04 10:58:15"
        ));
    }

    /**
     * Unlocks a message for the player
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:58:15 UTC
     */
    private void unlockMessage(DeathMessage message, ItemStack clickedItem) {
        player.getUnlockedDeathMessages().add(message);
        player.setCoins(player.getCoins() - message.getPrice());

        player.getPlayer().sendMessage(CC.tf(
                "&a✓ Unlocked &b%s &afor &e%d coins",
                message.getID(),
                message.getPrice()
        ));

        player.playSound(Sound.ENTITY_PLAYER_LEVELUP);
        updateItemMeta(clickedItem, message);

        plugin.getLogger().info(String.format(
                "[UNLOCK] %s | Message: %d | Cost: %d | Time: %s",
                "SyncFocus17",
                message.getID(),
                message.getPrice(),
                "2025-02-04 10:58:15"
        ));
    }

    /**
     * Updates item metadata after purchase/selection
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:59:52 UTC
     */
    private void updateItemMeta(ItemStack clickedItem, DeathMessage message) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        // Update lore with ownership status
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.replaceAll(line ->
                line.contains("Price") ? CC.t("&a✓ Owned") : line
        );
        meta.setLore(lore);

        // Add purchase metadata
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "purchased_at"),
                PersistentDataType.STRING,
                "2025-02-04 10:59:52"
        );

        clickedItem.setItemMeta(meta);
        plugin.getLogger().info(String.format(
                "[META] %s | Message: %d | Time: %s",
                "SyncFocus17",
                message.getID(),
                "2025-02-04 10:59:52"
        ));
    }

    /**
     * Selects a message for the player
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:58:15 UTC
     */
    private void selectMessage(DeathMessage message) {
        player.setSelectedDeathMessage(message);

        player.getPlayer().sendMessage(CC.tf(
                "&a✓ Selected message &b%s",
                message.getID()
        ));

        player.playSound(Sound.BLOCK_NOTE_BLOCK_PLING);
        setCloseOnClick(true);

        plugin.getLogger().info(String.format(
                "[SELECT] %s | Message: %d | Time: %s",
                "SyncFocus17",
                message.getID(),
                "2025-02-04 10:58:15"
        ));
    }
    /**
     * Handles navigation click events
     *
     * @author SyncFocus17
     * @created 2025-02-04 10:57:41 UTC
     */
    private Optional<Integer> handleNavigationClick(int slot) {
        if (slot == 45 && currentPage > 1) {
            logNavigationAction("previous", currentPage - 1);
            return Optional.of(currentPage - 1);
        }
        if (slot == 53 && currentPage < pages.size()) {
            logNavigationAction("next", currentPage + 1);
            return Optional.of(currentPage + 1);
        }
        return Optional.empty();
    }

    /**
     * Logs message actions
     */
    private void logMessageAction(String action, Integer id) {
        plugin.getLogger().info(String.format(
                "[MESSAGE] %s | Action: %s | ID: %d | Time: %s",
                "SyncFocus17",
                action,
                id,
                "2025-02-04 10:57:41"
        ));
    }

    /**
     * Logs navigation actions
     */
    private void logNavigationAction(String direction, int page) {
        plugin.getLogger().info(String.format(
                "[NAVIGATION] %s | Direction: %s | Page: %d | Time: %s",
                "SyncFocus17",
                direction,
                page,
                "2025-02-04 10:57:41"
        ));
    }

    /**
     * Logs interaction details
     */
    private void logInteraction(String action, int slot, String timestamp) {
        plugin.getLogger().info(String.format(
                "[INTERACTION] %s | User: %s | Action: %s | Slot: %d | Time: %s",
                UUID.randomUUID().toString().substring(0, 8),
                "SyncFocus17",
                action,
                slot,
                timestamp
        ));
    }

    /**
     * Updates interaction metrics
     */
    private void updateMetrics(ItemStack item, int slot, long timestamp) {
        CompletableFuture.runAsync(() -> {
            try {
                // Store interaction data
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("user", "SyncFocus17");
                metrics.put("timestamp", timestamp);
                metrics.put("slot", slot);
                metrics.put("item_type", item.getType().name());
                metrics.put("page", currentPage);
            } catch (Exception e) {
                plugin.getLogger().severe(String.format(
                        "Failed to update metrics: %s",
                        e.getMessage()
                ));
            }
        });
    }

    /**
     * Creates a unique interaction ID
     */
    private String generateInteractionId() {
        return String.format(
                "INT-%s-%s",
                UUID.randomUUID().toString().substring(0, 6),
                System.currentTimeMillis()
        );
    }

    /**
     * Extracts message ID from display name
     */
    private int extractMessageId(String name) {
        try {
            return Integer.parseInt(
                    name.substring(1, 3)
            );
        } catch (Exception e) {
            plugin.getLogger().warning(String.format(
                    "Failed to extract message ID from: %s",
                    name
            ));
            return -1;
        }
    }

    private record PageData(int number, List<ItemStack> items) {
        @Override
        public String toString() {
            return String.format("📚 Page %d [%d items]", number, items.size());
        }
    }

    // Core Components
    private final BridgePlayer player;
    private final Map<Integer, PageData> pages;
    private int currentPage;
    private final Map<String, Integer> categoryStats = new ConcurrentHashMap<>();

    // GUI Constants
    private static final int INVENTORY_SIZE = 54; // 6 rows
    private static final int ITEMS_PER_PAGE = 27;

    // Modern Layout Configuration
    private static final Set<Integer> ITEM_SLOTS = Set.of(
            10, 11, 12, 13, 14, 15, 16, // Row 1 ⭐
            19, 20, 21, 22, 23, 24, 25, // Row 2 ⭐
            28, 29, 30, 31, 32, 33, 34, // Row 3 ⭐
            37, 38, 39, 40, 41, 42      // Row 4 ⭐
    );

    // Category Styling
    private static final Map<String, String> CATEGORY_EMOJIS = Map.of(
            "humorous", "😄",
            "epic", "⚔️",
            "battle", "🗡️",
            "mythical", "✨",
            "victory", "🏆",
            "brutal", "💀",
            "fantasy", "🔮",
            "power", "⚡",
            "destruction", "💥"
    );

    // Material Mappings with Modern Icons
    private static final Map<String, Material> CATEGORY_MATERIALS = Map.of(
            "humorous", Material.PLAYER_HEAD,      // 😄 Personal touch
            "epic", Material.NETHERITE_SWORD,      // ⚔️ Premium weapon
            "battle", Material.IRON_SWORD,         // 🗡️ Classic combat
            "mythical", Material.ENCHANTED_BOOK,   // ✨ Magical element
            "victory", Material.GOLDEN_APPLE,      // 🏆 Achievement symbol
            "brutal", Material.ANVIL,              // 💀 Heavy impact
            "fantasy", Material.DRAGON_EGG,        // 🔮 Mystical item
            "power", Material.NETHER_STAR,         // ⚡ Energy source
            "destruction", Material.TNT            // 💥 Explosive finish
    );

    // Sound Effects for Modern Experience
    private static final Map<String, Sound> CATEGORY_SOUNDS = Map.of(
            "humorous", Sound.ENTITY_VILLAGER_CELEBRATE,
            "epic", Sound.ENTITY_ENDER_DRAGON_GROWL,
            "battle", Sound.ITEM_TRIDENT_THUNDER,
            "mythical", Sound.BLOCK_ENCHANTMENT_TABLE_USE,
            "victory", Sound.UI_TOAST_CHALLENGE_COMPLETE,
            "brutal", Sound.ENTITY_WITHER_SPAWN,
            "fantasy", Sound.ENTITY_ILLUSIONER_MIRROR_MOVE,
            "power", Sound.BLOCK_BEACON_ACTIVATE,
            "destruction", Sound.ENTITY_GENERIC_EXPLODE
    );

    /**
     * Creates a new modern shop menu instance
     * @param player The bridge player viewing the shop
     */
    public MessagesShopMenu(BridgePlayer player) {
        super(player.getPlayer(), CC.tf("&9&l🎯 Kill Messages Shop"), INVENTORY_SIZE);
        this.player = player;
        this.pages = initializePages();
        this.currentPage = 1;
        loadCurrentPage();
        startAnimationTask();
    }

    /**
     * Initializes the shop pages with modern styling
     * @return Mapped pages with items
     */
    private Map<Integer, PageData> initializePages() {
        List<DeathMessage> messages = new ArrayList<>(plugin.getSM().getDeathMessagesManager().getDeathMessages());
        Map<Integer, PageData> pageMap = new HashMap<>();

        IntStream.range(0, (messages.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE)
                .forEach(pageNum -> {
                    int start = pageNum * ITEMS_PER_PAGE;
                    int end = Math.min(start + ITEMS_PER_PAGE, messages.size());
                    List<ItemStack> pageItems = messages.subList(start, end).stream()
                            .map(this::createDeathMessageItem)
                            .collect(Collectors.toList());
                    pageMap.put(pageNum + 1, new PageData(pageNum + 1, pageItems));
                });

        return pageMap;
    }

    /**
     * Creates a modernized item representation of a death message
     */
    private ItemStack createDeathMessageItem(DeathMessage message) {
        boolean isUnlocked = player.getUnlockedDeathMessages().contains(message);
        String emoji = CATEGORY_EMOJIS.getOrDefault(message.getCategory().toLowerCase(), "📜");

        return new ItemBuilder(getMaterialForCategory(message.getCategory()))
                .setName(CC.tf("&b#%02d &e%s %s", message.getID(), message.getCategory(), emoji))
                .setLore(createMessageLore(message, isUnlocked))
                .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .applyIf(isUnlocked, builder ->
                        builder.addEnchantment(Enchantment.LUCK_OF_THE_SEA, 1, true)
                                .addGlow())
                .build();
    }
    /**
     * Creates modern lore with emojis and styling
     */
    private List<String> createMessageLore(DeathMessage message, boolean isUnlocked) {
        List<String> lore = new ArrayList<>(Arrays.asList(
                "",
                CC.tf("&6✉️ Message: &r%s", CC.t(message.getUnformattedMessage())),
                "",
                CC.tf("&6💰 Price: &e%d coins", message.getPrice()),
                "",
                CC.tf("&7🏷️ Category: &e%s %s",
                        message.getCategory(),
                        CATEGORY_EMOJIS.getOrDefault(message.getCategory().toLowerCase(), "📜")),
                CC.tf("&7📊 Popularity: &e%s", getRarityDisplay(message.getPopularity())),
                ""
        ));

        if (isUnlocked) {
            lore.add(CC.tf("&a✅ &7You own this message"));
            lore.add(CC.tf("&7&o➜ Click to select"));
            lore.add("");
        } else {
            lore.add(CC.tf("&c❌ &7Not unlocked"));
            lore.add(CC.tf("&7&o➜ Click to purchase"));
            lore.add("");
        }

        return lore;
    }

    /**
     * Generates a visual representation of rarity
     */
    private String getRarityDisplay(String popularity) {
        return switch (popularity.toLowerCase()) {
            case "common" -> "⚪ Common";
            case "uncommon" -> "🟢 Uncommon";
            case "rare" -> "🔵 Rare";
            case "epic" -> "🟣 Epic";
            case "legendary" -> "🟡 Legendary";
            default -> "⚪ " + popularity;
        };
    }

    // [Previous methods remain the same but with added emojis and modern styling]

    /**
     * Handles animations and dynamic updates
     */
    private void startAnimationTask() {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                tick++;
                if (tick % 20 == 0) { // Every second
                    updateCoinsDisplay(); // Animate coins display
                }
                if (tick % 40 == 0) { // Every 2 seconds
                    animateBorder(); // Animate border
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void animateBorder() {
        IntStream.range(0, INVENTORY_SIZE)
                .filter(i -> !ITEM_SLOTS.contains(i))
                .forEach(i -> {
                    if (new Random().nextInt(100) < 15) { // 15% chance to change color
                        getInventory().setItem(i,
                                new ItemBuilder(getRandomGlass())
                                        .setName(" ")
                                        .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                                        .build());
                    }
                });
    }

    /**
     * Gets a random colored glass for animations
     */
    private Material getRandomGlass() {
        Material[] glasses = {
                Material.BLACK_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE,
                Material.PURPLE_STAINED_GLASS_PANE
        };
        return glasses[new Random().nextInt(glasses.length)];
    }

    private void loadCurrentPage() {
        // Clear inventory first to prevent ghost items
        getInventory().clear();

        // Initialize border immediately without animation
        initializeStaticBorder();

        // Get current page data
        PageData currentPageData = pages.get(currentPage);
        if (currentPageData == null) return;

        // Load items synchronously to prevent visual bugs
        Iterator<Integer> slotIterator = ITEM_SLOTS.iterator();
        currentPageData.items().forEach(item -> {
            if (slotIterator.hasNext()) {
                int slot = slotIterator.next();
                getInventory().setItem(slot, enhanceItem(item));
            }
        });

        // Update navigation and display elements synchronously
        updateNavigationButtons();
        updateStaticCoinsDisplay();
        updateStaticPageIndicator();
    }

    private void updateStaticCoinsDisplay() {
        ItemStack coinDisplay = new ItemBuilder(Material.GOLD_INGOT)
                .setName(CC.tf("&6💰 Balance: &e%s coins",
                        NumberFormat.getInstance().format(player.getCoins())))
                .setLore(Arrays.asList(
                        "",
                        CC.t("&7Statistics:"),
                        CC.tf("&8└ &7Spent: &e%s coins",
                                NumberFormat.getInstance().format(player.getSpentCoins())),
                        CC.tf("&8└ &7Earned: &e%s coins",
                                NumberFormat.getInstance().format(player.getEarnedCoins())),
                        "",
                        CC.t("&eClick items to purchase!")
                ))
                .addFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();

        getInventory().setItem(49, coinDisplay);
    }

    // Replace animated page indicator with static indicator
    private void updateStaticPageIndicator() {
        ItemStack pageIndicator = new ItemBuilder(Material.COMPASS)
                .setName(CC.tf("&b📖 Page %d/%d", currentPage, pages.size()))
                .setLore(Arrays.asList(
                        "",
                        CC.tf("&7Items: &e%d&7-&e%d &7of &e%d",
                                ((currentPage - 1) * ITEMS_PER_PAGE) + 1,
                                Math.min(currentPage * ITEMS_PER_PAGE, getTotalItems()),
                                getTotalItems()),
                        "",
                        currentPage > 1 ? CC.t("&e← Previous Page") : "",
                        currentPage < pages.size() ? CC.t("&e→ Next Page") : ""
                ))
                .addFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();

        getInventory().setItem(4, pageIndicator);
    }


    // Replace animated border with static border
    private void initializeStaticBorder() {
        IntStream.range(0, INVENTORY_SIZE)
                .filter(i -> !ITEM_SLOTS.contains(i))
                .forEach(i -> getInventory().setItem(i,
                        createBorderItem(isCornerSlot(i))));
    }

    private ItemStack createBorderItem(boolean isCorner) {
        return new ItemBuilder(
                isCorner ? Material.PURPLE_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .addFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }


    private Material getMaterialForCategory(String category) {
        // Modern material mapping with validation
        final Map<String, MaterialData> CATEGORY_MATERIALS = Map.of(
                "humorous", new MaterialData(Material.PLAYER_HEAD, "😄", 0x55FFFF),
                "epic", new MaterialData(Material.NETHERITE_SWORD, "⚔️", 0xFF55FF),
                "battle", new MaterialData(Material.IRON_SWORD, "🗡️", 0xFFAA00),
                "mythical", new MaterialData(Material.ENCHANTED_BOOK, "✨", 0xAA00AA),
                "victory", new MaterialData(Material.GOLDEN_APPLE, "🏆", 0xFFFF55),
                "brutal", new MaterialData(Material.ANVIL, "💀", 0xFF5555),
                "fantasy", new MaterialData(Material.DRAGON_EGG, "🔮", 0xAA00AA),
                "power", new MaterialData(Material.NETHER_STAR, "⚡", 0xFFFF55),
                "destruction", new MaterialData(Material.TNT, "💥", 0xFF5555)
        );

        // Get category data with modern pattern matching
        return Optional.ofNullable(category)
                .map(String::toLowerCase)
                .map(CATEGORY_MATERIALS::get)
                .map(MaterialData::material)
                .orElseGet(() -> {
                    // Log unknown category
                    plugin.getLogger().warning(String.format(
                            "Unknown category '%s' requested by %s at %s",
                            category, player.getName(),
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    ));
                    return Material.PAPER; // Fallback material
                });
    }

    /**
     * Updates the coins display with modern animations and formatting
     */
    private void updateCoinsDisplay() {
        final int currentCoins = player.getCoins();
        final String formattedCoins = NumberFormat.getInstance().format(currentCoins);

        // Create animated coin display
        ItemStack coinDisplay = new ItemBuilder(Material.GOLD_INGOT)
                .setName(CC.tf("&6💰 Balance: &e%s coins", formattedCoins))
                .setLore(Arrays.asList(
                        "",
                        CC.t("&7Statistics:"),
                        CC.tf("&8└ &7Spent: &e%s coins",
                                NumberFormat.getInstance().format(player.getSpentCoins())),
                        CC.tf("&8└ &7Earned: &e%s coins",
                                NumberFormat.getInstance().format(player.getEarnedCoins())),
                        "",
                        CC.t("&7Shop Information:"),
                        CC.tf("&8└ &7Items Owned: &e%d/%d",
                                player.getUnlockedDeathMessages().size(),
                                plugin.getSM().getDeathMessagesManager().getDeathMessages().size()),
                        CC.tf("&8└ &7Current Page: &e%d/%d",
                                currentPage, pages.size()),
                        "",
                        CC.t("&eClick items to purchase!"),
                        CC.t("&eOr select owned messages to use them.")
                ))
                .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .addGlow()
                .build();

        // Animate coin display placement
        new BukkitRunnable() {
            private final int[] rotationSlots = {48, 49, 50};
            private int tick = 0;

            @Override
            public void run() {
                if (tick >= rotationSlots.length) {
                    getInventory().setItem(49, coinDisplay);
                    this.cancel();
                    return;
                }

                // Rotate display through slots
                getInventory().setItem(rotationSlots[tick], coinDisplay);
                if (tick > 0) {
                    getInventory().setItem(rotationSlots[tick - 1],
                            createGlassPane(ChatColor.GOLD));
                }

                // Play sound effect
                player.getPlayer().playSound(
                        player.getPlayer().getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_CHIME,
                        0.5f,
                        1.0f + (tick * 0.1f)
                );

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Helper record for material metadata
     */
    private record MaterialData(Material material, String emoji, int color) {
        @Override
        public String toString() {
            return String.format("%s %s", emoji, material.name());
        }
    }

    /**
     * Creates a decorated glass pane
     */
    private ItemStack createGlassPane(ChatColor color) {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(color + "")
                .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    /**
     * Updates the page indicator with modern styling and animations
     *
     * @author SyncFocus17
     * @version 2.1
     * @lastModified 2025-02-04 10:49:03 UTC
     */
    private void updatePageIndicator() {
        // Constants for visual styling
        final String[] PROGRESS_FRAMES = {"▰▱▱▱▱", "▰▰▱▱▱", "▰▰▰▱▱", "▰▰▰▰▱", "▰▰▰▰▰"};
        final int totalPages = pages.size();
        final double progress = (double) currentPage / totalPages;

        // Create progress display
        ItemStack pageIndicator = new ItemBuilder(Material.COMPASS)
                .setName(CC.tf("&b✧ Page Navigation ✧"))
                .setLore(Arrays.asList(
                        "",
                        CC.tf("&7Current Page: &e%d&7/&e%d", currentPage, totalPages),
                        CC.tf("&7Progress: &e%.1f%%", progress * 100),
                        "",
                        CC.tf("&8└ %s", PROGRESS_FRAMES[(int) (progress * (PROGRESS_FRAMES.length - 1))]),
                        "",
                        CC.tf("&7Items: &e%d&7-&e%d &7of &e%d",
                                ((currentPage - 1) * ITEMS_PER_PAGE) + 1,
                                Math.min(currentPage * ITEMS_PER_PAGE, getTotalItems()),
                                getTotalItems()),
                        "",
                        currentPage > 1 ? CC.t("&e← &7Click to go back") : "",
                        currentPage < totalPages ? CC.t("&e→ &7Click to go forward") : "",
                        ""
                ))
                .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .addGlow()
                .build();

        // Animate indicator placement
        new BukkitRunnable() {
            private float pitch = 0.5f;
            private boolean ascending = true;
            private int ticks = 0;
            private final int ANIMATION_DURATION = 10;

            @Override
            public void run() {
                if (ticks >= ANIMATION_DURATION) {
                    getInventory().setItem(4, pageIndicator);
                    this.cancel();
                    return;
                }

                // Update sound pitch
                if (ascending) {
                    pitch += 0.1f;
                    if (pitch >= 2.0f) ascending = false;
                } else {
                    pitch -= 0.1f;
                }

                // Play navigation sound
                player.getPlayer().playSound(
                        player.getPlayer().getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_BELL,
                        0.3f,
                        pitch
                );

                // Update indicator with animation frame
                getInventory().setItem(4, pageIndicator);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Updates navigation buttons with modern styling and effects
     *
     * @author SyncFocus17
     * @version 2.1
     * @lastModified 2025-02-04 10:49:03 UTC
     */
    private void updateNavigationButtons() {
        // Navigation button constants
        final int PREVIOUS_SLOT = 45;
        final int NEXT_SLOT = 53;

        CompletableFuture.runAsync(() -> {
            // Previous page button
            if (currentPage > 1) {
                ItemStack previousButton = new ItemBuilder(Material.ARROW)
                        .setName(CC.t("&a← Previous Page"))
                        .setLore(Arrays.asList(
                                "",
                                CC.tf("&7Click to go to page &e%d", currentPage - 1),
                                CC.tf("&7Items: &e%d&7-&e%d",
                                        ((currentPage - 2) * ITEMS_PER_PAGE) + 1,
                                        (currentPage - 1) * ITEMS_PER_PAGE),
                                ""
                        ))
                        .addFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .addPersistentData(plugin, "nav_type", PersistentDataType.STRING, "previous")
                        .build();

                animateNavigationButton(PREVIOUS_SLOT, previousButton, -1);
            }

            // Next page button
            if (currentPage < pages.size()) {
                ItemStack nextButton = new ItemBuilder(Material.ARROW)
                        .setName(CC.t("&aNext Page →"))
                        .setLore(Arrays.asList(
                                "",
                                CC.tf("&7Click to go to page &e%d", currentPage + 1),
                                CC.tf("&7Items: &e%d&7-&e%d",
                                        (currentPage * ITEMS_PER_PAGE) + 1,
                                        Math.min((currentPage + 1) * ITEMS_PER_PAGE, getTotalItems())),
                                ""
                        ))
                        .addFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .addPersistentData(plugin, "nav_type", PersistentDataType.STRING, "next")
                        .build();

                animateNavigationButton(NEXT_SLOT, nextButton, 1);
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe(String.format(
                    "Failed to update navigation buttons: %s",
                    throwable.getMessage()
            ));
            return null;
        });
    }

    /**
     * Animates a navigation button's appearance
     * @param slot The inventory slot
     * @param button The button ItemStack
     * @param direction Animation direction (-1 for left, 1 for right)
     */
    private void animateNavigationButton(int slot, ItemStack button, int direction) {
        new BukkitRunnable() {
            private int frame = 0;
            private final String[] ANIMATION_FRAMES = {"⬆", "↗", "→", "↘", "⬇", "↙", "←", "↖"};

            @Override
            public void run() {
                if (frame >= ANIMATION_FRAMES.length) {
                    getInventory().setItem(slot, button);
                    this.cancel();
                    return;
                }

                ItemStack animatedButton = button.clone();
                ItemMeta meta = animatedButton.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(CC.tf("&a%s %s",
                            direction < 0 ? "←" : "→",
                            ANIMATION_FRAMES[frame]
                    ));
                    animatedButton.setItemMeta(meta);
                }

                getInventory().setItem(slot, animatedButton);
                frame++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Gets the total number of items across all pages
     * @return Total item count
     */
    private int getTotalItems() {
        return pages.values().stream()
                .mapToInt(page -> page.items().size())
                .sum();
    }

    /**
     * Enhances an ItemStack with modern visual effects and metadata
     *
     * @author SyncFocus17
     * @version 2.1
     * @lastModified 2025-02-04 10:51:03 UTC
     *
     * @param item The ItemStack to enhance
     * @return Enhanced ItemStack with modern effects
     */
    private ItemStack enhanceItem(ItemStack item) {
        if (item == null) return null;

        // Enhancement timestamp for tracking
        final long enhancementTime = System.currentTimeMillis();
        final String category = extractCategory(item);

        return new ItemBuilder(item)
                .modifyMeta(meta -> {
                    // Add modern visual enhancements
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                    // Add category-specific styling
                    String emoji = CATEGORY_EMOJIS.getOrDefault(
                            category.toLowerCase(),
                            "📦"
                    );

                    // Update name with modern formatting
                    if (meta.hasDisplayName()) {
                        meta.setDisplayName(CC.tf("%s %s",
                                meta.getDisplayName(),
                                emoji
                        ));
                    }

                    // Add enhanced lore
                    if (lore != null) {
                        lore.add("");
                        lore.add(CC.tf("&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯"));
                        lore.add(CC.tf("&7🏷️ Category: &e%s", category));
                        lore.add(CC.tf("&7🎨 Rarity: %s", getRarityFormat(category)));
                        lore.add(CC.tf("&7📅 Updated: &e%s",
                                LocalDateTime.now().format(
                                        DateTimeFormatter.ofPattern("MM/dd/yy HH:mm")
                                )
                        ));
                        lore.add(CC.tf("&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯"));
                        meta.setLore(lore);
                    }

                    // Add modern item flags
                    Arrays.asList(
                            ItemFlag.HIDE_ATTRIBUTES,
                            ItemFlag.HIDE_ENCHANTS,
                            ItemFlag.HIDE_UNBREAKABLE
                    ).forEach(meta::addItemFlags);

                    // Add enhancement metadata
                    addEnhancementData(meta, enhancementTime);
                })
                .addPersistentData(plugin, "enhanced_by",
                        PersistentDataType.STRING, "SyncFocus17")
                .addPersistentData(plugin, "enhanced_time",
                        PersistentDataType.LONG, enhancementTime)
                .build();
    }

    /**
     * Extracts category information from an ItemStack
     *
     * @author SyncFocus17
     * @version 2.1
     * @lastModified 2025-02-04 10:52:19 UTC
     *
     * @param item The ItemStack to extract category from
     * @return The extracted category or "unknown" if not found
     */
    private String extractCategory(ItemStack item) {
        return Optional.ofNullable(item)
                .map(ItemStack::getItemMeta)
                .map(ItemMeta::getDisplayName)
                .map(name -> {
                    // Extract category from modern format "#XX Category"
                    String[] parts = name.split(" ");
                    return parts.length >= 2 ? parts[1] : "unknown";
                })
                .map(category -> {
                    // Log category extraction for analytics
                    plugin.getLogger().fine(String.format(
                            "[%s] Category extracted: %s",
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                            category
                    ));
                    return category;
                })
                .orElse("unknown");
    }

    /**
     * Clears and decorates the inventory with modern styling
     *
     * @author SyncFocus17
     * @version 2.1
     * @lastModified 2025-02-04 10:51:03 UTC
     */
    private void clearAndDecorate() {
        // Clear existing inventory
        getInventory().clear();

        // Modern border pattern
        final String[] BORDER_PATTERN = {
                "▛▀▀▀▀▀▀▀▜",
                "█       █",
                "█       █",
                "█       █",
                "▙▄▄▄▄▄▄▄▟"
        };

        // Animation frames for border
        final String[] ANIMATION_FRAMES = {"◈", "◇", "◆", "◇"};

        // Decoration colors
        final ChatColor[] COLORS = {
                ChatColor.BLUE,
                ChatColor.AQUA,
                ChatColor.DARK_AQUA
        };

        new BukkitRunnable() {
            private int animationTick = 0;
            private final Set<Integer> borderSlots = new HashSet<>();

            @Override
            public void run() {
                if (animationTick >= 20) { // 1 second animation
                    this.cancel();
                    return;
                }

                // Calculate current animation frame
                int frameIndex = (animationTick / 5) % ANIMATION_FRAMES.length;
                ChatColor color = COLORS[(animationTick / 7) % COLORS.length];

                // Decorate border slots
                IntStream.range(0, INVENTORY_SIZE)
                        .filter(i -> !ITEM_SLOTS.contains(i))
                        .forEach(slot -> {
                            borderSlots.add(slot);

                            ItemStack decoration = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                                    .setName(color + ANIMATION_FRAMES[frameIndex])
                                    .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                                    .addPersistentData(plugin, "decoration_type",
                                            PersistentDataType.STRING, "border")
                                    .addPersistentData(plugin, "animation_frame",
                                            PersistentDataType.INTEGER, frameIndex)
                                    .build();

                            // Add special corner decorations
                            if (isCornerSlot(slot)) {
                                decoration = createCornerDecoration(slot, color);
                            }

                            getInventory().setItem(slot, decoration);
                        });

                // Play ambient sound
                if (animationTick % 5 == 0) {
                    player.getPlayer().playSound(
                            player.getPlayer().getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_HARP,
                            0.3f,
                            1.0f + (animationTick * 0.05f)
                    );
                }

                animationTick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Creates a special corner decoration
     * @param slot The corner slot
     * @param color The current color
     * @return Decorated ItemStack
     */
    private ItemStack createCornerDecoration(int slot, ChatColor color) {
        String cornerSymbol = switch (slot) {
            case 0 -> "◤";
            case 8 -> "◥";
            case 45 -> "◣";
            case 53 -> "◢";
            default -> "◆";
        };

        return new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
                .setName(color + cornerSymbol)
                .addFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .addGlow()
                .build();
    }

    /**
     * Checks if a slot is a corner slot
     * @param slot The slot to check
     * @return true if the slot is a corner
     */
    private boolean isCornerSlot(int slot) {
        return slot == 0 || slot == 8 || slot == 45 || slot == 53;
    }

    /**
     * Gets the rarity format for a category
     * @param category The category to format
     * @return Formatted rarity string
     */
    private String getRarityFormat(String category) {
        return switch (category.toLowerCase()) {
            case "mythical" -> "&d&l✦ MYTHICAL";
            case "epic" -> "&5&l☬ EPIC";
            case "battle" -> "&c&l⚔ BATTLE";
            case "victory" -> "&6&l◊ VICTORY";
            case "power" -> "&e&l❖ POWER";
            default -> "&7&l○ COMMON";
        };
    }

    /**
     * Adds enhancement metadata to an ItemMeta
     * @param meta The ItemMeta to modify
     * @param timestamp The enhancement timestamp
     */
    private void addEnhancementData(ItemMeta meta, long timestamp) {
        if (meta == null) return;

        NamespacedKey enhancedKey = new NamespacedKey(plugin, "enhanced");
        NamespacedKey timestampKey = new NamespacedKey(plugin, "enhance_time");

        meta.getPersistentDataContainer().set(
                enhancedKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.getPersistentDataContainer().set(
                timestampKey,
                PersistentDataType.LONG,
                timestamp
        );
    }

    @Override
    public void onClose() {
    }
}