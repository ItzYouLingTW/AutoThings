package net.yotk.ylats.client.take;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class AutoLootTicker {

    private static int closeCountdown = -1;
    // 使用 Map 紀錄：物品類型 -> 領取總數
    private static final Map<Item, Integer> takenItemsSummary = new HashMap<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!AutoTakeState.ENABLED || client.world == null) return;

            if (client.currentScreen instanceof HandledScreen<?> screen) {
                ScreenHandler handler = screen.getScreenHandler();
                if (handler instanceof GenericContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler) {
                    processLooting(client, handler);
                }
            } else {
                // 意外關閉或手動關閉時清空統計
                takenItemsSummary.clear();
                closeCountdown = -1;
            }
        });
    }

    private static void processLooting(MinecraftClient client, ScreenHandler handler) {
        PlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        // 空間檢查
        if (player.getInventory().getEmptySlot() == -1 && !hasStackableSlot(player, handler)) {
            startOrTickClose(client);
            return;
        }

        int containerSize = handler.slots.size() - 36;
        int takenInThisTick = 0;
        boolean foundTarget = false;

        for (int i = 0; i < containerSize; i++) {
            Slot slot = handler.getSlot(i);
            ItemStack stack = slot.getStack();

            if (stack.isEmpty()) continue;

            if (AutoTakeState.TAKE_ITEMS.contains(stack.getItem())) {
                foundTarget = true;

                // 紀錄到統計 Map 中
                Item item = stack.getItem();
                takenItemsSummary.put(item, takenItemsSummary.getOrDefault(item, 0) + stack.getCount());

                // 執行取物
                client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, player);
                takenInThisTick++;

                if (player.getInventory().getEmptySlot() == -1) break;
                if (takenInThisTick >= 27) break;
            }
        }

        if (takenInThisTick == 0 && !foundTarget) {
            startOrTickClose(client);
        } else {
            closeCountdown = -1; // 還在拿取，重置倒數
        }
    }

    private static void startOrTickClose(MinecraftClient client) {
        if (closeCountdown < 0) {
            closeCountdown = 50; // 2.5 秒
            return;
        }

        closeCountdown--;

        if (closeCountdown <= 0) {
            if (client.player != null && !takenItemsSummary.isEmpty()) {
                // 輸出格式化訊息
                client.player.sendMessage(Text.literal("§7[AT系統] §a自動取物完成:"), false);

                takenItemsSummary.forEach((item, count) -> {
                    // 組合格式: - 64個 鑽石
                    MutableText line = Text.literal(" §7- §e" + count + "個")
                            .append(Text.translatable(item.getTranslationKey()).formatted(net.minecraft.util.Formatting.AQUA));
                    client.player.sendMessage(line, false);
                });
            }
            // 完成後徹底重置
            closeCountdown = -1;
            takenItemsSummary.clear();
            if (client.player != null) client.player.closeHandledScreen();
        }
    }

    private static boolean hasStackableSlot(PlayerEntity player, ScreenHandler handler) {
        int containerSize = handler.slots.size() - 36;
        for (int i = 0; i < containerSize; i++) {
            ItemStack containerStack = handler.getSlot(i).getStack();
            if (containerStack.isEmpty() || !AutoTakeState.TAKE_ITEMS.contains(containerStack.getItem())) continue;

            for (int j = 0; j < 36; j++) {
                ItemStack invStack = player.getInventory().getStack(j);
                if (!invStack.isEmpty() && invStack.getItem() == containerStack.getItem()
                        && invStack.getCount() < invStack.getMaxCount()) {
                    return true;
                }
            }
        }
        return false;
    }
}