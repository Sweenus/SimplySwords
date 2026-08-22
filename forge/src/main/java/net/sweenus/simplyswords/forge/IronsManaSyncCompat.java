package net.sweenus.simplyswords.forge;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.SimplySwords;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

final class IronsManaSyncCompat {
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final ManaSyncSender SENDER = bindSender();

    private IronsManaSyncCompat() {
    }

    static void sync(ServerPlayerEntity player, MagicData magicData) {
        try {
            SENDER.send(player, magicData);
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (WARNED.compareAndSet(false, true)) {
                SimplySwords.LOGGER.warn("Unable to synchronize Iron's Spells mana to the client", exception);
            }
        }
    }

    private static ManaSyncSender bindSender() {
        try {
            return modernSender();
        } catch (ClassNotFoundException ignored) {
            try {
                return legacySender();
            } catch (ReflectiveOperationException exception) {
                return failingSender(exception);
            }
        } catch (ReflectiveOperationException exception) {
            return failingSender(exception);
        }
    }

    private static ManaSyncSender modernSender() throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName("io.redspace.ironsspellbooks.network.SyncManaPacket");
        Constructor<?> packetConstructor = packetClass.getConstructor(MagicData.class);
        Class<?> distributorClass = Class.forName("io.redspace.ironsspellbooks.setup.PacketDistributor");
        Method send = distributorClass.getMethod("sendToPlayer", ServerPlayerEntity.class, Object.class);
        return (player, magicData) -> send.invoke(null, player, packetConstructor.newInstance(magicData));
    }

    private static ManaSyncSender legacySender() throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName("io.redspace.ironsspellbooks.network.ClientboundSyncMana");
        Constructor<?> packetConstructor = packetClass.getConstructor(MagicData.class);
        Class<?> messagesClass = Class.forName("io.redspace.ironsspellbooks.setup.Messages");
        Method send = messagesClass.getMethod("sendToPlayer", Object.class, ServerPlayerEntity.class);
        return (player, magicData) -> send.invoke(null, packetConstructor.newInstance(magicData), player);
    }

    private static ManaSyncSender failingSender(ReflectiveOperationException cause) {
        return (player, magicData) -> {
            throw cause;
        };
    }

    @FunctionalInterface
    private interface ManaSyncSender {
        void send(ServerPlayerEntity player, MagicData magicData) throws ReflectiveOperationException;
    }
}
