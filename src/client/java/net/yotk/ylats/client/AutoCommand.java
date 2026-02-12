package net.yotk.ylats.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.Box;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.yotk.ylats.client.prepare_litematica.AutoPrepareState;
import net.yotk.ylats.client.take.AutoTakeState;

import java.util.HashMap;
import java.util.Map;

public class AutoCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {
                    // 建立 set 子分支
                    var setBranch = ClientCommandManager.literal("set")
                            .then(setTakeItemBranch(registryAccess))
                            .then(setGrindEnchBranch(registryAccess));

                    // ✅ 如果有 Litematica，擴充 set 分支
                    if (FabricLoader.getInstance().isModLoaded("litematica")) {
                        setBranch = setBranch.then(setPrepareLitematicaBranch());
                    }

                    // 建立主指令 /auto
                    var autoNode = ClientCommandManager.literal("auto")
                            .then(setBranch)
                            .then(toggleTakeItem())
                            .then(toggleGrindstone());

                    // ✅ 條件註冊：只有當 Litematica 已加載時，才加入 prepare-litematica 指令
                    if (FabricLoader.getInstance().isModLoaded("litematica")) {
                        autoNode = autoNode.then(togglePrepareLitematica());
                    }

                    dispatcher.register(autoNode);
                }
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

    private static LiteralArgumentBuilder<FabricClientCommandSource> togglePrepareLitematica() {
        return ClientCommandManager.literal("prepare-litematica")
                .executes(ctx -> {
                    AutoPrepareState.ENABLED = !AutoPrepareState.ENABLED;
                    MutableText status = AutoPrepareState.ENABLED ?
                            Text.literal("開啟").formatted(Formatting.GREEN) :
                            Text.literal("關閉").formatted(Formatting.RED);

                    ctx.getSource().sendFeedback(Text.literal("[AT系統] ").formatted(Formatting.GRAY)
                            .append(Text.literal("自動備貨功能: ").formatted(Formatting.WHITE))
                            .append(status));
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

    // ------------------- Litematica 自動備貨 -------------------

    private static ArgumentBuilder<FabricClientCommandSource, ?> setPrepareLitematicaBranch() {
        return ClientCommandManager.literal("prepare-litematica")
                .then(ClientCommandManager.literal("load")
                        .executes(ctx -> {
                            var client = ctx.getSource().getClient();
                            var placementManager = DataManager.getSchematicPlacementManager();
                            var placements = placementManager.getAllSchematicsPlacements();

                            if (placements.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal("[AT系統] ").formatted(Formatting.RED).append("目前沒有載入任何投影放置！"));
                                return 0;
                            }

                            // 1. 建立 Litematica 底層方法需要的 Map
                            Object2IntOpenHashMap<BlockState> countsTotal = new Object2IntOpenHashMap<>();

                            // 2. 遍歷投影並計算方塊 (這是 Litematica 核心邏輯)
                            for (SchematicPlacement placement : placements) {
                                LitematicaSchematic schematic = placement.getSchematic();
                                for (String regionName : schematic.getAreas().keySet()) {
                                    LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);
                                    if (container == null) continue;

                                    // 這裡使用 Litematica 內建的高效迭代
                                    for (int y = 0; y < container.getSize().getY(); y++) {
                                        for (int x = 0; x < container.getSize().getX(); x++) {
                                            for (int z = 0; z < container.getSize().getZ(); z++) {
                                                BlockState state = container.get(x, y, z);
                                                if (state != null && !state.isAir()) {
                                                    countsTotal.addTo(state, 1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. 呼叫你找到的底層方法進行轉化 (方塊 -> 物品)
                            // 注意：因為我們只想拿總量，countsMissing 和 countsMismatch 傳入空的 Map 即可
                            Object2IntOpenHashMap<BlockState> emptyMap = new Object2IntOpenHashMap<>();
                            var reqs = fi.dy.masa.litematica.materials.MaterialListUtils.getMaterialList(
                                    countsTotal, emptyMap, emptyMap, client.player
                            );

                            if (reqs == null || reqs.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal("[AT系統] ").formatted(Formatting.YELLOW).append("計算完成，但清單為空。"));
                                return 1;
                            }

                            // 4. 更新到我們的待取名單
                            // 4. 更新到我們的待取名單 (使用 setNeeds)
                            Map<Item, Integer> needsMap = new HashMap<>();
                            int count = 0;

                            for (var req : reqs) {
                                int totalNeeded = req.getCountTotal();
                                if (totalNeeded > 0) {
                                    needsMap.put(req.getStack().getItem(), totalNeeded);
                                    count++;
                                }
                            }

                            AutoPrepareState.setNeeds(needsMap);

                            ctx.getSource().sendFeedback(Text.literal("[AT系統] ").formatted(Formatting.GRAY)
                                    .append(Text.literal("成功從藍圖載入 ").formatted(Formatting.GREEN))
                                    .append(Text.literal(String.valueOf(count)).formatted(Formatting.AQUA))
                                    .append(Text.literal(" 種物品需求。").formatted(Formatting.GREEN)));

                            return 1;
                        }));
    }
}