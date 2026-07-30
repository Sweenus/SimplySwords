package net.sweenus.simplyswords.registry;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.screen.RunicForgeScreenHandler;

public final class ScreenHandlerRegistry {
    public static final DeferredRegister<ScreenHandlerType<?>> SCREEN_HANDLERS =
            DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.SCREEN_HANDLER);

    public static final RegistrySupplier<ScreenHandlerType<RunicForgeScreenHandler>> RUNIC_FORGE =
            SCREEN_HANDLERS.register("runic_forge",
                    () -> MenuRegistry.ofExtended(RunicForgeScreenHandler::new));

    private ScreenHandlerRegistry() {
    }
}
