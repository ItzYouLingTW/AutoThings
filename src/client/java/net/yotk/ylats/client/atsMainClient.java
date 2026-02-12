package net.yotk.ylats.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.*;
import net.yotk.ylats.client.grind.AutoGrindState;
import net.yotk.ylats.client.grind.AutoGrindTicker;
import net.yotk.ylats.client.prepare_litematica.AutoPrepareTicker;
import net.yotk.ylats.client.take.AutoLootTicker;
import net.yotk.ylats.client.take.AutoTakeState;

public class atsMainClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // TakeItem / Loot
        AutoTakeState.load();
        AutoLootTicker.register();

        // Grindstone
        AutoGrindState.load();
        AutoGrindTicker.register();

        // Commands
        AutoCommand.register();

        // 監聽容器開啟事件來觸發 Prepare-Litematica
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.currentScreen instanceof HandledScreen<?> screen) {
                ScreenHandler handler = screen.getScreenHandler();
                // 檢查是否為目標容器：箱子、木桶、界伏盒、熔爐
                if (handler instanceof GenericContainerScreenHandler ||
                        handler instanceof ShulkerBoxScreenHandler ||
                        handler instanceof AbstractFurnaceScreenHandler) {

                    AutoPrepareTicker.tick(client);
                }
            }
        });
    }
}
