package gg.azura.bridges.gui;

import org.bukkit.Material;

public enum ShopCategory {
    FEATURED("Featured", "Special curated blocks", Material.NETHER_STAR),
    PREMIUM("Premium", "Exclusive high-tier blocks", Material.DIAMOND),
    MODERN("Modern", "Contemporary building blocks", Material.SMOOTH_QUARTZ),
    NATURE("Nature", "Natural & organic blocks", Material.FLOWERING_AZALEA),
    TECH("Tech", "Futuristic & tech blocks", Material.SEA_LANTERN),
    DECORATIVE("Decorative", "Aesthetic blocks", Material.AMETHYST_BLOCK);

    private final String displayName;
    private final String description;
    private final Material icon;

    ShopCategory(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }
}