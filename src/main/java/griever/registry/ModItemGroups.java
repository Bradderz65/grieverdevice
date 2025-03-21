package griever.registry;

import griever.Grieverdevice;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    
    // Create a registry key for our item group
    public static final RegistryKey<ItemGroup> GRIEVER_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            new Identifier(Grieverdevice.MOD_ID, "griever_group")
    );
    
    // Register our item group
    public static void registerItemGroups() {
        Registry.register(Registries.ITEM_GROUP, GRIEVER_GROUP, FabricItemGroup.builder()
                .displayName(Text.translatable("itemgroup.grieverdevice"))
                .icon(() -> new ItemStack(ModItems.GRIEVER_DEVICE))
                .entries((context, entries) -> {
                    // Add items to the item group
                    entries.add(ModItems.GRIEVER_DEVICE);
                })
                .build()
        );
    }
} 