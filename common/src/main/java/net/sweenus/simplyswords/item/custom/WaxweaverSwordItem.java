package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.RevivalWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.RevivalCandleVisualManager;
import net.sweenus.simplyswords.world.RevivalCooldownManager;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.NatureSwarmMasteryTuning;
import net.sweenus.simplyswords.api.ability.NatureSwarmMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.world.NatureSwarmMasteryCombatManager;

import java.util.Comparator;
import java.util.List;

public class WaxweaverSwordItem extends UniqueSwordItem implements RevivalWeapon, UniqueWeaponActiveAbility {
    public WaxweaverSwordItem(ToolMaterial toolMaterial, Settings settings) {
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
                    NatureSwarmMasteryAbilities.WAXWEAVER_TEMPO, serverWorld, stack, attacker, target);
            NatureSwarmMasteryTuning tuning = NatureSwarmMasteryAbilities.tuning(execution);
            int maximumStacks = Math.max(1, tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_TEMPO_STACK_CAP,
                    Config.uniqueEffects.waxweaver.maxStacks));
            int duration = tempoDuration(tuning);
            HelperMethods.playHitSounds(attacker, target);

            if (target.isOnFire()) {
                StatusEffectInstance previousHaste = attacker.getStatusEffect(StatusEffects.HASTE);
                int previousStacks = previousHaste == null ? 0
                        : Math.min(maximumStacks, previousHaste.getAmplifier());
                int stacks = nextTempoStacks(previousStacks, maximumStacks);
                attacker.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.STRENGTH, duration, stacks, false, false, true));
                attacker.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.HASTE, duration, stacks, false, false, true));
                boolean gainedStack = stacks > previousStacks;
                boolean reachedMaximum = previousStacks < maximumStacks && stacks >= maximumStacks;
                if (reachedMaximum) {
                    WaxweaverEncasementManager.primeFlashWax(attacker, tuning);
                }
                if (stacks >= maximumStacks && tuning.flag(1 << 12)) {
                    NatureSwarmMasteryCombatManager.applyWaxRhythm(serverWorld, attacker, duration,
                            tuning.get(NatureSwarmMasteryTuning.Setting.WAX_RHYTHM_SPEED_BONUS, .08));
                }
                if (tuning.flag(1 << 16)) {
                    NatureSwarmMasteryCombatManager.applyWaxFrenzy(serverWorld, attacker, duration, stacks,
                            tuning.get(NatureSwarmMasteryTuning.Setting.WAX_FRENZY_BONUS_MULTIPLIER, 1.5));
                } else {
                    NatureSwarmMasteryCombatManager.clearWaxFrenzy(attacker);
                }
                int speedDuration = tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_CANDLE_STEP_TICKS, 0);
                if (gainedStack && speedDuration > 0) {
                    attacker.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                            StatusEffects.SPEED, speedDuration, 0, false, true, true));
                }
                WaxweaverEncasementManager.reduceActiveCooldown(attacker, stack, tuning);
            }
            if (WaxweaverEncasementManager.isEncased(target)) {
                int fireTicks = tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_ENCASED_FIRE_TICKS, 0);
                if (fireTicks > 0) target.setOnFireFor((fireTicks + 19) / 20);
            }
            UniqueAbilityApi.finish(execution, NatureSwarmMasteryAbilities.FINISH, target.isOnFire() ? 1 : 0);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean canRevive(LivingEntity entity, ItemStack stack, DamageSource source) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return entity.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld
                && !RevivalCooldownManager.isCoolingDown(serverWorld, entity, stack);
    }

    @Override
    public void postRevive(LivingEntity entity, ItemStack stack, DamageSource source) {
        ServerWorld serverWorld = (ServerWorld) entity.getWorld();
        UniqueAbilityExecution execution = NatureSwarmMasteryCombatManager.beginPassive(
                NatureSwarmMasteryAbilities.WAXWEAVER_REVIVAL, serverWorld, stack, entity, null);
        NatureSwarmMasteryTuning tuning = NatureSwarmMasteryAbilities.tuning(execution);
        int skillCooldown = revivalCooldown(Config.uniqueEffects.waxweaver.cooldown, tuning);
        if (entity instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            RevivalCandleVisualManager.activate(serverPlayer, stack);
        }
        RevivalCooldownManager.setCooldown(serverWorld, entity, stack, skillCooldown);
        int resistanceDuration = resistanceDuration(tuning);
        if (resistanceDuration > 0) {
            int amplifier = tuning.flag(1 << 25)
                    ? tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_QUEEN_RESISTANCE_AMPLIFIER, 3) : 2;
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.RESISTANCE, resistanceDuration, amplifier, false, true, true));
        }
        double radius = tuning.get(NatureSwarmMasteryTuning.Setting.WAX_MOLTEN_RADIUS, 0);
        int fireTicks = tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_MOLTEN_FIRE_TICKS, 0);
        if (radius > 0 && fireTicks > 0) {
            serverWorld.getEntitiesByClass(LivingEntity.class, entity.getBoundingBox().expand(radius),
                            target -> target != entity && target.squaredDistanceTo(entity) <= radius * radius
                                    && HelperMethods.checkAbilityTarget(target, entity))
                    .stream().sorted(Comparator.comparingDouble(entity::squaredDistanceTo))
                    .limit(tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_MOLTEN_TARGET_CAP, 8))
                    .forEach(target -> target.setOnFireFor((fireTicks + 19) / 20));
        }
        if (tuning.flag(1 << 24)) {
            NatureSwarmMasteryCombatManager.scheduleSecondSkin(serverWorld, entity, resistanceDuration,
                    tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_SECOND_SKIN_DURATION_TICKS, 100),
                    tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_SECOND_SKIN_ABSORPTION, 6));
        }
        int affected = tuning.flag(1 << 26)
                ? WaxweaverEncasementManager.detonateRevival(
                serverWorld, entity, stack, tuning, execution) : 1;
        UniqueAbilityApi.finish(execution, NatureSwarmMasteryAbilities.FINISH, affected);

        World world = entity.getWorld();
        world.playSound(null, entity.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                entity.getSoundCategory(), 0.7f, 1.0f);
        world.playSound(null, entity.getBlockPos(), SoundRegistry.SPELL_MISC_02.get(),
                entity.getSoundCategory(), 0.8f, 1.0f);
    }

    @Override
    public float getReviveHealth(LivingEntity entity, ItemStack stack, DamageSource source) {
        if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return entity.getMaxHealth();
        UniqueAbilityExecution execution = NatureSwarmMasteryCombatManager.beginPassive(
                NatureSwarmMasteryAbilities.WAXWEAVER_REVIVAL, serverWorld, stack, entity, null);
        float health = entity.getMaxHealth() * (float) NatureSwarmMasteryAbilities.tuning(execution)
                .get(NatureSwarmMasteryTuning.Setting.WAX_QUEEN_HEALTH_THRESHOLD, 1);
        UniqueAbilityApi.finish(execution, NatureSwarmMasteryAbilities.FINISH, 0);
        return health;
    }

    public static int tempoDuration(NatureSwarmMasteryTuning tuning) {
        int base = tuning.flag(1 << 16)
                ? tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_FRENZY_DURATION_TICKS, 30)
                : tuning.flag(1 << 17)
                ? tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_PATIENT_DURATION_TICKS, 160) : 60;
        return Math.max(1, base + tuning.integer(
                NatureSwarmMasteryTuning.Setting.WAX_TEMPO_DURATION_BONUS_TICKS, 0));
    }

    public static int nextTempoStacks(int current, int maximum) {
        return Math.min(Math.max(1, maximum), Math.max(0, current) + 1);
    }

    public static int revivalCooldown(int configured, NatureSwarmMasteryTuning tuning) {
        return Math.max(0, configured
                + tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_REVIVE_COOLDOWN_BONUS_TICKS, 0)
                + tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_QUEEN_COOLDOWN_BONUS_TICKS, 0));
    }

    public static int resistanceDuration(NatureSwarmMasteryTuning tuning) {
        if (tuning.flag(1 << 26)) return 0;
        int base = tuning.flag(1 << 25)
                ? tuning.integer(NatureSwarmMasteryTuning.Setting.WAX_QUEEN_RESISTANCE_TICKS, 100) : 100;
        return Math.max(0, base + tuning.integer(
                NatureSwarmMasteryTuning.Setting.WAX_REVIVE_RESISTANCE_BONUS_TICKS, 0));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return WaxweaverEncasementManager.canStart(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return WaxweaverEncasementManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.waxweaver.activeCooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.WHITE_ASH,
                ParticleTypes.WHITE_ASH, ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip8",
                net.sweenus.simplyswords.client.util.TooltipUtils.getEffectiveWeaponCooldownTicks(
                        itemStack, Config.uniqueEffects.waxweaver.cooldown) / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip5").setStyle(Styles.TEXT));

        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.waxweaver.activeCooldown);

        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WAXWEAVER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 1200;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 3;
        @ValidatedInt.Restrict(min = 0)
        public int activeCooldown = 260;
        @ValidatedFloat.Restrict(min = 1.0F)
        public float targetRange = 12.0F;
        @ValidatedInt.Restrict(min = 1)
        public int encasementDuration = 120;
        @ValidatedFloat.Restrict(min = 0.1F)
        public float maximumTargetWidth = 0.9F;
        @ValidatedFloat.Restrict(min = 0.1F)
        public float maximumTargetHeight = 2.1F;
        @ValidatedInt.Restrict(min = 1)
        public int tauntInterval = 10;
        @ValidatedFloat.Restrict(min = 1.0F)
        public float tauntRadius = 10.0F;
        @ValidatedInt.Restrict(min = 0)
        public int tauntMaxTargets = 10;
        @ValidatedInt.Restrict(min = 1)
        @ValidatedFloat.Restrict(min = 0.0F)
        public float explosionDamageScaling = 0.55F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float spellScaling = 6.196F;
        @ValidatedFloat.Restrict(min = 0.5F)
        public float explosionRadius = 4.0F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float explosionKnockback = 0.4F;
        @ValidatedInt.Restrict(min = 0)
        public int explosionIgniteSeconds = 4;

    }
}
