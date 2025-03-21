package griever.events;

import griever.Grieverdevice;
import griever.config.ConfigManager;
import griever.registry.ModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

public class LootTableModifiers {
    // Vanilla spider loot table ID
    private static final Identifier SPIDER_LOOT_TABLE_ID = EntityType.SPIDER.getLootTableId();
    
    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            // Only modify the spider loot table
            if (SPIDER_LOOT_TABLE_ID.equals(id)) {
                // Get the drop chance from config
                float dropChance = ConfigManager.getDropChance();
                
                // Only add the loot pool if we haven't reached the maximum number of devices
                if (ConfigManager.canSpawnMoreDevices()) {
                    // Add a custom loot pool for the Griever Device
                    LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(ModItems.GRIEVER_DEVICE))
                        .conditionally(RandomChanceLootCondition.builder(dropChance)); // Configurable chance
                    
                    tableBuilder.pool(poolBuilder);
                    
                    Grieverdevice.LOGGER.info("Modified spider loot table to include Griever Device with " + 
                                             (dropChance * 100) + "% drop chance (max: " + 
                                             ConfigManager.getConfig().dropSettings.maxDevicesInWorld + ")");
                }
            }
        });
    }
}
