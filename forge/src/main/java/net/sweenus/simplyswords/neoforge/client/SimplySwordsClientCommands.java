package net.sweenus.simplyswords.neoforge.client;

import net.minecraft.server.command.CommandManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.tooltip.UniqueTooltipExportController;

@EventBusSubscriber(modid = SimplySwords.MOD_ID, value = Dist.CLIENT)
public final class SimplySwordsClientCommands {
    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(CommandManager.literal("simplyswords")
                .then(CommandManager.literal("export_unique_tooltips")
                        .executes(context -> UniqueTooltipExportController.start())
                        .then(CommandManager.literal("cancel")
                                .executes(context -> UniqueTooltipExportController.cancel()))));
    }

    private SimplySwordsClientCommands() {
    }
}
