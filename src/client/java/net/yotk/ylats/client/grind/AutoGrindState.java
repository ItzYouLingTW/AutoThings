package net.yotk.ylats.client.grind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AutoGrindState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean ENABLED = false;

    // key = 附魔名稱字串
    public static final Map<String, Integer> KEEP_ENCHANTS = new HashMap<>();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("ylats")
            .resolve("grind_enchants.json");

    private AutoGrindState() {}

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) return;

        try {
            JsonObject root = GSON.fromJson(Files.readString(CONFIG_PATH), JsonObject.class);
            KEEP_ENCHANTS.clear();

            JsonArray array = root.getAsJsonArray("enchants");
            if (array != null) {
                array.forEach(el -> {
                    JsonObject obj = el.getAsJsonObject();
                    String name = obj.get("name").getAsString();
                    int minLevel = obj.get("min_level").getAsInt();
                    KEEP_ENCHANTS.put(name, minLevel);
                });
            }
        } catch (Exception e) {
            System.err.println("[YLATS] Failed to load grind_enchants.json");
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            JsonArray array = new JsonArray();
            for (Map.Entry<String, Integer> entry : KEEP_ENCHANTS.entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", entry.getKey());
                obj.addProperty("min_level", entry.getValue());
                array.add(obj);
            }

            JsonObject root = new JsonObject();
            root.add("enchants", array);

            Files.writeString(CONFIG_PATH, GSON.toJson(root));
        } catch (IOException e) {
            System.err.println("[YLATS] Failed to save grind_enchants.json");
            e.printStackTrace();
        }
    }
}
