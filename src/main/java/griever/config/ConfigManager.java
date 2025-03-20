package griever.config;

import griever.Grieverdevice;
import griever.events.DeviceTracker;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.server.MinecraftServer;

public class ConfigManager {
    private static ConfigHolder<GrieverConfig> configHolder;
    private static GrieverConfig config;
    
    // Server instance for accessing persistent data
    private static MinecraftServer server;
    
    public static void register() {
        // Register the config
        AutoConfig.register(GrieverConfig.class, JanksonConfigSerializer::new);
        configHolder = AutoConfig.getConfigHolder(GrieverConfig.class);
        config = configHolder.getConfig();
        
        Grieverdevice.LOGGER.info("Griever Device config loaded");
    }
    
    public static GrieverConfig getConfig() {
        return config;
    }
    
    public static void save() {
        configHolder.save();
    }
    
    // Set the server instance (called when server is available)
    public static void setServer(MinecraftServer serverInstance) {
        server = serverInstance;
    }
    
    public static void incrementDeviceCount() {
        // This is handled directly in DeviceTracker now
        Grieverdevice.LOGGER.debug("Device count incremented");
    }
    
    public static void decrementDeviceCount() {
        // This is handled directly in DeviceTracker now  
        Grieverdevice.LOGGER.debug("Device count decremented");
    }
    
    public static void resetDeviceCount() {
        // Call the DeviceTracker's method
        DeviceTracker.resetDeviceCount();
    }
    
    public static int getCurrentDeviceCount() {
        // Call the DeviceTracker's method
        return DeviceTracker.getCurrentDeviceCount();
    }
    
    public static boolean canSpawnMoreDevices() {
        // Check against the DeviceTracker's count
        return DeviceTracker.getCurrentDeviceCount() < config.dropSettings.maxDevicesInWorld;
    }
    
    public static float getDropChance() {
        return config.dropSettings.dropChancePercentage / 100.0f;
    }
}
