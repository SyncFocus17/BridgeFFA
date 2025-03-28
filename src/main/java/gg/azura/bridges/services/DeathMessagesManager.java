package gg.azura.bridges.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.azura.bridges.Bridges;
import gg.azura.bridges.DeathMessage;
import gg.azura.bridges.utils.CC;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DeathMessagesManager {

    private final Bridges plugin;
    private List<DeathMessage> deathMessages;

    public DeathMessagesManager(Bridges plugin) {
        this.deathMessages = new ArrayList<>();
        this.plugin = plugin;
        load();
    }

    public List<DeathMessage> getDeathMessages() {
        return this.deathMessages;
    }

    public DeathMessage getDeathMessage(int id) {
        return this.deathMessages.stream().filter(dm -> dm.getID() == id).findAny().orElse(null);
    }

    private void load() {
        try {
            File file = new File(this.plugin.getDataFolder(), "deathmessages.json");
            if (!file.exists()) return;

            String jsonText = readAll(new FileReader(file));
            this.deathMessages = new ArrayList<>();
            JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();

            for (String idS : json.keySet()) {
                int id = Integer.parseInt(idS);
                JsonObject json2 = json.get(idS).getAsJsonObject();
                String messageWithKiller = json2.get("message_with_killer").getAsString();
                String messageWithoutKiller = json2.get("message_without_killer").getAsString();
                int price = json2.get("price").getAsInt();
                String category = json2.has("category") ? json2.get("category").getAsString() : "Default";
                String popularity = json2.has("popularity") ? json2.get("popularity").getAsString() : "Common";

                this.deathMessages.add(new DeathMessage(id, messageWithKiller, messageWithoutKiller, price, category, popularity));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            this.plugin.getServer().getLogger().log(Level.WARNING, "");
            this.plugin.getServer().getLogger().log(Level.WARNING, CC.t("&cUnable to parse death messages JSON. Read stacktrace above."));
            this.plugin.getServer().getLogger().log(Level.WARNING, "");
        }
    }

    public List<DeathMessage> fromDatabase(String deathMessages) {
        List<DeathMessage> list = new ArrayList<>();
        JsonArray json = JsonParser.parseString(deathMessages).getAsJsonArray();
        json.forEach(e -> list.add(getDeathMessage(e.getAsInt())));
        return list;
    }

    public JsonArray toDatabase(List<DeathMessage> unlockedDeathMessages) {
        JsonArray json = new JsonArray();
        unlockedDeathMessages.forEach(deathMessage -> json.add(deathMessage.getID()));
        return json;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}