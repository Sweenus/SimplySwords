package net.sweenus.simplyswords.client.api;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.ObserverStatusEffectSnapshot;
import net.sweenus.simplyswords.client.ObserverStatusEffectClientState;
import net.sweenus.simplyswords.network.ObserverStatusEffectsPacket;

import java.util.Optional;

//
// Client-only access to presentation snapshots for observer-synchronized
// status effects.
//
@Environment(EnvType.CLIENT)
public final class ObserverStatusEffectClientApi {

    private static boolean initialized;

    private ObserverStatusEffectClientApi() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                ObserverStatusEffectsPacket.ID,
                ObserverStatusEffectsPacket.CODEC,
                (packet, context) -> context.queue(() -> ObserverStatusEffectClientState.apply(packet)));
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> ObserverStatusEffectClientState.clear());
    }

    public static boolean isActive(LivingEntity entity, Identifier effectId) {
        return get(entity, effectId).isPresent();
    }

    public static Optional<ObserverStatusEffectSnapshot> get(LivingEntity entity, Identifier effectId) {
        return ObserverStatusEffectClientState.get(entity, effectId);
    }
}
