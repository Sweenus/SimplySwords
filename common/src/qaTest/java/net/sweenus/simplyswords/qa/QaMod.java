package net.sweenus.simplyswords.qa;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.command.CommandManager;

public final class QaMod {
    public static final String MOD_ID = "simplyswords_qa";

    private QaMod() {}

    public static void init() {
        if (!Boolean.getBoolean("simplyswords.qa.enabled")) {
            throw new IllegalStateException("The Simply Swords QA companion requires -Dsimplyswords.qa.enabled=true");
        }
        QaNetwork.init();
        TickEvent.SERVER_POST.register(QaHarness::tick);
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("simplyswords_qa")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("run")
                                .then(CommandManager.argument("profile", StringArgumentType.word())
                                        .executes(context -> QaHarness.start(
                                                context.getSource(), StringArgumentType.getString(context, "profile"), 1L, 0))
                                        .then(CommandManager.argument("seed", IntegerArgumentType.integer())
                                                .executes(context -> QaHarness.start(
                                                        context.getSource(), StringArgumentType.getString(context, "profile"),
                                                        IntegerArgumentType.getInteger(context, "seed"), 0))
                                                .then(CommandManager.argument("start_index", IntegerArgumentType.integer(0))
                                                        .executes(context -> QaHarness.start(
                                                                context.getSource(), StringArgumentType.getString(context, "profile"),
                                                                IntegerArgumentType.getInteger(context, "seed"),
                                                                IntegerArgumentType.getInteger(context, "start_index")))
                                                        .then(CommandManager.argument("runic_samples", IntegerArgumentType.integer(1))
                                                                .executes(context -> QaHarness.start(
                                                                        context.getSource(), StringArgumentType.getString(context, "profile"),
                                                                        IntegerArgumentType.getInteger(context, "seed"),
                                                                        IntegerArgumentType.getInteger(context, "start_index"),
                                                                        IntegerArgumentType.getInteger(context, "runic_samples"))))))))
                        .then(CommandManager.literal("abort").executes(QaHarness::abort))
                        .then(CommandManager.literal("status").executes(QaHarness::status))
        ));
    }
}
