package net.sweenus.simplyswords.world;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerWeaponAbilityKeybindState {

    private static final Map<UUID, KeybindState> SERVER_STATES = new HashMap<>();
    private static boolean clientMainhandRebound;
    private static boolean clientOffhandRebound;

    private PlayerWeaponAbilityKeybindState() {
    }

    public static void setClientState(boolean mainhandRebound, boolean offhandRebound) {
        clientMainhandRebound = mainhandRebound;
        clientOffhandRebound = offhandRebound;
    }

    public static void updateServerState(ServerPlayerEntity player, boolean mainhandRebound, boolean offhandRebound) {
        if (player != null) {
            SERVER_STATES.put(player.getUuid(), new KeybindState(mainhandRebound, offhandRebound));
        }
    }

    public static void clearServerState(ServerPlayerEntity player) {
        if (player != null) {
            SERVER_STATES.remove(player.getUuid());
        }
    }

    public static boolean isHandRebound(World world, PlayerEntity player, Hand hand) {
        if (hand == null) {
            return false;
        }
        if (world != null && world.isClient()) {
            return hand == Hand.MAIN_HAND ? clientMainhandRebound : clientOffhandRebound;
        }
        if (player == null) {
            return false;
        }

        KeybindState state = SERVER_STATES.get(player.getUuid());
        return state != null && (hand == Hand.MAIN_HAND ? state.mainhandRebound : state.offhandRebound);
    }

    private record KeybindState(boolean mainhandRebound, boolean offhandRebound) {
    }
}
