package griever.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import griever.config.ConfigManager;
import griever.events.DeviceTracker;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class GrieverCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("griever")
                .requires(source -> source.hasPermissionLevel(2)) // Op permission level
                .then(CommandManager.literal("resetdrops")
                    .executes(GrieverCommands::resetDrops)
                )
                .then(CommandManager.literal("count")
                    .executes(GrieverCommands::showCount)
                )
        );
    }
    
    private static int resetDrops(CommandContext<ServerCommandSource> context) {
        // Reset the device count to 0
        DeviceTracker.resetDeviceCount();
        context.getSource().sendFeedback(() -> Text.literal("Reset Griever Device count to 0"), true);
        return 1;
    }
    
    private static int showCount(CommandContext<ServerCommandSource> context) {
        // Show the current device count
        int count = DeviceTracker.getCurrentDeviceCount();
        int max = ConfigManager.getConfig().dropSettings.maxDevicesInWorld;
        context.getSource().sendFeedback(() -> 
            Text.literal("Current Griever Device count: " + count + " / " + max), false);
        return 1;
    }
}
