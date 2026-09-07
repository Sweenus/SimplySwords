package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.NatureSwarmMasteryTuning;
import net.sweenus.simplyswords.api.ability.NatureSwarmMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.ChompolotlMasteryManager;
import net.sweenus.simplyswords.world.NatureSwarmMasteryCombatManager;

import java.util.List;

public class ChompolotlSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public ChompolotlSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            UniqueAbilityExecution execution = NatureSwarmMasteryCombatManager.beginPassive(
                    NatureSwarmMasteryAbilities.CHOMPOLOTL_PROC, serverWorld, stack, attacker, target);
            NatureSwarmMasteryTuning tuning = NatureSwarmMasteryAbilities.tuning(execution);
            int skillCooldown = procCooldown(Config.uniqueEffects.chompolotl.cooldown, tuning);
            float skillDamage = Config.uniqueEffects.chompolotl.damageScaling;
            HelperMethods.playHitSounds(attacker, target);

            if (!ChompolotlMasteryManager.procReady(attacker) || target == null
                    || !HelperMethods.checkAbilityTarget(target, attacker)) {
                UniqueAbilityApi.cancel(execution);
                return super.postHit(stack, target, attacker);
            }
            var existing = ChompolotlMasteryManager.owned(attacker);
            int spawned = 0;
            for (int i = 0; i < passiveSummonCount(tuning); i++) {
                if (spawnAxolotl(serverWorld, attacker, target, stack, skillDamage, false, tuning,
                        Config.uniqueEffects.chompolotl.cooldown * 10) != null) spawned++;
            }
            if (spawned > 0) {
                ChompolotlMasteryManager.setProcCooldown(attacker, skillCooldown);
                rallyExisting(existing, tuning);
                UniqueAbilityApi.finish(execution, NatureSwarmMasteryAbilities.FINISH, spawned);
            } else UniqueAbilityApi.cancel(execution);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        return UniqueWeaponActiveAbility.super.startPlayerAbility(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null && context.actor() != null && context.actor().isAlive()
                && context.stack() != null && !context.stack().isEmpty()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) return false;
        LivingEntity summonTarget = context.target() != null
                && HelperMethods.checkAbilityTarget(context.target(), context.actor())
                ? context.target() : context.actor();
        UniqueAbilityExecution execution = NatureSwarmMasteryCombatManager.beginActive(
                NatureSwarmMasteryAbilities.CHOMPOLOTL_RALLY, context, Config.uniqueEffects.chompolotl.cooldown * 10);
        NatureSwarmMasteryTuning tuning = NatureSwarmMasteryAbilities.tuning(execution);
        var existing = ChompolotlMasteryManager.owned(context.actor());
        int count = activeSummonCount(tuning);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (spawnAxolotl(context.world(), context.actor(), summonTarget, context.stack(),
                    Config.uniqueEffects.chompolotl.damageScaling, true, tuning,
                    execution.cooldownTicks(Config.uniqueEffects.chompolotl.cooldown * 10)) != null) spawned++;
        }
        if (spawned > 0) {
            rallyExisting(existing, tuning);
            if (tuning.flag(1 << 22) && !tuning.flag(1 << 26)) NatureSwarmMasteryCombatManager.cleanse(context.actor(),
                    tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_CLEANSE_EFFECT_COUNT, 1),
                    tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_CLEANSE_LOCKOUT_TICKS, 400));
            UniqueAbilityApi.start(execution);
            UniqueAbilityApi.finish(execution, NatureSwarmMasteryAbilities.FINISH, spawned);
        } else UniqueAbilityApi.cancel(execution);
        return spawned > 0;
    }

    private static void rallyExisting(List<SimplySwordsAxolotlEntity> existing, NatureSwarmMasteryTuning tuning) {
        if (!tuning.flag(1 << 10)) return;
        for (SimplySwordsAxolotlEntity axolotl : existing) {
            if (axolotl.isAlive() && !axolotl.isRemoved()) axolotl.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.SPEED,
                    tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_RALLY_SPEED_DURATION_TICKS, 100),
                    tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_RALLY_SPEED_AMPLIFIER, 1)));
        }
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.chompolotl.cooldown * 10;
    }

    private static SimplySwordsAxolotlEntity spawnAxolotl(ServerWorld serverWorld, LivingEntity owner,
                                                          LivingEntity target, ItemStack stack, float skillDamage,
                                                          boolean activeSummon, NatureSwarmMasteryTuning tuning, int activeCooldown) {
        SimplySwordsAxolotlEntity axolotlEntity = EntityRegistry.SIMPLYAXOLOTLENTITY.get().spawn(
                serverWorld,
                owner.getBlockPos().up(2).offset(owner.getMovementDirection(), 3),
                SpawnReason.MOB_SUMMONED);
        if (axolotlEntity == null) {
            return null;
        }
        axolotlEntity.setTarget(target != owner ? target : null);
        axolotlEntity.setOwner(owner);
        if (activeSummon) {
            axolotlEntity.setVariant(AxolotlEntity.Variant.values()[4]);
        }
        double attackDamage = 0.5f + HelperMethods.abilityScaledDamage("nature", owner, stack,
                skillDamage, Config.uniqueEffects.chompolotl.spellScaling);
        attackDamage *= summonDamageMultiplier(activeSummon, tuning);
        EntityAttributeInstance attackAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackAttribute != null) {
            attackAttribute.setBaseValue(attackDamage);
        }
        if (tuning.flag(1 << 7)) {
            EntityAttributeInstance scale = axolotlEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
            if (scale != null) scale.setBaseValue(1.6);
        }
        if (activeSummon) {
            EntityAttributeInstance speedAttribute = axolotlEntity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.setBaseValue(2.0);
            }
            serverWorld.playSound(null, owner.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_WATER_SHOOT_IMPACT_01.get(),
                    owner.getSoundCategory(), 0.4f, 1f);
        }
        int duration = summonDuration(Config.uniqueEffects.chompolotl.duration, activeSummon, tuning);
        boolean canPerch = !(tuning.flag(1 << 8) || tuning.flag(1 << 17) || tuning.flag(1 << 26) || tuning.flag(1 << 25));
        boolean canAttack = !(tuning.flag(1 << 16) || tuning.flag(1 << 25));
        double auraRadius = tuning.flag(1 << 25) ? 0 : 16;
        int graceDuration = 200 + tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_GRACE_DURATION_BONUS_TICKS, 0);
        double shoulderAuraRadius = shoulderAuraRadius(tuning);
        axolotlEntity.configureMastery(duration,
                tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_LOW_HEALTH_THRESHOLD, 0),
                (float) Math.max(0, tuning.get(
                        NatureSwarmMasteryTuning.Setting.CHOMP_LOW_HEALTH_DAMAGE_MULTIPLIER, 1) - 1),
                splashMultiplier(tuning), splashRadius(tuning), splashCap(tuning), canPerch, canAttack,
                auraRadius, shoulderAuraRadius, graceDuration,
                8 + tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_TARGET_RANGE_BONUS, 0),
                tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_TARGET_SEARCH_CAP, 0),
                (float) Math.max(0, tuning.get(
                        NatureSwarmMasteryTuning.Setting.CHOMP_COORDINATED_DAMAGE_MULTIPLIER, 1) - 1),
                tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_HELPFUL_ABSORPTION, 0),
                tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_HELPFUL_DURATION_TICKS, 0),
                tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_HELPFUL_LOCKOUT_TICKS, 0),
                activeSummon && tuning.flag(1 << 25) && !tuning.flag(1 << 26));
        axolotlEntity.configureGuardian(tuning.flag(1 << 21) && !tuning.flag(1 << 26)
                        ? tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_GUARD_RANGE, 6) : 0,
                tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_GUARD_INCOMING_MULTIPLIER, 1),
                tuning.flag(1 << 23) && !tuning.flag(1 << 26),
                tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_RESCUE_HEALTH_THRESHOLD, 0),
                tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_RESCUE_DURATION_TICKS, 0),
                tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_RESCUE_RESISTANCE_AMPLIFIER, 0));
        axolotlEntity.configurePack(tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_PACK_RANGE, 0),
                (float) tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_PACK_DAMAGE_PER_ALLY, 0),
                tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_PACK_ALLY_CAP, 0),
                tuning.flag(1 << 6) ? tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_CHAIN_RANGE, 8) : 0,
                tuning.flag(1 << 6) ? tuning.integer(
                        NatureSwarmMasteryTuning.Setting.CHOMP_CHAIN_EXTENSION_TICKS, 60) : 0,
                activeSummon && tuning.flag(1 << 24)
                        ? tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_FIRST_BITE_REFUND_PERCENT, 25) : 0);
        axolotlEntity.configureRally(tuning.flag(1 << 15)
                        ? tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_VICTORY_KILL_COUNT, 3) : 0,
                tuning.flag(1 << 15) ? tuning.integer(
                        NatureSwarmMasteryTuning.Setting.CHOMP_VICTORY_WINDOW_TICKS, 200) : 0,
                tuning.flag(1 << 15) ? tuning.integer(
                        NatureSwarmMasteryTuning.Setting.CHOMP_VICTORY_REFUND_TICKS, 100) : 0,
                tuning.flag(1 << 14) ? tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_POUNCE_RANGE, 4) : 0,
                tuning.flag(1 << 14) ? tuning.integer(
                        NatureSwarmMasteryTuning.Setting.CHOMP_POUNCE_INTERVAL_TICKS, 40) : 0);
        axolotlEntity.configureSummon(stack, activeCooldown, tuning, activeSummon);
        ChompolotlMasteryManager.register(axolotlEntity, true);
        return axolotlEntity;
    }

    static int procCooldown(int configuredCooldown, NatureSwarmMasteryTuning tuning) {
        int cooldown = Math.max(0, configuredCooldown + tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_PROC_COOLDOWN_BONUS_TICKS, 0));
        return tuning.flag(1 << 7) ? Math.max(tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_COLOSSAL_MINIMUM_TICKS, 600),
                (int) Math.round(cooldown * tuning.get(
                NatureSwarmMasteryTuning.Setting.CHOMP_COLOSSAL_COOLDOWN_MULTIPLIER, 3))) : cooldown;
    }

    static int passiveSummonCount(NatureSwarmMasteryTuning tuning) {
        if (tuning.flag(1 << 8)) return Math.max(1, tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_RELEASE_COUNT, 4));
        return 1;
    }

    static int activeSummonCount(NatureSwarmMasteryTuning tuning) {
        if (tuning.flag(1 << 25)) return 1;
        if (tuning.flag(1 << 17)) return Math.max(1, tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_HUNTER_COUNT, 3));
        if (tuning.flag(1 << 16)) return Math.max(1, tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_BRIGADE_COUNT, 3));
        return 1;
    }

    static double summonDamageMultiplier(boolean active, NatureSwarmMasteryTuning tuning) {
        double multiplier = tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_PROC_DAMAGE_MULTIPLIER, 1);
        if (!active && tuning.flag(1 << 7)) multiplier *= tuning.get(
                NatureSwarmMasteryTuning.Setting.CHOMP_COLOSSAL_DAMAGE_MULTIPLIER, 1);
        if (!active && tuning.flag(1 << 8)) multiplier *= tuning.get(
                NatureSwarmMasteryTuning.Setting.CHOMP_RELEASE_DAMAGE_MULTIPLIER, 1);
        if (active) multiplier *= tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_BLUE_DAMAGE_MULTIPLIER, 1);
        if (active && tuning.flag(1 << 17)) multiplier *= tuning.get(
                NatureSwarmMasteryTuning.Setting.CHOMP_HUNTER_DAMAGE_MULTIPLIER, 1);
        if (active && tuning.flag(1 << 26)) multiplier *= tuning.get(
                NatureSwarmMasteryTuning.Setting.CHOMP_RAVAGER_DAMAGE_MULTIPLIER, 1);
        return multiplier;
    }

    static int summonDuration(int configuredDuration, boolean active, NatureSwarmMasteryTuning tuning) {
        int bonus = tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_LIFESPAN_BONUS_TICKS, 0)
                + (active ? tuning.integer(NatureSwarmMasteryTuning.Setting.CHOMP_BLUE_LIFESPAN_BONUS_TICKS, 0) : 0);
        int duration = configuredDuration + bonus;
        if (!active && tuning.flag(1 << 8)) duration = tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_RELEASE_LIFESPAN_TICKS, duration);
        if (active && tuning.flag(1 << 17)) duration = tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_HUNTER_LIFESPAN_TICKS, duration) + bonus;
        if (active && tuning.flag(1 << 25)) duration = tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_ETERNAL_LIFESPAN_TICKS, duration);
        if (active && tuning.flag(1 << 26)) duration = tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_RAVAGER_LIFESPAN_TICKS, duration);
        return Math.max(20, duration);
    }

    static float splashMultiplier(NatureSwarmMasteryTuning tuning) {
        return (float) (tuning.flag(1 << 7)
                ? tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_COLOSSAL_SPLASH_MULTIPLIER, 0)
                : tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_SPLASH_DAMAGE_MULTIPLIER, 0));
    }

    static double splashRadius(NatureSwarmMasteryTuning tuning) {
        return tuning.flag(1 << 7)
                ? tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_COLOSSAL_SPLASH_RADIUS, 0)
                : tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_SPLASH_RADIUS, 0);
    }

    static int splashCap(NatureSwarmMasteryTuning tuning) {
        return tuning.flag(1 << 7) ? 64 : tuning.integer(
                NatureSwarmMasteryTuning.Setting.CHOMP_SPLASH_TARGET_CAP, 0);
    }

    static double shoulderAuraRadius(NatureSwarmMasteryTuning tuning) {
        double radius = 5 + tuning.get(NatureSwarmMasteryTuning.Setting.CHOMP_SHOULDER_AURA_BONUS, 0);
        if (tuning.flag(1 << 16)) radius *= tuning.get(
                NatureSwarmMasteryTuning.Setting.CHOMP_BRIGADE_AURA_MULTIPLIER, 1);
        if (tuning.flag(1 << 25)) radius = 0;
        return radius;
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.chompolotlsworditem.tooltip7").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.chompolotl.cooldown * 10);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "nature");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.CHOMPOLOTL::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 60;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 2.52f;
        @ValidatedFloat.Restrict(min = 20f)
        public int duration = 500;
        @ValidatedFloat.Restrict(min = 0f)
        public float breedChance = 0.0266f;
        public boolean dolphinsGrace = true;

    }
}
