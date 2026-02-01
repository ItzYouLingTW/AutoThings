package net.yotk.ylats.client;

import net.fabricmc.api.ClientModInitializer;
import net.yotk.ylats.client.grind.AutoGrindState;
import net.yotk.ylats.client.grind.AutoGrindTicker;
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
    }
}
