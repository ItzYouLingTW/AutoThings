package net.yotk.ylats.client.take;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class AutoTakeState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean ENABLED = false;
    public static final Set<Item> TAKE_ITEMS = new HashSet<>();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("ylats")
            .resolve("take_items.json");

    private AutoTakeState() {}

    // ====== 存檔 ======

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) return;

        try {
            JsonObject root = GSON.fromJson(
                    Files.readString(CONFIG_PATH),
                    JsonObject.class
            );

            TAKE_ITEMS.clear();

            JsonArray items = root.getAsJsonArray("items");
            if (items == null) return;

            for (var el : items) {
                Identifier id = Identifier.tryParse(el.getAsString());
                if (id != null && Registries.ITEM.containsId(id)) {
                    TAKE_ITEMS.add(Registries.ITEM.get(id));
                }
            }
        } catch (Exception e) {
            System.err.println("[YLATS] Failed to load take_items.json");
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            JsonArray array = new JsonArray();
            for (Item item : TAKE_ITEMS) {
                array.add(Registries.ITEM.getId(item).toString());
            }

            JsonObject root = new JsonObject();
            root.add("items", array);

            Files.writeString(CONFIG_PATH, GSON.toJson(root));
        } catch (IOException e) {
            System.err.println("[YLATS] Failed to save take_items.json");
            e.printStackTrace();
        }
    }
}
