package net.sweenus.simplyswords.neoforge.gametest;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingDefinition;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.SpellScalingTarget;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.gametest.CompatibilityConfigGameTestSuite;
import net.sweenus.simplyswords.gametest.SpellPowerWeaponAttributeGameTestSuite;
import net.sweenus.simplyswords.neoforge.ForgeHelperMethods;

public final class SpellPowerWeaponAttributeGameTests {
    private static final Identifier SPELL_POWER_ONLY_PROFILE =
            Identifier.of("simplyswords_balance_test", "spell_power_only");

    @GameTest(templateName = "balance_empty", tickLimit = 20)
    @EmptyTemplate(value = "4x4x4")
    @TestHolder("simplyswords.spell_power_weapon_attributes")
    public static void spellPowerWeaponAttributes(TestContext context) {
        SpellPowerWeaponAttributeGameTestSuite.run(context);
    }

    @GameTest(templateName = "balance_empty", tickLimit = 20)
    @EmptyTemplate(value = "4x4x4")
    @TestHolder("simplyswords.compatibility_config_weapon_ids")
    public static void compatibilityConfigWeaponIds(TestContext context) {
        CompatibilityConfigGameTestSuite.run(context);
    }

    @GameTest(templateName = "balance_empty", tickLimit = 20)
    @EmptyTemplate(value = "4x4x4")
    @TestHolder("simplyswords.spell_power_api_scaling_multiplier")
    public static void spellPowerApiScalingMultiplier(TestContext context) {
        SimplySwordsAPI.registerSpellScalingDefinition(new SpellScalingDefinition(
                SPELL_POWER_ONLY_PROFILE,
                new SpellScalingTarget(Identifier.of("spell_power", "fire"), ""),
                null
        ));
        ZombieEntity actor = context.spawnMob(EntityType.ZOMBIE, 1, 1, 1);
        var settings = Config.compatibility.spellPowerApi.getUnconditional();
        float previousMultiplier = settings.scalingMultiplier.get();
        try {
            settings.scalingMultiplier.accept(1.0F);
            float fullValue = ForgeHelperMethods.useSpellAttributeScaling(
                    1.0F, actor, SPELL_POWER_ONLY_PROFILE);
            float ironsValueAtFullMultiplier = ForgeHelperMethods.useSpellAttributeScaling(
                    1.0F, actor, SpellScalingProfile.FIRE.registryId());

            settings.scalingMultiplier.accept(0.0F);
            float disabledValue = ForgeHelperMethods.useSpellAttributeScaling(
                    1.0F, actor, SPELL_POWER_ONLY_PROFILE);
            float ironsValueAtZeroMultiplier = ForgeHelperMethods.useSpellAttributeScaling(
                    1.0F, actor, SpellScalingProfile.FIRE.registryId());

            context.assertTrue(fullValue > 0.0F, "Spell Power API returned no scaling value");
            context.assertTrue(disabledValue == 0.0F,
                    "Spell Power API scaling multiplier did not suppress the scaling value");
            context.assertTrue(ironsValueAtFullMultiplier == ironsValueAtZeroMultiplier,
                    "Spell Power API scaling multiplier changed Iron's Spells scaling");
            context.complete();
        } finally {
            settings.scalingMultiplier.accept(previousMultiplier);
        }
    }
}
