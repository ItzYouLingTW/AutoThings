package net.yotk.ylats.client.prepare_litematica;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.block.ShulkerBoxBlock;

public class AutoPrepareTicker {

    private static int cooldown = 0;

    public static void tick(MinecraftClient client) {

        if (!AutoPrepareState.ENABLED) return;
        if (client.currentScreen == null) return;
        if (client.player == null) return;

        if (!(client.player.currentScreenHandler != null)) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        process(client, client.player.currentScreenHandler);
    }

    private static void process(MinecraftClient client, ScreenHandler handler) {

        if (AutoPrepareState.NEEDS.isEmpty()) return;

        int containerSlots = handler.slots.size() - 36;
        if (containerSlots <= 0) return;

        for (int i = 0; i < containerSlots; i++) {

            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();

            Integer need = AutoPrepareState.NEEDS.get(item);
            if (need != null && need > 0) {
                quickMove(client, handler, i, stack.copy());
                return;
            }

            if (isShulker(stack) && shulkerContainsNeeded(stack)) {
                quickMove(client, handler, i, stack.copy());
                return;
            }
        }
    }

    private static void quickMove(MinecraftClient client,
                                  ScreenHandler handler,
                                  int slotIndex,
                                  ItemStack moved) {

        if (client.interactionManager == null) return;

        client.interactionManager.clickSlot(
                handler.syncId,
                slotIndex,
                0,
                SlotActionType.QUICK_MOVE,
                client.player
        );

        updateNeeds(moved);
        cooldown = 4; // 防止被反作弊判定
    }

    private static void updateNeeds(ItemStack stack) {

        if (isShulker(stack)) {

            ContainerComponent container =
                    stack.get(DataComponentTypes.CONTAINER);

            if (container == null) return;

            for (ItemStack inner : container.iterateNonEmpty()) {
                if (!inner.isEmpty()) {
                    AutoPrepareState.decrease(inner.getItem(), inner.getCount());
                }
            }

        } else {
            AutoPrepareState.decrease(stack.getItem(), stack.getCount());
        }
    }

    private static boolean shulkerContainsNeeded(ItemStack shulker) {

        ContainerComponent container =
                shulker.get(DataComponentTypes.CONTAINER);

        if (container == null) return false;

        for (ItemStack inner : container.iterateNonEmpty()) {

            if (inner.isEmpty()) continue;

            Integer need = AutoPrepareState.NEEDS.get(inner.getItem());
            if (need != null && need > 0) {
                return true;
            }
        }

        return false;
    }

    private static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi
                && bi.getBlock() instanceof ShulkerBoxBlock;
    }
}
