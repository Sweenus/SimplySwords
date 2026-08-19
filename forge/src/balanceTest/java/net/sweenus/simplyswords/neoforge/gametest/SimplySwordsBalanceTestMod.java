package net.sweenus.simplyswords.neoforge.gametest;

import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.testframework.conf.FrameworkConfiguration;

@Mod("simplyswords_balance_test")
public final class SimplySwordsBalanceTestMod {

    public SimplySwordsBalanceTestMod(IEventBus eventBus, ModContainer container) {
        IronsCasterGearEnhancer.register();
        FrameworkConfiguration.builder(Identifier.of("simplyswords_balance_test", "tests"))
                .build()
                .create()
                .init(eventBus, container);
    }
}
