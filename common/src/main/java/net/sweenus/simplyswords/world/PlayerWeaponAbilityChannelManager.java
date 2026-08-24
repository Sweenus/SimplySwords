package net.sweenus.simplyswords.world;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PlayerWeaponAbilityChannelManager {

    private static final Map<ChannelKey, ActiveChannel> ACTIVE_CHANNELS = new HashMap<>();

    private PlayerWeaponAbilityChannelManager() {
    }

    public static void start(ServerPlayerEntity player, Hand hand, ItemStack stack) {
        if (player == null || hand == null || stack == null || stack.isEmpty()) {
            return;
        }

        int maxUseTime = stack.getMaxUseTime(player);
        if (maxUseTime <= 0) {
            return;
        }

        ActiveChannel channel = new ActiveChannel(hand, stack.copy(), player.getWorld().getRegistryKey(),
                player.getWorld().getTime(), maxUseTime);
        ACTIVE_CHANNELS.put(new ChannelKey(player.getUuid(), hand), channel);
    }

    public static boolean stop(ServerPlayerEntity player, Hand hand) {
        if (player == null || hand == null) {
            return false;
        }

        ActiveChannel channel = ACTIVE_CHANNELS.remove(new ChannelKey(player.getUuid(), hand));
        if (channel == null) {
            return false;
        }

        if (channel.finished) {
            return true;
        }

        finish(player, channel);
        return true;
    }

    public static boolean finishEarly(ServerPlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }

        for (Hand hand : Hand.values()) {
            ChannelKey key = new ChannelKey(player.getUuid(), hand);
            ActiveChannel channel = ACTIVE_CHANNELS.get(key);
            if (channel != null
                    && ItemStack.areItemsEqual(channel.stack, stack)
                    && ItemStack.areItemsEqual(player.getStackInHand(hand), stack)) {
                if (!channel.finished) {
                    finish(player, channel);
                }
                channel.finished = true;
                return true;
            }
        }

        return false;
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        Iterator<Map.Entry<ChannelKey, ActiveChannel>> iterator = ACTIVE_CHANNELS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChannelKey, ActiveChannel> entry = iterator.next();
            ChannelKey key = entry.getKey();
            if (!key.playerUuid.equals(player.getUuid())) {
                continue;
            }

            ActiveChannel channel = entry.getValue();
            if (channel.finished) {
                iterator.remove();
                continue;
            }

            if (!isValid(player, channel)) {
                iterator.remove();
                continue;
            }

            int remainingUseTicks = remainingUseTicks(player, channel);
            if (remainingUseTicks <= 0) {
                iterator.remove();
                finish(player, channel);
                continue;
            }

            if (isVanillaChanneling(player, channel)) {
                continue;
            }

            ItemStack currentStack = player.getStackInHand(channel.hand);
            currentStack.usageTick(player.getWorld(), player, remainingUseTicks);
            if (channel.finished) {
                iterator.remove();
            }
        }
    }

    public static boolean isChanneling(ServerPlayerEntity player, Hand hand, Item item) {
        if (player == null || hand == null || item == null) {
            return false;
        }

        ActiveChannel channel = ACTIVE_CHANNELS.get(new ChannelKey(player.getUuid(), hand));
        return channel != null
                && channel.stack.isOf(item)
                && isValid(player, channel);
    }

    private static boolean isValid(ServerPlayerEntity player, ActiveChannel channel) {
        if (!player.isAlive() || player.getWorld().isClient()
                || IncapacitatingStatusEffectRegistry.isIncapacitated(player)
                || player.getWorld().getRegistryKey() != channel.worldKey) {
            return false;
        }

        ItemStack currentStack = player.getStackInHand(channel.hand);
        return !currentStack.isEmpty()
                && ItemStack.areItemsEqual(currentStack, channel.stack)
                && currentStack.getDamage() < currentStack.getMaxDamage() - 1;
    }

    private static boolean isVanillaChanneling(ServerPlayerEntity player, ActiveChannel channel) {
        return player.isUsingItem()
                && player.getActiveHand() == channel.hand
                && ItemStack.areItemsEqual(player.getStackInHand(channel.hand), channel.stack);
    }

    private static void finish(ServerPlayerEntity player, ActiveChannel channel) {
        if (!isValid(player, channel)) {
            return;
        }

        ItemStack currentStack = player.getStackInHand(channel.hand);
        int remainingUseTicks = Math.max(0, remainingUseTicks(player, channel));
        currentStack.onStoppedUsing(player.getWorld(), player, remainingUseTicks);

        if (player.isUsingItem() && player.getActiveHand() == channel.hand) {
            player.clearActiveItem();
        }
    }

    private static int remainingUseTicks(ServerPlayerEntity player, ActiveChannel channel) {
        long elapsed = Math.max(0L, player.getWorld().getTime() - channel.startedAt);
        return (int) Math.max(0L, channel.maxUseTime - elapsed);
    }

    private record ChannelKey(UUID playerUuid, Hand hand) {
    }

    private static final class ActiveChannel {
        private final Hand hand;
        private final ItemStack stack;
        private final RegistryKey<World> worldKey;
        private final long startedAt;
        private final int maxUseTime;
        private boolean finished;

        private ActiveChannel(Hand hand, ItemStack stack, RegistryKey<World> worldKey, long startedAt, int maxUseTime) {
            this.hand = hand;
            this.stack = stack;
            this.worldKey = worldKey;
            this.startedAt = startedAt;
            this.maxUseTime = maxUseTime;
        }
    }
}
