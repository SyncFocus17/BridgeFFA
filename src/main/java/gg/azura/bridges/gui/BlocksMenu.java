package gg.azura.bridges.gui;

import gg.azura.bridges.BlockItem;
import gg.azura.bridges.BridgePlayer;
import gg.azura.bridges.utils.CC;
import gg.azura.bridges.utils.GuiMenu;
import gg.azura.bridges.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class BlocksMenu extends GuiMenu {

    private final BridgePlayer player;
    private ClickInteraction clickInteraction;
    private static final int[] ITEM_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private static final Sound SELECT_SOUND = Sound.UI_BUTTON_CLICK;
    private static final Sound ERROR_SOUND = Sound.BLOCK_NOTE_BLOCK_BASS;
    private int currentPage = 0;
    private List<List<BlockItem>> pages;

    public BlocksMenu(BridgePlayer player, int size) {
        super(player.getPlayer(), CC.t("&9&lBridgeFFA - Block Selector"), 54);
        this.player = player;
        initializeMenu();
    }

    private void initializeMenu() {
        setupPages();
        updateInventory();
    }

    private void setupPages() {
        List<BlockItem> unlockedBlocks = new ArrayList<>(player.getUnlockedBlocks());
        pages = new ArrayList<>();

        // Sort blocks by category and name
        unlockedBlocks.sort((b1, b2) -> {
            int categoryCompare = b1.getCategory().compareTo(b2.getCategory());
            if (categoryCompare == 0) {
                return b1.getItem().getType().name().compareTo(b2.getItem().getType().name());
            }
            return categoryCompare;
        });

        // Split into pages
        for (int i = 0; i < unlockedBlocks.size(); i += ITEM_SLOTS.length) {
            pages.add(unlockedBlocks.subList(i,
                    Math.min(i + ITEM_SLOTS.length, unlockedBlocks.size())));
        }
    }

    private void updateInventory() {
        getInventory().clear();
        decorateBackground();

        // Place blocks
        if (!pages.isEmpty()) {
            List<BlockItem> currentPageBlocks = pages.get(currentPage);
            for (int i = 0; i < currentPageBlocks.size(); i++) {
                getInventory().setItem(ITEM_SLOTS[i], createBlockItem(currentPageBlocks.get(i)));
            }
        }

        // Navigation items
        if (pages.size() > 1) {
            if (currentPage > 0) {
                getInventory().setItem(45, new ItemBuilder(Material.ARROW)
                        .setName(CC.t("&a← Previous Page"))
                        .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                        .build());
            }
            if (currentPage < pages.size() - 1) {
                getInventory().setItem(53, new ItemBuilder(Material.ARROW)
                        .setName(CC.t("&aNext Page →"))
                        .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                        .build());
            }
        }

        // Info item
        getInventory().setItem(49, new ItemBuilder(Material.NETHER_STAR)
                .setName(CC.t("&b&lBlock Information"))
                .setLore(Arrays.asList(
                        CC.t("&7You have &b" + player.getUnlockedBlocks().size() + " &7blocks unlocked"),
                        CC.t("&7Currently selected: &b" + (player.getSelectedBlockItem() != null ?
                                formatName(player.getSelectedBlockItem().getItem().getType().name()) : "None")),
                        "",
                        CC.t("&eClick on a block to select it!")
                ))
                .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    private void decorateBackground() {
        for (int i = 0; i < getInventory().getSize(); i++) {
            int finalI = i;
            if (!Arrays.stream(ITEM_SLOTS).anyMatch(slot -> slot == finalI)) {
                getInventory().setItem(i, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                        .setName(" ")
                        .addFlag(ItemFlag.HIDE_ATTRIBUTES)
                        .build());
            }
        }
    }

    private ItemStack createBlockItem(BlockItem blockItem) {
        ItemBuilder builder = new ItemBuilder(blockItem.getItem().clone())
                .setName(CC.tf("&b%s", formatName(blockItem.getItem().getType().name())))
                .addFlag(ItemFlag.HIDE_ATTRIBUTES);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(CC.tf("&7Category: &b%s", blockItem.getCategory()));

        if (blockItem.getDescription() != null && !blockItem.getDescription().isEmpty()) {
            lore.add("");
            lore.add(CC.tf("&7%s", blockItem.getDescription()));
        }

        lore.add("");
        if (player.getSelectedBlockItem() != null &&
                player.getSelectedBlockItem().equals(blockItem)) {
            builder.addEnchantment(Enchantment.EFFICIENCY, 1, true);
            builder.addFlag(ItemFlag.HIDE_ENCHANTS);
            lore.add(CC.t("&b➤ Currently Selected"));
        } else {
            lore.add(CC.t("&eClick to select!"));
        }

        builder.setLore(lore);
        return builder.build();
    }

    private String formatName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(name);
    }

    public void open(ClickInteraction interaction) {
        this.clickInteraction = interaction;
        open();
    }

    @Override
    public void clickHandler(ItemStack item, int slot) {
        if (item == null || item.getType() == Material.AIR ||
                item.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        // Handle navigation
        if (slot == 45 && currentPage > 0) {
            currentPage--;
            player.getPlayer().playSound(player.getPlayer().getLocation(),
                    SELECT_SOUND, 1.0F, 1.0F);
            updateInventory();
            return;
        }

        if (slot == 53 && currentPage < pages.size() - 1) {
            currentPage++;
            player.getPlayer().playSound(player.getPlayer().getLocation(),
                    SELECT_SOUND, 1.0F, 1.0F);
            updateInventory();
            return;
        }

        // Handle block selection
        if (clickInteraction != null) {
            clickInteraction.clickHandler(item, slot);
            updateInventory();
        }
    }

    @Override
    public void onClose() {
        player.getPlayer().playSound(player.getPlayer().getLocation(),
                Sound.BLOCK_CHEST_CLOSE, 1.0F, 1.0F);
    }

    public interface ClickInteraction {
        void clickHandler(ItemStack itemStack, int slot);
    }
}