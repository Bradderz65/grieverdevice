package griever;

import griever.item.GrieverDeviceItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import griever.registry.ModItems;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GrieverdeviceClient implements ClientModInitializer {
	
	// Constants for the light colors
	private static final int RED_COLOR = 0xFF0000;
	private static final int GREEN_COLOR = 0x00FF00;
	private static final Logger LOGGER = LogManager.getLogger();
	
	// Static variable to track flashing state
	private static boolean flashState = false;
	private static boolean lastWithinRange = false; // Track when we enter the 5-block range
	
	// Flash duration in ticks (how long the light stays on when it flashes)
	private static final int FLASH_DURATION = 3;
	
	@Override
	public void onInitializeClient() {
		// Register color provider for the Griever Device
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
			// Only apply coloring to tintIndex 1 (which is our light)
			if (tintIndex == 1) {
				NbtCompound nbt = stack.getNbt();
				
				// Check if the device has a target
				if (nbt != null && nbt.contains(GrieverDeviceItem.HAS_TARGET_KEY) && nbt.getBoolean(GrieverDeviceItem.HAS_TARGET_KEY)) {
					// Calculate distance if player is close enough
					if (nbt.contains(GrieverDeviceItem.CURRENT_DISTANCE_KEY)) {
						double distance = nbt.getDouble(GrieverDeviceItem.CURRENT_DISTANCE_KEY);
						
						// Turn green if within 5 blocks
						if (distance <= 5.0) {
							// Only log when we first enter the 5-block range
							if (!lastWithinRange) {
								LOGGER.info("Within 5 blocks, turning green.");
								lastWithinRange = true;
							}
							return GREEN_COLOR;
						} else {
							// Reset the range tracking when we leave the 5-block range
							if (lastWithinRange) {
								lastWithinRange = false;
							}
							
							// Get the current world time
							MinecraftClient client = MinecraftClient.getInstance();
							if (client.world == null) {
								return RED_COLOR; // Default to red if world is null
							}
							
							long worldTime = client.world.getTime();
							
							// Check if we have beep timing data
							if (nbt.contains(GrieverDeviceItem.LAST_BEEP_TIME_KEY) && nbt.contains(GrieverDeviceItem.BEEP_INTERVAL_KEY)) {
								long lastBeepTime = nbt.getLong(GrieverDeviceItem.LAST_BEEP_TIME_KEY);
								int beepInterval = nbt.getInt(GrieverDeviceItem.BEEP_INTERVAL_KEY);
								
								// Calculate time since last beep
								long timeSinceLastBeep = worldTime - lastBeepTime;
								
								// Flash red only when a beep occurs (at the start of each interval)
								// or for a short duration after the beep
								if (timeSinceLastBeep < FLASH_DURATION || timeSinceLastBeep >= beepInterval && timeSinceLastBeep < beepInterval + FLASH_DURATION) {
									return RED_COLOR; // Light ON during beep
								} else {
									return 0x000000; // Light OFF between beeps
								}
							}
							
							// Fallback if no beep timing data is available
							return RED_COLOR;
						}
					}
					
					// Fallback to simple red if we can't get the distance
					return RED_COLOR;
				}
				
				// Default light color when no target
				return 0x888888; // Gray when inactive
			}
			
			// Return white (no tint) for other parts
			return 0xFFFFFF;
		}, ModItems.GRIEVER_DEVICE);
	}
}