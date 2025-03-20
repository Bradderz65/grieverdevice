package griever;

import griever.item.GrieverDeviceItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.item.ItemColorProvider;
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
						LOGGER.info("Distance: " + distance);
						
						// Turn green if within 5 blocks
						if (distance <= 5.0) {
							LOGGER.info("Within 5 blocks, turning green.");
							return GREEN_COLOR;
						}
					}
					
					// If not close enough, flash red based on system time
					boolean flashRed = (System.currentTimeMillis() / 250 % 2 == 0);
					LOGGER.info("Flashing red: " + flashRed);
					return flashRed ? RED_COLOR : 0x000000;
				}
				
				// Default light color when no target
				return 0x888888; // Gray when inactive
			}
			
			// Return white (no tint) for other parts
			return 0xFFFFFF;
		}, ModItems.GRIEVER_DEVICE);
	}
}