package gg.azura.bridges;

import gg.azura.bridges.utils.CC;

public class DeathMessage implements Buyable {

    private final int id;
    private final String messageWithKiller;
    private final String messageWithoutKiller;
    private final int price;
    private final String category;
    private final String popularity;

    private String killer;
    private String victim;
    private double health;

    public DeathMessage(int id, String messageWithKiller, String messageWithoutKiller, int price, String category, String popularity) {
        this.id = id;
        this.messageWithKiller = messageWithKiller;
        this.messageWithoutKiller = messageWithoutKiller;
        this.price = price;
        this.category = category;
        this.popularity = popularity;
    }

    public int getID() {
        return id;
    }

    public String getUnformattedMessage() {
        return (killer != null) ? messageWithKiller : messageWithoutKiller;
    }

    public String getFormattedMessage() {
        String s = messageWithoutKiller;
        if (killer != null) {
            s = messageWithKiller;
            s = s.replace("{killer}", killer);
        }
        s = s.replace("{victim}", (victim != null) ? victim : "UnknownPlayer");
        s = s.replace("{health}", String.format("%.2f", health / 2.0));
        return CC.t(s);
    }

    public String getDeathMessage() {
        return getFormattedMessage();
    }

    public int getPrice() {
        return price;
    }

    public void setKiller(String killer) {
        this.killer = killer;
    }

    public void setVictim(String victim) {
        this.victim = victim;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public String getCategory() {
        return category;
    }

    public String getPopularity() {
        return popularity;
    }
}