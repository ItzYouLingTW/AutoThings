package net.yotk.ylats.client.prepare_litematica;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LitematicaMaterialProvider {

    public static Map<Item, Integer> collectMaterials() {

        Map<Item, Integer> result = new HashMap<>();

        Collection<SchematicPlacement> placements =
                DataManager.getSchematicPlacementManager().getAllSchematicsPlacements();

        for (SchematicPlacement placement : placements) {

            LitematicaSchematic schematic =
                    (LitematicaSchematic) placement.getSchematic();

            for (String regionName : schematic.getAreas().keySet()) {

                LitematicaBlockStateContainer container =
                        schematic.getSubRegionContainer(regionName);

                if (container == null) continue;

                int sx = container.getSize().getX();
                int sy = container.getSize().getY();
                int sz = container.getSize().getZ();

                for (int x = 0; x < sx; x++) {
                    for (int y = 0; y < sy; y++) {
                        for (int z = 0; z < sz; z++) {

                            BlockState state = container.get(x, y, z);
                            if (state == null || state.isAir()) continue;

                            Item item = state.getBlock().asItem();
                            if (item == Items.AIR) continue;

                            result.merge(item, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        return result;
    }
}
