package net.sweenus.simplyswords.api;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellScalingRegistryTest {
    @Test
    void builtInMappingsRemainStable() {
        SpellScalingDefinition frost = SimplySwordsAPI.getSpellScalingDefinition(
                SpellScalingProfile.FROST.registryId()).orElseThrow();

        assertEquals(new Identifier("spell_power", "frost"), frost.spellPowerTarget().schoolId());
        assertEquals("item.simplyswords.compat.scaleFrost", frost.spellPowerTarget().displayTranslationKey());
        assertEquals(new Identifier("irons_spellbooks", "ice"), frost.ironsTarget().schoolId());
        assertEquals("item.simplyswords.compat.scaleIce", frost.ironsTarget().displayTranslationKey());
    }

    @Test
    void addonProfileCanTargetBothBackends() {
        Identifier id = new Identifier("simplyswords_test", "solar");
        SpellScalingDefinition definition = new SpellScalingDefinition(
                id,
                new SpellScalingTarget(new Identifier("examplemagic", "solar"), "school.examplemagic.solar"),
                new SpellScalingTarget(new Identifier("ironsaddon", "solar"), "school.ironsaddon.solar")
        );

        SimplySwordsAPI.registerSpellScalingDefinition(definition);

        assertEquals(definition, SimplySwordsAPI.getSpellScalingDefinition(id).orElseThrow());
    }

    @Test
    void conflictingRegistrationFailsClearly() {
        Identifier id = new Identifier("simplyswords_test", "duplicate");
        SpellScalingDefinition first = new SpellScalingDefinition(
                id,
                new SpellScalingTarget(new Identifier("examplemagic", "first"), "school.examplemagic.first"),
                null
        );
        SpellScalingDefinition conflicting = new SpellScalingDefinition(
                id,
                new SpellScalingTarget(new Identifier("examplemagic", "second"), "school.examplemagic.second"),
                null
        );

        SimplySwordsAPI.registerSpellScalingDefinition(first);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> SimplySwordsAPI.registerSpellScalingDefinition(conflicting));
        assertTrue(exception.getMessage().contains(id.toString()));
    }
}
