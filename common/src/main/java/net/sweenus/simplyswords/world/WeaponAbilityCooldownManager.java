package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class WeaponAbilityCooldownManager {

    private static final Map<CooldownKey, Long> COOLDOWNS = new HashMap<>();

    private WeaponAbilityCooldownManager() {
    }

    public static boolean isCoolingDown(ServerWorld world, LivingEntity actor, ItemStack stack) {
        if (actor == null || stack == null || stack.isEmpty()) {
            return true;
        }
        return COOLDOWNS.getOrDefault(new CooldownKey(actor.getUuid(), stack.getItem()), 0L) > world.getTime();
    }

    public static void setCooldown(ServerWorld world, LivingEntity actor, ItemStack stack, int cooldownTicks) {
        if (actor == null || stack == null || stack.isEmpty() || cooldownTicks <= 0) {
            return;
        }
        COOLDOWNS.put(new CooldownKey(actor.getUuid(), stack.getItem()), world.getTime() + cooldownTicks);
    }

    public static void clearCooldown(LivingEntity actor, ItemStack stack) {
        if (actor == null || stack == null || stack.isEmpty()) {
            return;
        }
        COOLDOWNS.remove(new CooldownKey(actor.getUuid(), stack.getItem()));
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 200L != 0L) {
            return;
        }
        long now = world.getTime();
        Iterator<Map.Entry<CooldownKey, Long>> iterator = COOLDOWNS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<CooldownKey, Long> entry = iterator.next();
            if (entry.getValue() <= now) {
                iterator.remove();
            }
        }
    }

    private record CooldownKey(UUID entityId, Item item) {
    }
}
