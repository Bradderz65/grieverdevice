package griever.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "grieverdevice")
public class GrieverConfig implements ConfigData {
    
    @ConfigEntry.Gui.CollapsibleObject
    public DropSettings dropSettings = new DropSettings();
    
    public static class DropSettings {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int dropChancePercentage = 50;
        
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 1000)
        public int maxDevicesInWorld = 10;
    }
    
    @Override
    public void validatePostLoad() {
        // Ensure values are within reasonable ranges
        if (dropSettings.dropChancePercentage < 1) dropSettings.dropChancePercentage = 1;
        if (dropSettings.dropChancePercentage > 100) dropSettings.dropChancePercentage = 100;
        
        if (dropSettings.maxDevicesInWorld < 1) dropSettings.maxDevicesInWorld = 1;
        if (dropSettings.maxDevicesInWorld > 1000) dropSettings.maxDevicesInWorld = 1000;
    }
}
