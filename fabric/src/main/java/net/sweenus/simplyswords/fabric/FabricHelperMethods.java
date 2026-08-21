package net.sweenus.simplyswords.fabric;

import dev.architectury.platform.Platform;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingDefinition;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.SpellScalingTarget;

public class FabricHelperMethods {


    public static String spellSchoolDisplayKey(Identifier scalingProfileId) {
        return target(scalingProfileId)
                .map(SpellScalingTarget::displayTranslationKey)
                .filter(key -> !key.isBlank())
                .orElse("item.simplyswords.compat.scaleArcane");
    }

    public static String spellSchoolDisplayKey(String legacySchool) {
        return spellSchoolDisplayKey(SpellScalingProfile.fromLegacyName(legacySchool).registryId());
    }

    public static float useSpellAttributeScaling(float damageModifier, LivingEntity player, Identifier scalingProfileId) {
        if (Platform.isFabric() && SimplySwords.passVersionCheck("spell_power", SimplySwords.minimumSpellPowerVersion)) {
            if (player != null && !player.getWorld().isClient) {
                SpellSchool school = resolveSchool(scalingProfileId);
                if (school != null) {
                    return (float) (damageModifier * SpellPower.getSpellPower(school, player).randomValue());
                }
            }
        }
        return 0;
    }

    public static float useSpellAttributeScaling(float damageModifier, LivingEntity player, String legacySchool) {
        return useSpellAttributeScaling(
                damageModifier, player, SpellScalingProfile.fromLegacyName(legacySchool).registryId());
    }

    public static DamageSource getAbilityMagicDamageSource(ServerWorld world, LivingEntity actor,
                                                            Identifier scalingProfileId) {
        return world.getDamageSources().indirectMagic(actor, actor);
    }

    public static float getAbilityMagicResistanceMultiplier(LivingEntity target, Identifier scalingProfileId) {
        return 1.0F;
    }

    public static int applySpellCooldownReduction(int baseTicks, LivingEntity actor) {
        return baseTicks;
    }

    private static SpellSchool resolveSchool(Identifier scalingProfileId) {
        return target(scalingProfileId)
                .map(target -> SpellSchools.getSchool(target.schoolId().toString()))
                .orElse(null);
    }

    private static java.util.Optional<SpellScalingTarget> target(Identifier scalingProfileId) {
        return SimplySwordsAPI.getSpellScalingDefinition(scalingProfileId)
                .map(SpellScalingDefinition::spellPowerTarget);
    }

}
