package griever.data;

import griever.Grieverdevice;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

/**
 * Persistent state for tracking Griever Device data across game sessions
 */
public class GrieverDeviceData extends PersistentState {
    private static final String IDENTIFIER = Grieverdevice.MOD_ID + "_data";
    private static final String DEVICE_COUNT_KEY = "device_count";
    
    private int deviceCount = 0;
    
    public GrieverDeviceData() {
        // Default constructor
    }
    
    public GrieverDeviceData(int deviceCount) {
        this.deviceCount = deviceCount;
    }
    
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt(DEVICE_COUNT_KEY, deviceCount);
        return nbt;
    }
    
    public static GrieverDeviceData createFromNbt(NbtCompound nbt) {
        GrieverDeviceData data = new GrieverDeviceData();
        data.deviceCount = nbt.getInt(DEVICE_COUNT_KEY);
        return data;
    }
    
    /**
     * Gets the GrieverDeviceData for the server, creating it if it doesn't exist
     */
    public static GrieverDeviceData getOrCreate(MinecraftServer server) {
        // Get the overworld persistent state manager
        var persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        
        // Get or create our data
        return persistentStateManager.getOrCreate(
            GrieverDeviceData::createFromNbt,
            GrieverDeviceData::new,
            IDENTIFIER
        );
    }
    
    /**
     * Gets the current device count
     */
    public int getDeviceCount() {
        return deviceCount;
    }
    
    /**
     * Sets the device count
     */
    public void setDeviceCount(int count) {
        this.deviceCount = Math.max(0, count); // Ensure count is never negative
        this.markDirty(); // Mark as dirty to ensure it gets saved
    }
    
    /**
     * Increments the device count
     */
    public void incrementDeviceCount() {
        this.deviceCount++;
        this.markDirty();
    }
    
    /**
     * Decrements the device count
     */
    public void decrementDeviceCount() {
        if (this.deviceCount > 0) {
            this.deviceCount--;
            this.markDirty();
        }
    }
}
