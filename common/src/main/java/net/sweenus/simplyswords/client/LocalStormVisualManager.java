package net.sweenus.simplyswords.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.sweenus.simplyswords.api.LocalStormStatusEffectRegistry;
import net.sweenus.simplyswords.client.api.ObserverStatusEffectClientApi;

@Environment(EnvType.CLIENT)
public final class LocalStormVisualManager {

    private static float intensity;
    private static boolean initialized;

    private LocalStormVisualManager() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(LocalStormVisualManager::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> intensity = 0.0F);
    }

    private static void tick(MinecraftClient client) {
        boolean active = false;
        if (client != null && client.player != null && client.world != null) {
            for (Identifier effectId : LocalStormStatusEffectRegistry.registeredEffects()) {
                if (ObserverStatusEffectClientApi.isActive(client.player, effectId)) {
                    active = true;
                    break;
                }
            }
        }
        float target = active ? 1.0F : 0.0F;
        if (intensity < target) intensity = Math.min(target, intensity + 0.1F);
        else if (intensity > target) intensity = Math.max(target, intensity - 0.1F);
        intensity = MathHelper.clamp(intensity, 0.0F, 1.0F);
    }

    public static float intensity() {
        return intensity;
    }
}
