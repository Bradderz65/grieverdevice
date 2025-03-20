package griever.registry;

import griever.Grieverdevice;
import griever.item.GrieverDeviceItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    
    // Define our item
    public static final Item GRIEVER_DEVICE = new GrieverDeviceItem(new FabricItemSettings().maxCount(1));
    
    // Register all items
    public static void registerItems() {
        registerItem("griever_device", GRIEVER_DEVICE);
    }
    
    // Helper method to register an item
    private static void registerItem(String name, Item item) {
        Registry.register(Registries.ITEM, new Identifier(Grieverdevice.MOD_ID, name), item);
    }
} 