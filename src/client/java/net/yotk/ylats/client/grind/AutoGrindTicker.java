package net.yotk.ylats.client.grind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class AutoGrindTicker {

    private static int processedCount = 0;
    private static boolean isFirstTick = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!AutoGrindState.ENABLED || client.world == null) return;

            if (client.currentScreen instanceof GrindstoneScreen screen) {
                if(!isFirstTick) {
                    processedCount = 0;
                    doGrind(screen);
                }
            } else if(isFirstTick) isFirstTick = false;
        });
    }

    private static void doGrind(GrindstoneScreen screen) {
        isFirstTick = true;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        PlayerInventory inv = player.getInventory();
        int si = screen.getScreenHandler().syncId;

        // 背包
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = inv.getStack(i);

            // 1. 檢查是否有附魔
            if (stack.isEmpty() || !stack.hasEnchantments()) continue;

            // 2. 1.21 組件檢查：判斷是否需要保留
            ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants == null || enchants.isEmpty()) continue;

            boolean keep = false;
            for (var entry : enchants.getEnchantmentEntries()) {
                String enchId = entry.getKey().getKey()
                        .map(key -> key.getValue().toString())
                        .orElse("unknown");
                int level = entry.getIntValue();

                Integer minLevel = AutoGrindState.KEEP_ENCHANTS.get(enchId);
                if (minLevel != null && level >= minLevel) {
                    keep = true;
                    break;
                }
            }

            // 3. 如果不需要保留，執行無延遲原地洗除
            if (!keep) {
                client.interactionManager.clickSlot(si, i - 6, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(si, 0, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(si, 2, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(si, i - 6, 0, SlotActionType.PICKUP, player);

                processedCount++;
            }
        }

        if (processedCount > 0) {
            player.sendMessage(Text.literal("§a[AutoGrind] §f處理完畢，共計: §e" + processedCount + " §f個物品"), false);
            player.closeHandledScreen();
            isFirstTick = false;
            processedCount = 0;
        }
    }
}