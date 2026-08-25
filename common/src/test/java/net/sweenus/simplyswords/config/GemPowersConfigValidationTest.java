package net.sweenus.simplyswords.config;

import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedSet;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.power.GemPower;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemPowersConfigValidationTest {

    @Test
    void disabledPowerIdsDoNotRequireARegistryAndAllowAddonIds() {
        ValidatedSet<Identifier> disabledPowers = GemPowersConfig.createDisabledPowers();
        Identifier addonPower = Identifier.of("example_addon", "custom_power");

        assertTrue(disabledPowers.isEmpty());

        Set<Identifier> configuredIds = Set.of(GemPower.EMPTY_ID, addonPower);
        disabledPowers.accept(configuredIds);

        assertEquals(configuredIds, disabledPowers.get());
    }
}
