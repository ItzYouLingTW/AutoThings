package net.yotk.ylats.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.yotk.ylats.client.grind.AutoGrindState;
import net.yotk.ylats.client.take.AutoTakeState;

public class AutoCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> dispatcher.register(
                        ClientCommandManager.literal("auto")
                                .then(toggleTakeItem())
                                .then(toggleGrindstone())
                                .then(ClientCommandManager.literal("set")
                                        .then(setTakeItemBranch(registryAccess))
                                        .then(setGrindEnchBranch(registryAccess))
                                )
                )
        );
    }

    // ------------------- 功能開關 (顏色修正) -------------------

    private static LiteralArgumentBuilder<FabricClientCommandSource> toggleTakeItem() {
        return ClientCommandManager.literal("takeitem")
                .executes(ctx -> {
                    AutoTakeState.ENABLED = !AutoTakeState.ENABLED;
                    MutableText status = AutoTakeState.ENABLED ?
                            Text.literal("開啟").formatted(Formatting.GREEN) :
                            Text.literal("關閉").formatted(Formatting.RED);

                    ctx.getSource().sendFeedback(Text.literal("[AT系統] ").formatted(Formatting.GRAY)
                            .append(Text.literal("自動取物: ").formatted(Formatting.WHITE))
                            .append(status));
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> toggleGrindstone() {
        return ClientCommandManager.literal("grind")
                .executes(ctx -> {
                    AutoGrindState.ENABLED = !AutoGrindState.ENABLED;
                    MutableText status = AutoGrindState.ENABLED ?
                            Text.literal("開啟").formatted(Formatting.GREEN) :
                            Text.literal("關閉").formatted(Formatting.RED);

                    ctx.getSource().sendFeedback(Text.literal("[AT系統] ").formatted(Formatting.GRAY)
                            .append(Text.literal("自動磨石: ").formatted(Formatting.WHITE))
                            .append(status));
                    return 1;
                });
    }

    // ------------------- 自動取物配置 (列表上色修正) -------------------

    private static ArgumentBuilder<FabricClientCommandSource, ?> setTakeItemBranch(CommandRegistryAccess registryAccess) {
        return ClientCommandManager.literal("takeitem")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                .executes(ctx -> {
                                    Item item = ItemStackArgumentType.getItemStackArgument(ctx, "item").getItem();
                                    if (AutoTakeState.TAKE_ITEMS.add(item)) {
                                        AutoTakeState.save();
                                        ctx.getSource().sendFeedback(Text.literal("成功新增取物項目: ").formatted(Formatting.GREEN)
                                                .append(Text.translatable(item.getTranslationKey()).formatted(Formatting.AQUA)));
                                    } else {
                                        ctx.getSource().sendFeedback(Text.literal("此物品已在取物清單中。").formatted(Formatting.YELLOW));
                                    }
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                .executes(ctx -> {
                                    Item item = ItemStackArgumentType.getItemStackArgument(ctx, "item").getItem();
                                    if (AutoTakeState.TAKE_ITEMS.remove(item)) {
                                        AutoTakeState.save();
                                        ctx.getSource().sendFeedback(Text.literal("已移除取物項目: ").formatted(Formatting.RED)
                                                .append(Text.translatable(item.getTranslationKey()).formatted(Formatting.AQUA)));
                                    } else {
                                        ctx.getSource().sendFeedback(Text.literal("取物清單中找不到該物品。").formatted(Formatting.YELLOW));
                                    }
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("--- 自動取物清單 ---").formatted(Formatting.GOLD));
                            if (AutoTakeState.TAKE_ITEMS.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal(" (清單為空)").formatted(Formatting.GRAY));
                            } else {
                                AutoTakeState.TAKE_ITEMS.forEach(i -> {
                                    ctx.getSource().sendFeedback(Text.literal(" - ").formatted(Formatting.GRAY)
                                            .append(Text.translatable(i.getTranslationKey()).formatted(Formatting.AQUA)));
                                });
                            }
                            return 1;
                        }));
    }

    // ------------------- 自動磨石配置 (括號上色修正) -------------------

    private static ArgumentBuilder<FabricClientCommandSource, ?> setGrindEnchBranch(CommandRegistryAccess registryAccess) {
        return ClientCommandManager.literal("grind")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("enchantment", IdentifierArgumentType.identifier())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    registryAccess.getOrThrow(RegistryKeys.ENCHANTMENT)
                                            .streamKeys()
                                            .map(key -> key.getValue())
                                            .filter(id -> id.toString().startsWith(remaining) || id.getPath().startsWith(remaining))
                                            .forEach(id -> builder.suggest(id.toString()));
                                    return builder.buildFuture();
                                })
                                .then(ClientCommandManager.argument("minLevel", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            Identifier id = ctx.getArgument("enchantment", Identifier.class);
                                            String enchId = id.toString();
                                            int level = IntegerArgumentType.getInteger(ctx, "minLevel");

                                            AutoGrindState.KEEP_ENCHANTS.put(enchId, level);
                                            AutoGrindState.save();

                                            // 格式：已設定保留附魔: ID (≥ 等級)
                                            MutableText feedback = Text.literal("已設定保留附魔: ").formatted(Formatting.GREEN)
                                                    .append(Text.literal(enchId).formatted(Formatting.AQUA))
                                                    .append(Text.literal(" (").formatted(Formatting.WHITE))
                                                    .append(Text.literal("≥ ").formatted(Formatting.WHITE))
                                                    .append(Text.literal(String.valueOf(level)).formatted(Formatting.YELLOW))
                                                    .append(Text.literal(")").formatted(Formatting.WHITE));

                                            ctx.getSource().sendFeedback(feedback);
                                            return 1;
                                        }))))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("enchantment", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    AutoGrindState.KEEP_ENCHANTS.keySet().stream()
                                            .filter(id -> id.toLowerCase().contains(remaining))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "enchantment");
                                    if (AutoGrindState.KEEP_ENCHANTS.remove(name) != null) {
                                        AutoGrindState.save();
                                        ctx.getSource().sendFeedback(Text.literal("已移除保留附魔: ").formatted(Formatting.RED)
                                                .append(Text.literal(name).formatted(Formatting.AQUA)));
                                    } else {
                                        ctx.getSource().sendFeedback(Text.literal("保留清單中找不到: ").formatted(Formatting.YELLOW)
                                                .append(Text.literal(name).formatted(Formatting.WHITE)));
                                    }
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("--- 自動磨石保留清單 ---").formatted(Formatting.GOLD));
                            if (AutoGrindState.KEEP_ENCHANTS.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal(" (清單為空)").formatted(Formatting.GRAY));
                            } else {
                                AutoGrindState.KEEP_ENCHANTS.forEach((id, lv) -> {
                                    MutableText line = Text.literal(" - ").formatted(Formatting.GRAY)
                                            .append(Text.literal(id).formatted(Formatting.AQUA))
                                            .append(Text.literal(" (").formatted(Formatting.WHITE))
                                            .append(Text.literal("≥ ").formatted(Formatting.WHITE))
                                            .append(Text.literal(String.valueOf(lv)).formatted(Formatting.YELLOW))
                                            .append(Text.literal(")").formatted(Formatting.WHITE));
                                    ctx.getSource().sendFeedback(line);
                                });
                            }
                            return 1;
                        }));
    }
}