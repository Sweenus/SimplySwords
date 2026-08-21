package net.sweenus.simplyswords.neoforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingDefinition;
import net.sweenus.simplyswords.util.HelperMethods;

final class NeoForgeSpellPowerCompat {
    private NeoForgeSpellPowerCompat() {
    }

    static float scale(float damageModifier, LivingEntity player, Identifier scalingProfileId) {
        SpellSchool school = SimplySwordsAPI.getSpellScalingDefinition(scalingProfileId)
                .map(SpellScalingDefinition::spellPowerTarget)
                .map(target -> SpellSchools.getSchool(target.schoolId().toString()))
                .orElse(null);
        if (school == null) {
            return 0.0F;
        }
        float scaledValue = (float) (damageModifier
                * SpellPower.getSpellPower(school, player).randomValue());
        return HelperMethods.applySpellPowerApiScalingMultiplier(scaledValue);
    }
}
