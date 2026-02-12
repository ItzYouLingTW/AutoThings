package net.yotk.ylats.client.prepare_litematica;

import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Map;

public class AutoPrepareState {

    public static boolean ENABLED = false;

    // 剩餘需求: 物品 -> 剩餘數量
    public static final Map<Item, Integer> NEEDS = new HashMap<>();

    /**
     * 設置新的需求列表，會覆蓋原本的需求。
     */
    public static void setNeeds(Map<Item, Integer> newNeeds) {
        NEEDS.clear();
        if (newNeeds != null) {
            NEEDS.putAll(newNeeds);
        }
    }

    /**
     * 從需求中扣除指定數量。
     * 若剩餘數量 <= 0，則移除該物品。
     */
    public static void decrease(Item item, int amount) {
        if (item == null || amount <= 0) return;

        NEEDS.computeIfPresent(item, (key, current) -> {
            int updated = current - amount;
            return updated > 0 ? updated : null; // null 會自動移除
        });
    }

    /**
     * 清空所有需求。
     */
    public static void clear() {
        NEEDS.clear();
    }
}
