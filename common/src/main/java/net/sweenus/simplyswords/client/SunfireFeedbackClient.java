package net.sweenus.simplyswords.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.sweenus.simplyswords.network.EmberReservePacket;
import net.sweenus.simplyswords.network.PhoenixStandardPacket;

@Environment(EnvType.CLIENT)
public final class SunfireFeedbackClient {
    private static boolean initialized;
    private static ClientWorld reserveWorld;
    private static EmberReservePacket reserve;

    private SunfireFeedbackClient() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        NetworkManager.registerReceiver(NetworkManager.s2c(), EmberReservePacket.ID, EmberReservePacket.CODEC,
                (packet, context) -> context.queue(() -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.world != null && client.world.getRegistryKey().getValue().equals(packet.dimensionId())) {
                        reserveWorld = client.world;
                        reserve = packet;
                    }
                }));
        NetworkManager.registerReceiver(NetworkManager.s2c(), PhoenixStandardPacket.ID, PhoenixStandardPacket.CODEC,
                (packet, context) -> context.queue(() -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null && client.world != null
                            && client.world.getRegistryKey().getValue().equals(packet.dimensionId())
                            && !packet.displayStack().isEmpty()) {
                        client.gameRenderer.showFloatingItem(packet.displayStack());
                    }
                }));
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> clear());
    }

    public static EmberReservePacket reserve() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != reserveWorld || client.player == null || !client.player.isAlive()) clear();
        return reserve;
    }

    private static void clear() {
        reserveWorld = null;
        reserve = null;
    }
}
