package net.sweenus.simplyswords.neoforge;

import dev.architectury.platform.Platform;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.entity.LivingEntity;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingDefinition;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.SpellScalingTarget;
import net.sweenus.simplyswords.config.Config;

public class ForgeHelperMethods {
    public static String spellSchoolDisplayKey(Identifier scalingProfileId) {
        return activeTarget(scalingProfileId)
                .map(SpellScalingTarget::displayTranslationKey)
                .filter(key -> !key.isBlank())
                .orElse("item.simplyswords.compat.scaleEnder");
    }

    public static String spellSchoolDisplayKey(String legacySchool) {
        return spellSchoolDisplayKey(SpellScalingProfile.fromLegacyName(legacySchool).registryId());
    }

    public static float useSpellAttributeScaling(float damageModifier, LivingEntity player, Identifier scalingProfileId) {
        if (player != null && !player.getWorld().isClient) {
            if (hasManaSystem()) {
                double spellPower = player.getAttributes().hasAttribute(AttributeRegistry.SPELL_POWER) ? player.getAttributeValue(AttributeRegistry.SPELL_POWER) : 1.f;
                SchoolType school = resolveSchool(scalingProfileId);
                if (school != null) {
                    return (float) (damageModifier
                            * Math.max(0.f, Config.compatibility.ironsSpells.get().basePower.get())
                            * spellPower
                            * school.getPowerFor(player));
                }
            }
            if (hasSpellPowerSystem()) {
                return NeoForgeSpellPowerCompat.scale(damageModifier, player, scalingProfileId);
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
                : world.getDamageSources().create(school.getDamageType(), actor);
    }

    public static float getAbilityMagicResistanceMultiplier(LivingEntity target, Identifier scalingProfileId) {
        SchoolType school = resolveSchool(scalingProfileId);
        return school == null ? 1.0F : DamageSources.getResist(target, school);
    }

    public static int applySpellCooldownReduction(int baseTicks, LivingEntity actor) {
        return hasManaSystem() ? Utils.applyCooldownReduction(baseTicks, actor) : baseTicks;
    }

    private static SchoolType resolveSchool(Identifier scalingProfileId) {
        if (!hasManaSystem()) {
            return null;
        }
        return ironsTarget(scalingProfileId)
                .map(target -> SchoolRegistry.getSchool(target.schoolId()))
                .orElse(null);
    }

    private static java.util.Optional<SpellScalingTarget> activeTarget(Identifier scalingProfileId) {
        return hasManaSystem() ? ironsTarget(scalingProfileId) : spellPowerTarget(scalingProfileId);
    }

    private static java.util.Optional<SpellScalingTarget> ironsTarget(Identifier scalingProfileId) {
        return SimplySwordsAPI.getSpellScalingDefinition(scalingProfileId)
                .map(SpellScalingDefinition::ironsTarget);
    }

    private static java.util.Optional<SpellScalingTarget> spellPowerTarget(Identifier scalingProfileId) {
        return SimplySwordsAPI.getSpellScalingDefinition(scalingProfileId)
                .map(SpellScalingDefinition::spellPowerTarget);
    }

    public static boolean hasManaSystem() {
        return Platform.isForgeLike()
                && SimplySwords.passVersionCheck("irons_spellbooks", SimplySwords.minimumSpellbookVersion);
    }

    public static boolean hasSpellPowerSystem() {
        return Platform.isForgeLike()
                && SimplySwords.passVersionCheck("spell_power", SimplySwords.minimumSpellPowerVersion);
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
        // MagicData has no internal sync, so the client HUD needs the packet explicitly.
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(magicData));
        }
    }
}
