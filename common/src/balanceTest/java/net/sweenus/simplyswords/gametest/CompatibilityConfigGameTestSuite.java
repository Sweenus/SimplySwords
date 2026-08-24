package net.sweenus.simplyswords.gametest;

import net.minecraft.registry.Registries;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.config.Config;

public final class CompatibilityConfigGameTestSuite {

    private CompatibilityConfigGameTestSuite() {
    }

    public static void run(TestContext context) {
        var spellPowerApi = Config.compatibility.spellPowerApi.getUnconditional();
        var ironsSpells = Config.compatibility.ironsSpells.getUnconditional();

        spellPowerApi.weaponSpellPowerBonuses.keySet().forEach(id -> assertRegistered(context, id));
        ironsSpells.weaponManaCosts.keySet().forEach(id -> assertRegistered(context, id));
        context.assertTrue(
                spellPowerApi.weaponSpellPowerBonuses.isValidEntry(spellPowerApi.weaponSpellPowerBonuses),
                "Spell Power weapon bonuses failed strong config validation");
        context.assertTrue(
                ironsSpells.weaponManaCosts.isValidEntry(ironsSpells.weaponManaCosts),
                "Weapon Mana Costs failed strong config validation");
        context.assertTrue(
                !ironsSpells.weaponManaCosts.containsKey(Identifier.of("simplyswords", "dreadtide")),
                "Inactive Dreadtide must not be present in Weapon Mana Costs");
        context.complete();
    }

    private static void assertRegistered(TestContext context, Identifier id) {
        context.assertTrue(Registries.ITEM.containsId(id),
                "Compatibility config references an unregistered item: " + id);
    }
}
