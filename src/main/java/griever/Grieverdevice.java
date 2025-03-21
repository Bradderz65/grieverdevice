package griever;

import griever.commands.GrieverCommands;
import griever.config.ConfigManager;
import griever.events.DeviceTracker;
import griever.registry.ModItemGroups;
import griever.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Grieverdevice implements ModInitializer {
	public static final String MOD_ID = "grieverdevice";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	// Sound Events
	public static final Identifier BEEP_SOUND_ID = new Identifier(MOD_ID, "beep");
	public static final SoundEvent BEEP_SOUND_EVENT = SoundEvent.of(BEEP_SOUND_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Register config
		ConfigManager.register();
		
		// Register mod items
		ModItems.registerItems();
		
		// Register item groups
		ModItemGroups.registerItemGroups();
		
		// Register sound events
		Registry.register(Registries.SOUND_EVENT, BEEP_SOUND_ID, BEEP_SOUND_EVENT);
		
		// Register device tracker (handles both tracking and drops)
		DeviceTracker.register();
		
		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			GrieverCommands.register(dispatcher);
		});
		
		LOGGER.info("Griever Device mod initialized");
	}
}