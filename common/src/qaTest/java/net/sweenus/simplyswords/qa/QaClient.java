package net.sweenus.simplyswords.qa;

import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.network.UseWeaponAbilityPacket;

public final class QaClient {
    private static boolean announced;
    private static long lastHeartbeat;
    private static PendingRelease pending;

    private QaClient() {}

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(QaClient::tick);
    }

    public static void handle(QaNetwork.ClientAction packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            try {
                if (client.player == null || client.world == null) {
                    status("error", packet.caseId, "client player/world unavailable");
                    return;
                }
                Entity target = client.world.getEntityById(packet.targetEntityId);
                if ("attack".equals(packet.action)) {
                    if (target == null || client.interactionManager == null) {
                        status("error", packet.caseId, "attack target unavailable");
                        return;
                    }
                    client.interactionManager.attackEntity(client.player, target);
                    client.player.swingHand(Hand.MAIN_HAND);
                    status("ack", packet.caseId, "attack dispatched");
                } else if ("ability".equals(packet.action)) {
                    new UseWeaponAbilityPacket(Hand.MAIN_HAND, true).sendToServer();
                    pending = new PendingRelease(packet.caseId, Math.max(1, packet.holdTicks));
                    status("ack", packet.caseId, "ability pressed");
                } else {
                    status("error", packet.caseId, "unknown action " + packet.action);
                }
            } catch (Throwable throwable) {
                status("exception", packet.caseId, throwable.getClass().getName() + ": " + throwable.getMessage());
            }
        });
    }

    private static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            announced = false;
            pending = null;
            return;
        }
        if (!announced) {
            status("hello", "", client.player.getName().getString());
            announced = true;
        }
        long now = System.nanoTime();
        if (now - lastHeartbeat >= 1_000_000_000L) {
            status("heartbeat", "", "ok");
            lastHeartbeat = now;
        }
        if (pending != null && --pending.ticks <= 0) {
            try {
                new UseWeaponAbilityPacket(Hand.MAIN_HAND, false).sendToServer();
                status("released", pending.caseId, "ability released");
            } catch (Throwable throwable) {
                status("exception", pending.caseId, throwable.getClass().getName() + ": " + throwable.getMessage());
            }
            pending = null;
        }
    }

    private static void status(String kind, String caseId, String detail) {
        new QaNetwork.ClientStatus(QaNetwork.PROTOCOL, kind, caseId, detail, System.nanoTime()).sendToServer();
    }

    private static final class PendingRelease {
        private final String caseId;
        private int ticks;
        private PendingRelease(String caseId, int ticks) { this.caseId = caseId; this.ticks = ticks; }
    }
}
