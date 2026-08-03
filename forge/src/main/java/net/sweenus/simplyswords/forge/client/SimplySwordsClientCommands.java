package net.sweenus.simplyswords.forge.client;

import net.minecraft.server.command.CommandManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.client.tooltip.UniqueTooltipExportController;

@EventBusSubscriber(modid = SimplySwords.MOD_ID, value = Dist.CLIENT)
public final class SimplySwordsClientCommands {
    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(CommandManager.literal("simplyswords_client")
                .then(CommandManager.literal("export_unique_tooltips")
                        .executes(context -> UniqueTooltipExportController.start())
                        .then(CommandManager.literal("cancel")
                                .executes(context -> UniqueTooltipExportController.cancel()))));
    }

    private SimplySwordsClientCommands() {
    }
}
