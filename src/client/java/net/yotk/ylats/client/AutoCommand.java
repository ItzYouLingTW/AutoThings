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
import net.minecraft.text.Text;
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

    private static LiteralArgumentBuilder<FabricClientCommandSource> toggleTakeItem() {
        return ClientCommandManager.literal("takeitem")
                .executes(ctx -> {
                    AutoTakeState.ENABLED = !AutoTakeState.ENABLED;
                    ctx.getSource().sendFeedback(Text.literal("§7[AT系統] §f自動取物: " + (AutoTakeState.ENABLED ? "§a開啟" : "§c關閉")));
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> toggleGrindstone() {
        return ClientCommandManager.literal("grind")
                .executes(ctx -> {
                    AutoGrindState.ENABLED = !AutoGrindState.ENABLED;
                    ctx.getSource().sendFeedback(Text.literal("§7[AT系統] §f自動磨石: " + (AutoGrindState.ENABLED ? "§a開啟" : "§c關閉")));
                    return 1;
                });
    }

    // ------------------- 自動取物配置 -------------------

    private static ArgumentBuilder<FabricClientCommandSource, ?> setTakeItemBranch(CommandRegistryAccess registryAccess) {
        return ClientCommandManager.literal("takeitem")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                .executes(ctx -> {
                                    Item item = ItemStackArgumentType.getItemStackArgument(ctx, "item").getItem();
                                    if (AutoTakeState.TAKE_ITEMS.add(item)) {
                                        AutoTakeState.save();
                                        ctx.getSource().sendFeedback(Text.literal("§a成功新增取物項目: §b").append(Text.translatable(item.getTranslationKey())));
                                    } else {
                                        ctx.getSource().sendFeedback(Text.literal("§e此物品已在取物清單中。"));
                                    }
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("item", ItemStackArgumentType.itemStack(registryAccess))
                                .executes(ctx -> {
                                    Item item = ItemStackArgumentType.getItemStackArgument(ctx, "item").getItem();
                                    if (AutoTakeState.TAKE_ITEMS.remove(item)) {
                                        AutoTakeState.save();
                                        ctx.getSource().sendFeedback(Text.literal("§c已移除取物項目: §b").append(Text.translatable(item.getTranslationKey())));
                                    } else {
                                        ctx.getSource().sendFeedback(Text.literal("§e取物清單中找不到該物品。"));
                                    }
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("§6--- 自動取物清單 ---"));
                            if (AutoTakeState.TAKE_ITEMS.isEmpty()) ctx.getSource().sendFeedback(Text.literal(" §7(清單為空)"));
                            AutoTakeState.TAKE_ITEMS.forEach(i ->
                                    ctx.getSource().sendFeedback(Text.literal(" §7- §b").append(Text.translatable(i.getTranslationKey()))));
                            return 1;
                        }));
    }

    // ------------------- 自動磨石配置 -------------------

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

                                            ctx.getSource().sendFeedback(Text.literal("§a已設定保留附魔: §b" + enchId + " §f(§f≥ §e" + level + "§f)"));
                                            return 1;
                                        }))))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("enchantment", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    AutoGrindState.KEEP_ENCHANTS.keySet().stream()
                                            .filter(id -> id.startsWith(remaining) || (id.contains(":") && id.split(":")[1].startsWith(remaining)))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "enchantment");
                                    if (AutoGrindState.KEEP_ENCHANTS.remove(name) != null) {
                                        AutoGrindState.save();
                                        ctx.getSource().sendFeedback(Text.literal("§c已移除保留附魔: §b" + name));
                                    } else {
                                        ctx.getSource().sendFeedback(Text.literal("§e保留清單中找不到: " + name));
                                    }
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal("§6--- 自動磨石保留清單 ---"));
                            if (AutoGrindState.KEEP_ENCHANTS.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal(" §7(清單為空)"));
                            } else {
                                AutoGrindState.KEEP_ENCHANTS.forEach((id, lv) ->
                                        ctx.getSource().sendFeedback(Text.literal(" §7- §b" + id + " §f(§f≥ §e" + lv + "§f)")));
                            }
                            return 1;
                        }));
    }
}