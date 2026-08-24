package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.SimplySwordsAPI;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// Keeps passive revival cooldowns independent from active weapon ability cooldowns.
public final class RevivalCooldownManager {

    private static final Map<CooldownKey, Long> COOLDOWNS = new HashMap<>();

    private RevivalCooldownManager() {
    }

    public static boolean isCoolingDown(ServerWorld world, LivingEntity owner, ItemStack stack) {
        if (world == null || owner == null || stack == null || stack.isEmpty()) {
            return true;
        }
        return COOLDOWNS.getOrDefault(new CooldownKey(owner.getUuid(), stack.getItem()), 0L) > world.getTime();
    }

    public static void setCooldown(ServerWorld world, LivingEntity owner, ItemStack stack, int ticks) {
        if (world == null || owner == null || stack == null || stack.isEmpty() || ticks <= 0) {
            return;
        }
        int cooldown = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(stack, owner, ticks);
        COOLDOWNS.put(new CooldownKey(owner.getUuid(), stack.getItem()), world.getTime() + cooldown);
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 200L != 0L) {
            return;
        }
        long now = world.getTime();
        Iterator<Map.Entry<CooldownKey, Long>> iterator = COOLDOWNS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }

    private record CooldownKey(UUID ownerId, Item item) {
    }
}
