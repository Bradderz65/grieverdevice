package griever.item;

import griever.Grieverdevice;
import griever.config.ConfigManager;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class GrieverDeviceItem extends Item {
    
    // NBT keys - storing as constants to avoid string repetition
    private static final String TARGET_X_KEY = "TargetX";
    private static final String TARGET_Y_KEY = "TargetY";
    private static final String TARGET_Z_KEY = "TargetZ";
    public static final String HAS_TARGET_KEY = "HasTarget";
    private static final String LIGHT_ON_KEY = "LightOn";
    // New NBT keys for light/client functionality
    public static final String CURRENT_DISTANCE_KEY = "CurrentDistance";
    public static final String LAST_BEEP_TIME_KEY = "LastBeepTime";
    public static final String BEEP_INTERVAL_KEY = "BeepInterval";
    
    // Use FastUtil collections for better performance with primitive types
    private static final Object2IntMap<UUID> playerBeepTimers = new Object2IntOpenHashMap<>();
    private static final Object2DoubleMap<UUID> playerLastDistances = new Object2DoubleOpenHashMap<>();
    private static final Object2LongMap<UUID> playerLastBeepTimes = new Object2LongOpenHashMap<>();
    
    // Constants for beep intervals (in ticks)
    private static final int MIN_BEEP_INTERVAL = 10; // Fastest beeping rate (for very close proximity)
    private static final int MAX_BEEP_INTERVAL = 60; // Slowest beeping rate (for max distance)
    
    // Distance thresholds (in blocks)
    private static final double MAX_DETECTION_DISTANCE = 100.0; // Maximum distance for detection
    private static final double CLOSE_THRESHOLD = 10.0; // Threshold for "close" status (green light)
    private static final double VERY_CLOSE_THRESHOLD = 3.0; // Threshold for "very close" status
    private static final double MEDIUM_THRESHOLD = 30.0; // Threshold for "medium" distance
    private static final double FAR_THRESHOLD = 70.0; // Threshold for "far" distance
    
    // Volume settings
    private static final float CLOSE_VOLUME = 1.0f;
    private static final float FAR_VOLUME = 0.6f;
    
    // Pre-calculate constants used in formulas to avoid redundant calculations
    private static final double INTERVAL_RANGE = MAX_BEEP_INTERVAL - MIN_BEEP_INTERVAL;
    private static final double INVERSE_MAX_DISTANCE = 1.0 / MAX_DETECTION_DISTANCE;
    private static final double VOLUME_DISTANCE_RANGE = FAR_THRESHOLD - CLOSE_THRESHOLD;
    private static final double INVERSE_VOLUME_DISTANCE_RANGE = 1.0 / VOLUME_DISTANCE_RANGE;
    private static final float VOLUME_RANGE = CLOSE_VOLUME - FAR_VOLUME;
    
    // Static text instances to avoid creating new ones repeatedly
    private static final Text NO_TARGET_TEXT = Text.translatable("item.grieverdevice.griever_device.no_target");
    private static final Text GREEN_LIGHT_TEXT = Text.literal("Light: GREEN (Within range)");
    private static final Text RED_LIGHT_TEXT = Text.literal("Light: RED (Outside range)");
    private static final Text VERY_STRONG_TEXT = Text.literal("Signal strength: Very Strong (You're extremely close!)");
    private static final Text STRONG_TEXT = Text.literal("Signal strength: Strong (You're getting close)");
    private static final Text MEDIUM_TEXT = Text.literal("Signal strength: Medium (On the right track)");
    private static final Text WEAK_TEXT = Text.literal("Signal strength: Weak (Still quite far)");
    private static final Text VERY_WEAK_TEXT = Text.literal("Signal strength: Very Weak (Very far away)");
    
    public GrieverDeviceItem(Settings settings) {
        super(settings);
        
        // Initialize FastUtil collections with default values
        playerBeepTimers.defaultReturnValue(0);
        playerLastDistances.defaultReturnValue(Double.MAX_VALUE);
        playerLastBeepTimes.defaultReturnValue(0L);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        if (hasTarget(stack)) {
            BlockPos targetPos = getTargetPos(stack);
            tooltip.add(Text.translatable("item.grieverdevice.griever_device.tooltip.target", 
                    targetPos.getX(), targetPos.getY(), targetPos.getZ()));
        } else {
            tooltip.add(Text.translatable("item.grieverdevice.griever_device.tooltip.no_target"));
        }
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        ItemStack stack = context.getStack();
        BlockPos blockPos = context.getBlockPos();
        
        if (player != null && player.isSneaking()) {
            // Set target location when shift-right-clicking on a block
            setTargetPos(stack, blockPos);
            
            if (!world.isClient) {
                player.sendMessage(Text.translatable("item.grieverdevice.griever_device.target_set", 
                        blockPos.getX(), blockPos.getY(), blockPos.getZ()), true);
                
                // Play a confirmation sound
                world.playSound(
                    null, 
                    blockPos, 
                    Grieverdevice.BEEP_SOUND_EVENT, 
                    SoundCategory.PLAYERS, 
                    1.0f, 
                    1.0f 
                );
                
                // Reset beep timer and tracking for this player
                UUID playerUuid = player.getUuid();
                playerBeepTimers.put(playerUuid, 0);
                playerLastDistances.put(playerUuid, calculateDistance(player, blockPos));
                playerLastBeepTimes.put(playerUuid, world.getTime());
            }
            
            return ActionResult.success(world.isClient);
        }
        
        return ActionResult.PASS;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (!user.isSneaking()) {
            if (hasTarget(stack)) {
                // Calculate distance to target
                BlockPos targetPos = getTargetPos(stack);
                double distance = calculateDistance(user, targetPos);
                
                // Store current distance in NBT for client-side rendering
                updateDistanceData(stack, distance);
                
                if (!world.isClient) {
                    // Send distance information to the player
                    user.sendMessage(Text.literal(String.format("Distance to target: %.1f blocks", distance)), true);
                    
                    // Update message to include color information
                    if (distance <= CLOSE_THRESHOLD) { 
                        user.sendMessage(GREEN_LIGHT_TEXT, true);
                    } else {
                        user.sendMessage(RED_LIGHT_TEXT, true);
                    }
                    
                    // Send signal strength description
                    user.sendMessage(getSignalStrengthText(distance), true);
                    
                    if (Grieverdevice.LOGGER.isDebugEnabled()) {
                        Grieverdevice.LOGGER.debug("Player {} used the Griever Device. Distance to target: {}", 
                                user.getName().getString(), distance);
                    }
                }
                
                return TypedActionResult.success(stack);
            } else {
                // No target set
                if (!world.isClient) {
                    // Play a different sound for no target
                    world.playSound(
                        null, 
                        user.getBlockPos(), 
                        Grieverdevice.BEEP_SOUND_EVENT, 
                        SoundCategory.PLAYERS, 
                        0.5f, 
                        0.5f 
                    );
                    
                    user.sendMessage(NO_TARGET_TEXT, true);
                }
                
                return TypedActionResult.success(stack);
            }
        }
        
        return TypedActionResult.pass(stack);
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        // Only process if this is a player and the item is selected or in offhand
        if (entity instanceof PlayerEntity player && (selected || player.getOffHandStack() == stack)) {
            // Only process if the item has a target
            if (hasTarget(stack)) {
                processDeviceTick(stack, world, player);
            }
        }
    }
    
    // Optimized distance calculation - avoid creating temporary objects and use squared distance where possible
    private double calculateDistance(PlayerEntity player, BlockPos targetPos) {
        double dx = player.getX() - (targetPos.getX() + 0.5);
        double dy = player.getY() - (targetPos.getY() + 0.5);
        double dz = player.getZ() - (targetPos.getZ() + 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    // Calculate squared distance - more efficient when only comparing distances
    private double calculateDistanceSquared(PlayerEntity player, BlockPos targetPos) {
        double dx = player.getX() - (targetPos.getX() + 0.5);
        double dy = player.getY() - (targetPos.getY() + 0.5);
        double dz = player.getZ() - (targetPos.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz;
    }
    
    // Adjusted beep interval calculation for smoother response with better performance
    private int calculateBeepInterval(double distance) {
        // Clamp the distance to our maximum detection range
        double clampedDistance = Math.min(distance, MAX_DETECTION_DISTANCE);
        
        // Calculate the interval using a more gradual curve
        double distanceRatio = clampedDistance * INVERSE_MAX_DISTANCE;
        
        // Use a linear scale for more predictable beeping
        return Math.max(MIN_BEEP_INTERVAL, (int)(MIN_BEEP_INTERVAL + (distanceRatio * INTERVAL_RANGE)));
    }
    
    // Optimized processDeviceTick method
    private void processDeviceTick(ItemStack stack, World world, PlayerEntity player) {
        // Only process if the player has the device selected or if it's in the offhand
        if (!isActivelyHeld(player, stack)) {
            return;
        }
        
        UUID playerUuid = player.getUuid();
        BlockPos targetPos = getTargetPos(stack);
        
        // Use squared distance for comparison to avoid sqrt calculation when possible
        double currentDistanceSquared = calculateDistanceSquared(player, targetPos);
        double lastDistanceSquared = playerLastDistances.getDouble(playerUuid) * playerLastDistances.getDouble(playerUuid);
        
        // Only calculate actual distance when needed for beep interval
        double currentDistance = Math.sqrt(currentDistanceSquared);
        
        // Calculate beep interval based on distance
        int beepInterval = calculateBeepInterval(currentDistance);
        
        long currentTime = world.getTime();
        int beepTimer = playerBeepTimers.getInt(playerUuid);
        beepTimer++;
        
        if (beepTimer >= beepInterval) {
            // Always play the sound on both client and server
            // This ensures more consistent beeping
            float volume = calculateVolume(currentDistance);
            
            // Play on server (for other players)
            if (!world.isClient) {
                world.playSound(
                    null, 
                    player.getBlockPos(), 
                    Grieverdevice.BEEP_SOUND_EVENT, 
                    SoundCategory.PLAYERS, 
                    volume, 
                    1.0f 
                );
            }
            
            // Play on client (for the player)
            if (world.isClient) {
                // Use the correct method signature for client-side sound
                player.playSound(
                    Grieverdevice.BEEP_SOUND_EVENT,
                    SoundCategory.PLAYERS,
                    volume,
                    1.0f
                );
            }
            
            // Store the time of this beep and the interval in the NBT data
            updateBeepData(stack, currentTime, beepInterval);
            
            playerLastBeepTimes.put(playerUuid, currentTime);
            beepTimer = 0;
        }
        
        playerBeepTimers.put(playerUuid, beepTimer);
        playerLastDistances.put(playerUuid, currentDistance);
        updateDistanceData(stack, currentDistance);
    }
    
    // Helper method to check if the device is actively held
    private boolean isActivelyHeld(PlayerEntity player, ItemStack stack) {
        return player.getMainHandStack() == stack || player.getOffHandStack() == stack;
    }
    
    // Optimized volume calculation
    private float calculateVolume(double distance) {
        // Volume decreases with distance - using pre-calculated constants
        if (distance <= CLOSE_THRESHOLD) {
            return CLOSE_VOLUME;
        } else if (distance >= FAR_THRESHOLD) {
            return FAR_VOLUME;
        } else {
            // Linear interpolation with pre-calculated constants
            double ratio = (distance - CLOSE_THRESHOLD) * INVERSE_VOLUME_DISTANCE_RANGE;
            return CLOSE_VOLUME - (float)(ratio * VOLUME_RANGE);
        }
    }
    
    // Get pre-created text to avoid creating new Text objects each time
    private Text getSignalStrengthText(double distance) {
        if (distance <= VERY_CLOSE_THRESHOLD) {
            return VERY_STRONG_TEXT;
        } else if (distance <= CLOSE_THRESHOLD) {
            return STRONG_TEXT;
        } else if (distance <= MEDIUM_THRESHOLD) {
            return MEDIUM_TEXT;
        } else if (distance <= FAR_THRESHOLD) {
            return WEAK_TEXT;
        } else {
            return VERY_WEAK_TEXT;
        }
    }
    
    // Helper method to update distance data in NBT
    private void updateDistanceData(ItemStack stack, double distance) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putDouble(CURRENT_DISTANCE_KEY, distance);
    }
    
    // Helper method to update beep timing data in NBT
    private void updateBeepData(ItemStack stack, long worldTime, int beepInterval) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(LAST_BEEP_TIME_KEY, worldTime);
        nbt.putInt(BEEP_INTERVAL_KEY, beepInterval);
    }
    
    // NBT handling methods
    public void setTargetPos(ItemStack stack, BlockPos pos) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt(TARGET_X_KEY, pos.getX());
        nbt.putInt(TARGET_Y_KEY, pos.getY());
        nbt.putInt(TARGET_Z_KEY, pos.getZ());
        nbt.putBoolean(HAS_TARGET_KEY, true);
    }
    
    public BlockPos getTargetPos(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(TARGET_X_KEY)) {
            int x = nbt.getInt(TARGET_X_KEY);
            int y = nbt.getInt(TARGET_Y_KEY);
            int z = nbt.getInt(TARGET_Z_KEY);
            return new BlockPos(x, y, z);
        }
        return BlockPos.ORIGIN;
    }
    
    public boolean hasTarget(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(HAS_TARGET_KEY) && nbt.getBoolean(HAS_TARGET_KEY);
    }
    
    // Light control methods
    public boolean isLightOn(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(LIGHT_ON_KEY);
    }
    
    public void setLightOn(ItemStack stack, boolean lightOn) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(LIGHT_ON_KEY, lightOn);
    }
    
    public void toggleLight(ItemStack stack) {
        setLightOn(stack, !isLightOn(stack));
    }
}