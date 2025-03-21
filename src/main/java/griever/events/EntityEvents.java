package griever.events;

import griever.Grieverdevice;
import griever.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Random;

public class EntityEvents {
    private static final Random RANDOM = new Random();
    
    public static void register() {
        // Register the server tick event to check for spider deaths
        ServerTickEvents.END_WORLD_TICK.register(EntityEvents::onWorldTick);
    }
    
    private static void onWorldTick(ServerWorld world) {
        // Process all entities in the world
        for (Entity entity : world.getEntitiesByType(EntityType.SPIDER, (spider) -> true)) {
            if (entity instanceof SpiderEntity spider) {
                // Check if the spider is dead or about to die
                if (spider.isDead() || spider.getHealth() <= 0) {
                    // Check if the spider was killed by a player
                    LivingEntity attacker = spider.getAttacker();
                    if (attacker instanceof PlayerEntity && RANDOM.nextFloat() < 0.5f) {
                        // 50% chance to drop the Griever Device
                        world.spawnEntity(new ItemEntity(
                            world,
                            spider.getX(),
                            spider.getY(),
                            spider.getZ(),
                            new ItemStack(ModItems.GRIEVER_DEVICE)
                        ));
                        
                        // Remove the spider from our tracking to avoid duplicate drops
                        spider.setAttacker(null);
                    }
                }
            }
        }
    }
}
