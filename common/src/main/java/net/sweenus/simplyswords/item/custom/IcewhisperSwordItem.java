package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.util.WeaponManaCost;
import net.sweenus.simplyswords.world.FrostfallIceSpikeFieldManager;
import net.sweenus.simplyswords.world.IcewhisperAbilityManager;
import net.sweenus.simplyswords.world.IcewhisperCometManager;
import net.sweenus.simplyswords.world.Phase4AbsorptionTracker;
import net.sweenus.simplyswords.world.Phase6CombatManager;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityManager;

import java.util.List;

public class IcewhisperSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public static final int BASE_SLOW_TICKS = 120;

    public IcewhisperSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        HelperMethods.playHitSounds(attacker, target);
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        boolean reboundInput = PlayerWeaponAbilityManager.shouldSkipDefaultAbilityUse(world, user, hand, stack);
        if (reboundInput && !WeaponManaCost.canAfford(user, stack)) return TypedActionResult.fail(stack);
        TypedActionResult<ItemStack> result = UniqueWeaponActiveAbility.super.startPlayerAbility(world, user, hand);
        if (reboundInput && result.getResult().isAccepted()) WeaponManaCost.spend(user, stack);
        return result;
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        UniqueAbilityExecution execution = Phase6CombatManager.beginActive(
                Phase6UniqueAbilities.ICEWHISPER_COMETS, context, Config.uniqueEffects.icewhisper.cooldown);
        activateIcewhisper(context.world(), context.actor(), context.stack(),
                Phase6UniqueAbilities.tuning(execution), execution);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.icewhisper.cooldown;
    }

    private static void activateIcewhisper(ServerWorld serverWorld, LivingEntity actor, ItemStack stack,
                                           Phase6AbilityTuning tuning, UniqueAbilityExecution execution) {
        double radius = stormRadius(tuning, Config.uniqueEffects.icewhisper.radius * 2.0);
        float abilityDamage = HelperMethods.abilityScaledDamage("frost", actor, stack,
                Config.uniqueEffects.icewhisper.damageScaling, Config.uniqueEffects.icewhisper.spellScaling);
        int absorption = tuning.integer(s("ICEWHISPER_WARD_ABSORPTION"), 0);
        if (tuning.flag(IcewhisperAbilityManager.MODE_FROST_WARD) && absorption > 0
                && !tuning.flag(IcewhisperAbilityManager.MODE_BLACK_ICE)) {
            Phase4AbsorptionTracker.grant(actor, absorption,
                    tuning.integer(s("ICEWHISPER_WARD_DURATION_TICKS"), 80), absorption);
        }
        IcewhisperCometManager.startStorm(serverWorld, actor, stack, radius,
                abilityDamage * Config.uniqueEffects.icewhisper.cometDamageMultiplier,
                Config.uniqueEffects.icewhisper.duration, tuning, execution);
    }

    // Radius composes against the live configuration instead of a literal the config can drift from.
    public static double stormRadius(Phase6AbilityTuning tuning, double configured) {
        return Math.max(1, (configured + tuning.get(s("ICEWHISPER_STORM_RADIUS_BONUS"), 0))
                * tuning.get(s("ICEWHISPER_STORM_RADIUS_MULTIPLIER"), 1));
    }

    public static double auraRadius(Phase6AbilityTuning tuning, double configured) {
        return Math.max(1, (configured + tuning.get(s("ICEWHISPER_AURA_RADIUS_BONUS"), 0))
                * tuning.get(s("ICEWHISPER_AURA_RADIUS_MULTIPLIER"), 1));
    }

    // Base Permafrost steps its slow up one level per pulse and stops at the resolved cap.
    public static int slowAmplifier(int current, boolean present, int cap) {
        return present ? Math.min(cap, current + 1) : 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && world instanceof ServerWorld serverWorld && entity instanceof LivingEntity user
                && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            tickPassiveAura(serverWorld, user, stack);
        }
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE,
                ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public static void tickPassiveAura(ServerWorld world, LivingEntity user, ItemStack stack) {
        if (world == null || user == null || stack == null || stack.isEmpty()
                || !net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)
                || user.age % 35 != 0) {
            return;
        }
        UniqueAbilityExecution execution = Phase6CombatManager.beginPassive(
                Phase6UniqueAbilities.ICEWHISPER_AURA, world, stack, user, null);
        Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
        double radius = auraRadius(tuning, Config.uniqueEffects.icewhisper.radius);
        long now = world.getTime();
        IcewhisperAbilityManager.onAuraPulse(world, user, tuning, radius);
        Phase4AbsorptionTracker.tick(user);
        int slowDuration = Math.max(1, BASE_SLOW_TICKS
                + tuning.integer(s("ICEWHISPER_AURA_SLOW_BONUS_TICKS"), 0));
        int amplifierCap = tuning.integer(s("ICEWHISPER_AURA_AMPLIFIER_CAP"), 2);
        double innerRadius = tuning.get(s("ICEWHISPER_INNER_RADIUS"), 0);
        int innerAmplifier = tuning.integer(s("ICEWHISPER_INNER_AMPLIFIER"), 0);
        double slowRadius = tuning.flag(IcewhisperAbilityManager.MODE_KILLING_COLD)
                ? tuning.get(s("ICEWHISPER_SLOW_RADIUS"), radius) : radius;
        int freezePerPulse = tuning.integer(s("ICEWHISPER_FREEZE_PER_PULSE_TICKS"), 0);
        int freezeCap = tuning.integer(s("ICEWHISPER_FREEZE_CAP_TICKS"), 100);
        int frozenThreshold = tuning.integer(s("ICEWHISPER_FROZEN_THRESHOLD_TICKS"), 0);
        double frozenMultiplier = tuning.get(s("ICEWHISPER_FROZEN_DAMAGE_MULTIPLIER"), 1);
        double attackSlow = tuning.get(s("ICEWHISPER_SLOW_ATTACK_MULTIPLIER"), 1);
        int attackSlowTicks = tuning.integer(s("ICEWHISPER_SLOW_ATTACK_TICKS"), 0);
        float abilityDamage = HelperMethods.abilityScaledDamage("frost", user, stack,
                Config.uniqueEffects.icewhisper.damageScaling, Config.uniqueEffects.icewhisper.spellScaling);
        float pulseDamage = abilityDamage * (float) tuning.get(s("ICEWHISPER_AURA_DAMAGE_MULTIPLIER"), 1);

        Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                user.getX() - radius, user.getY() - radius, user.getZ() - radius);
        int affected = 0;
        int cap = tuning.has(s("ICEWHISPER_AURA_TARGET_CAP"))
                ? tuning.integer(s("ICEWHISPER_AURA_TARGET_CAP"), 20) : Integer.MAX_VALUE;
        for (Entity otherEntity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (!(otherEntity instanceof LivingEntity le) || !HelperMethods.checkAbilityTarget(le, user)) {
                continue;
            }
            IcewhisperAbilityManager.markAffected(user, le, now);
            int frozenBefore = le.getFrozenTicks();
            double distance = Math.sqrt(le.squaredDistanceTo(user.getX(), le.getY(), user.getZ()));
            if (distance <= slowRadius) {
                StatusEffectInstance slowness = le.getStatusEffect(StatusEffects.SLOWNESS);
                int amplifier = innerAmplifier > 0 && distance <= innerRadius
                        ? innerAmplifier
                        : slowAmplifier(slowness != null ? slowness.getAmplifier() : 0,
                                slowness != null, amplifierCap);
                le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowDuration, amplifier), user);
                if (attackSlowTicks > 0 && attackSlow < 1) {
                    IcewhisperAbilityManager.applyAttackSlow(le, attackSlow, attackSlowTicks, now);
                }
            }
            if (freezePerPulse > 0) {
                le.setFrozenTicks(IcewhisperAbilityManager.accrueFreeze(user, le, now, freezePerPulse, freezeCap));
            }
            float choose = (float) (Math.random() * 1);
            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                    le.getSoundCategory(), 0.1f, choose);
            float damage = pulseDamage;
            if (frozenThreshold > 0 && frozenBefore > frozenThreshold) {
                damage *= (float) frozenMultiplier;
            }
            SimplySwordsAPI.applyAbilityMagicDamage(world, user, stack, le, damage, SpellScalingProfile.FROST);
            double dwell = IcewhisperAbilityManager.dwellMultiplier(user, le, now, tuning);
            if (dwell > 0 && le.isAlive()) {
                SimplySwordsAPI.applyAbilityMagicDamageThroughIframes(world, user, stack, le,
                        pulseDamage * (float) dwell, SpellScalingProfile.FROST);
            }
            FrostfallIceSpikeFieldManager.createTargetBurst(world, le.getPos(), 4, 0.9F);
            if (++affected >= cap) break;
        }
        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_02.get(),
                user.getSoundCategory(), 0.1f, 0.6f);
        int particleRadius = (int) Math.ceil(radius);
        double xpos = user.getX() - (particleRadius + 1);
        double ypos = user.getY();
        double zpos = user.getZ() - (particleRadius + 1);

        for (int i = particleRadius * 2; i > 0; i--) {
            for (int j = particleRadius * 2; j > 0; j--) {
                float choose = (float) (Math.random() * 1);
                HelperMethods.spawnParticle(world, ParticleTypes.SNOWFLAKE,
                        xpos + i + choose, ypos + 0.4, zpos + j + choose,
                        0, 0.1, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.CLOUD,
                        xpos + i + choose, ypos + 0.1, zpos + j + choose,
                        0, 0, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.WHITE_ASH,
                        xpos + i + choose, ypos + 2, zpos + j + choose,
                        0, 0, 0);
            }
        }
        UniqueAbilityApi.finish(execution, Phase6UniqueAbilities.FINISH, affected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        double radius = Config.uniqueEffects.icewhisper.radius;
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip2", radius).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.icewhisper.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "frost");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.ICEWHISPER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 450;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.06f;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 200;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 4;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.40f;
        @ValidatedInt.Restrict(min = 1)
        public int cometInterval = 14;
        @ValidatedInt.Restrict(min = 0)
        public int cometsPerWave = 2;
        @ValidatedInt.Restrict(min = 1)
        public int cometFallTicks = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float cometSplashRadius = 2.5f;
        @ValidatedFloat.Restrict(min = 0f)
        public float cometDamageMultiplier = 16.0f;
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }
}
