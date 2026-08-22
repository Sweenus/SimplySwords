package net.sweenus.simplyswords.forge;

import dev.architectury.platform.Platform;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingDefinition;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.SpellScalingTarget;
import net.sweenus.simplyswords.compat.SpellScalingAssignments;
import net.sweenus.simplyswords.compat.SpellSchoolDisplay;
import net.sweenus.simplyswords.config.Config;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ForgeHelperMethods {
    private static final Set<Identifier> WARNED_MISSING_IRONS_SCHOOLS = ConcurrentHashMap.newKeySet();

    public static String spellSchoolDisplayKey(Identifier scalingProfileId) {
        return ironsTarget(scalingProfileId)
                .map(SpellScalingTarget::displayTranslationKey)
                .filter(key -> !key.isBlank())
                .orElse("item.simplyswords.compat.scaleEnder");
    }

    public static String spellSchoolDisplayKey(String legacySchool) {
        return spellSchoolDisplayKey(SpellScalingProfile.fromLegacyName(legacySchool).registryId());
    }

    public static float useSpellAttributeScaling(float damageModifier, LivingEntity player,
                                                 Identifier scalingProfileId) {
        if (hasManaSystem() && player != null && !player.getWorld().isClient) {
            double spellPower = player.getAttributes().hasAttribute(AttributeRegistry.SPELL_POWER.get())
                    ? player.getAttributeValue(AttributeRegistry.SPELL_POWER.get()) : 1.0;
            SchoolType school = resolveSchool(scalingProfileId);
            if (school != null) {
                return (float) (damageModifier
                        * Math.max(0.0F, Config.compatibility.ironsSpells.get().basePower.get())
                        * spellPower
                        * school.getPowerFor(player));
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
        SchoolType school = resolveSchool(scalingProfileId);
        return school == null
                ? world.getDamageSources().indirectMagic(actor, actor)
                : new DamageSource(
                        world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(school.getDamageType()),
                        actor);
    }

    public static float getAbilityMagicResistanceMultiplier(LivingEntity target, Identifier scalingProfileId) {
        SchoolType school = resolveSchool(scalingProfileId);
        return school == null ? 1.0F : DamageSources.getResist(target, school);
    }

    public static int applySpellCooldownReduction(int baseTicks, LivingEntity actor) {
        if (!hasManaSystem()) {
            return baseTicks;
        }
        double reduction = actor == null
                ? 1.0
                : actor.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION.get());
        return (int) (baseTicks * (2.0 - Utils.softCapFormula(reduction)));
    }

    public static List<Identifier> spellPowerSchoolIds() {
        return List.of();
    }

    public static List<Identifier> ironsSpellSchoolIds() {
        if (!hasManaSystem()) {
            return List.of();
        }
        return SchoolRegistry.REGISTRY.get().getKeys().stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
    }

    public static SpellSchoolDisplay activeSpellSchoolDisplay(Identifier scalingId) {
        if (hasManaSystem()) {
            SchoolType school = resolveSchool(scalingId);
            if (school != null) {
                return new SpellSchoolDisplay(school.getId(), school.getDisplayName());
            }
            Identifier fallback = SpellScalingAssignments.defaultIronsSchool(scalingId)
                    .orElse(new Identifier("irons_spellbooks", "ender"));
            return new SpellSchoolDisplay(fallback, Text.literal(fallback.toString()));
        }
        Identifier fallback = new Identifier("simplyswords", "arcane");
        return new SpellSchoolDisplay(fallback, Text.literal(fallback.toString()));
    }

    public static EntityAttribute spellPowerAttribute(Identifier scalingId) {
        return null;
    }

    private static SchoolType resolveSchool(Identifier scalingProfileId) {
        if (!hasManaSystem()) {
            return null;
        }
        Identifier configured = SpellScalingAssignments.ironsSchool(scalingProfileId).orElse(null);
        SchoolType school = configured == null ? null : SchoolRegistry.getSchool(configured);
        if (school != null) {
            return school;
        }
        if (configured != null && WARNED_MISSING_IRONS_SCHOOLS.add(configured)) {
            SimplySwords.LOGGER.warn("Unknown Iron's Spells school {}; using the default for {}", configured, scalingProfileId);
        }
        Identifier fallback = SpellScalingAssignments.defaultIronsSchool(scalingProfileId).orElse(null);
        return fallback == null ? null : SchoolRegistry.getSchool(fallback);
    }

    private static java.util.Optional<SpellScalingTarget> ironsTarget(Identifier scalingProfileId) {
        return SimplySwordsAPI.getSpellScalingDefinition(scalingProfileId)
                .map(SpellScalingDefinition::ironsTarget);
    }

    public static boolean hasManaSystem() {
        return Platform.isForge()
                && SimplySwords.passVersionCheck("irons_spellbooks", SimplySwords.minimumSpellbookVersion);
    }

    public static boolean hasMana(LivingEntity entity, float amount) {
        if (amount <= 0.0F || !hasManaSystem() || entity == null || entity.getWorld().isClient) {
            return true;
        }
        return MagicData.getPlayerMagicData(entity).getMana() >= amount;
    }

    public static void spendMana(LivingEntity entity, float amount) {
        if (amount <= 0.0F || !hasManaSystem() || entity == null || entity.getWorld().isClient) {
            return;
        }
        MagicData magicData = MagicData.getPlayerMagicData(entity);
        magicData.setMana(Math.max(0.0F, magicData.getMana() - amount));
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            IronsManaSyncCompat.sync(serverPlayer, magicData);
        }
    }
}
