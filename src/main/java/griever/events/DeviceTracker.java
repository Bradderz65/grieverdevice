package griever.events;

import griever.Grieverdevice;
import griever.config.ConfigManager;
import griever.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.Random;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class DeviceTracker {
    private static final Random RANDOM = new Random();
    
    // Simple counter for devices in the world
    private static int deviceCount = 0;
    
    // Simple counter for maximum device limit
    private static int maxDevices = 2; // Default, will be updated from config
    
    // Set of UUIDs for spiders we've already processed
    private static final Set<UUID> processedSpiders = new HashSet<>();
    
    public static void register() {
        // Get max devices from config
        maxDevices = ConfigManager.getConfig().dropSettings.maxDevicesInWorld;
        
        // Clear state when server starts
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            // Reset tracking
            deviceCount = 0;
            processedSpiders.clear();
            
            // Set the server in the config manager
            ConfigManager.setServer(server);
            
            Grieverdevice.LOGGER.info("Griever Device tracking initialized. Max devices: " + maxDevices);
        });
        
        // Track when devices are picked up
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity itemEntity && 
                itemEntity.getStack().getItem() == ModItems.GRIEVER_DEVICE) {
                // Decrement count when device is unloaded (picked up or despawned)
                if (deviceCount > 0) {
                    deviceCount--;
                    Grieverdevice.LOGGER.debug("Device unloaded, count now: " + deviceCount);
                }
            }
        });
        
        // Listen for entity deaths directly
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            // Only process if a player killed a spider
            if (entity instanceof PlayerEntity player && killedEntity instanceof SpiderEntity spider) {
                // Check if we've already processed this spider
                UUID spiderId = spider.getUuid();
                if (processedSpiders.contains(spiderId)) {
                    return;
                }
                
                // Mark as processed
                processedSpiders.add(spiderId);
                
                // Roll for drop
                float dropChance = ConfigManager.getDropChance();
                float roll = RANDOM.nextFloat();
                boolean shouldDrop = roll < dropChance;
                
                // Log the drop attempt
                StringBuilder logMsg = new StringBuilder();
                logMsg.append("Spider killed by ").append(player.getEntityName());
                logMsg.append(". Drop chance: ").append(String.format("%.1f", dropChance * 100));
                logMsg.append("%, Roll: ").append(String.format("%.1f", roll * 100)).append("%");
                
                // Drop device if successful roll and under max limit
                if (shouldDrop) {
                    if (deviceCount < maxDevices) {
                        // Increment count FIRST
                        deviceCount++;
                        
                        // Drop the device
                        ItemEntity deviceEntity = new ItemEntity(
                            world, 
                            spider.getX(), 
                            spider.getY(), 
                            spider.getZ(), 
                            new ItemStack(ModItems.GRIEVER_DEVICE)
                        );
                        world.spawnEntity(deviceEntity);
                        
                        logMsg.append(" - Dropped a Griever Device! Count: ").append(deviceCount);
                        logMsg.append("/").append(maxDevices);
                    } else {
                        logMsg.append(" - Successful roll, but max limit reached (")
                              .append(deviceCount).append("/").append(maxDevices).append(")");
                    }
                } else {
                    logMsg.append(" - No drop");
                }
                
                Grieverdevice.LOGGER.info(logMsg.toString());
            }
        });
    }
    
    // Add a command method to get the current count
    public static int getCurrentDeviceCount() {
        return deviceCount;
    }
    
    // Reset count to 0
    public static void resetDeviceCount() {
        deviceCount = 0;
        Grieverdevice.LOGGER.info("Device count reset to 0");
    }
}
