package net.sweenus.simplyswords.world;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.PlayerMovementIntent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerMovementIntentManager {

    private static final int STALE_AFTER_TICKS = 40;
    private static final Map<UUID, StoredIntent> INTENTS = new HashMap<>();

    private PlayerMovementIntentManager() {
    }

    public static void update(ServerPlayerEntity player, int forward, int strafe) {
        if (player == null) return;
        INTENTS.put(player.getUuid(), new StoredIntent(
                new PlayerMovementIntent(forward, strafe), player.getServerWorld().getRegistryKey(),
                player.getServerWorld().getTime()));
    }

    public static PlayerMovementIntent get(ServerPlayerEntity player) {
        if (player == null) return PlayerMovementIntent.NONE;
        StoredIntent stored = INTENTS.get(player.getUuid());
        if (stored == null || !stored.dimension.equals(player.getServerWorld().getRegistryKey())
                || player.getServerWorld().getTime() - stored.updatedAt > STALE_AFTER_TICKS) {
            return PlayerMovementIntent.NONE;
        }
        return stored.intent;
    }

    public static void clear(ServerPlayerEntity player) {
        if (player != null) INTENTS.remove(player.getUuid());
    }

    private record StoredIntent(PlayerMovementIntent intent, RegistryKey<World> dimension, long updatedAt) {
    }
}
