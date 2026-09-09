package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryTuning;
import net.sweenus.simplyswords.api.ability.StormSoulMasteryAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.api.ability.UniqueAbilityPhase;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.SoulrenderAbilityManager;
import net.sweenus.simplyswords.world.SoulrenderMarkVisualManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SoulrenderSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    private static final int MODE_PALLBEARER = 4;

    public SoulrenderSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            UniqueAbilityExecution execution = UniqueAbilityApi.preparePassive(StormSoulMasteryAbilities.SOULRENDER_MARK,
                    UniqueAbilityContext.passive(world, stack, attacker, target, null), builder -> builder
                            .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY));
            UniqueAbilityApi.start(execution);
            StormSoulMasteryTuning tuning = StormSoulMasteryAbilities.tuning(execution);
            int duration = tuning.integer(StormSoulMasteryTuning.Setting.MARK_DURATION_TICKS, Config.uniqueEffects.soulrender.duration);
            int maxStacks = tuning.integer(StormSoulMasteryTuning.Setting.STACK_CAP, Config.uniqueEffects.soulrender.maxStacks);
            ParticleEffect particleSelect  = ParticleTypes.ASH;
            int particleCount = 8; // Number of particles along the line

            boolean afflicted = SoulrenderAbilityManager.isMarked(target);
            int hitChance = tuning.integer(StormSoulMasteryTuning.Setting.CHANCE,
                    Config.uniqueEffects.soulrender.chance)
                    + tuning.integer(StormSoulMasteryTuning.Setting.CHANCE_BONUS, 0);
            if (afflicted) {
                hitChance -= tuning.integer(StormSoulMasteryTuning.Setting.REPEAT_CHANCE_PENALTY, 0);
            }
            boolean guaranteed = !afflicted
                    && (tuning.integer(StormSoulMasteryTuning.Setting.MODE, 0) & MODE_PALLBEARER) != 0;
            int markRoll = attacker.getRandom().nextInt(100);
            boolean mark = guaranteed || hitChance >= 0 && markRoll <= hitChance;
            UniqueAbilityApi.reportRoll(attacker, StormSoulMasteryAbilities.SOULRENDER_MARK.id(),
                    guaranteed ? "CHANCE (guaranteed)" : "CHANCE", hitChance, markRoll, mark);
            if (mark) {
                particleSelect  = ParticleTypes.SMOKE;
                HelperMethods.spawnOrbitParticles(world, target.getPos(), particleSelect, 0.5f, particleCount);
                HelperMethods.spawnOrbitParticles(world, target.getPos().add(0,0.2,0), ParticleTypes.SOUL, 0.4f, 5);

                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_01.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);
                else if (choose_sound <= 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);
                else if (choose_sound <= 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);

                StatusEffectInstance weakness = target.getStatusEffect(StatusEffects.WEAKNESS);
                int weaknessAmplifier = weakness == null ? 0 : Math.min(1, weakness.getAmplifier() + 1);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration,
                        weaknessAmplifier), attacker);

                StatusEffectInstance slowness = target.getStatusEffect(StatusEffects.SLOWNESS);
                int granted = slowness == null
                        ? SoulrenderAbilityManager.openingStacks(world, target, tuning) : 1;
                int slownessAmplifier = slowness == null
                        ? Math.min(maxStacks, granted - 1)
                        : Math.min(maxStacks, slowness.getAmplifier() + granted);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration,
                        slownessAmplifier), attacker);

                SoulrenderMarkVisualManager.refreshMark(world, target, duration);
                SoulrenderAbilityManager.echoMark(world, attacker, target, tuning);
                UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, StormSoulMasteryAbilities.HIT,
                        target, 1, maxStacks);
            }
            int amplifier = target.hasStatusEffect(StatusEffects.SLOWNESS)
                    ? target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1 : 0;
            float bonus = (float) (Math.min(tuning.get(StormSoulMasteryTuning.Setting.MELEE_BONUS_CAP, 0),
                    amplifier * tuning.get(StormSoulMasteryTuning.Setting.MELEE_BONUS_PER_STACK, 0))
                    + SoulrenderAbilityManager.takeTitheBonus(world, attacker));
            if (bonus > 0) {
                var source = attacker.getDamageSources().indirectMagic(attacker, attacker);
                target.damage(source, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source,
                        (float) attacker.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)
                                * bonus));
            }
            UniqueAbilityApi.finish(execution, StormSoulMasteryAbilities.FINISH, amplifier);
            HelperMethods.spawnWaistHeightParticles(world, particleSelect, attacker, target, particleCount);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
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
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(StormSoulMasteryAbilities.SOULRENDER_REAP,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(StormSoulMasteryAbilities.TUNING, StormSoulMasteryTuning.EMPTY)
                        .set(StormSoulMasteryAbilities.COOLDOWN_TICKS, 0));
        int consumed = consumeSoulrenderMarks(context.world(), context.actor(), context.stack(),
                StormSoulMasteryAbilities.tuning(execution), execution);
        if (consumed > 0) SoulrenderMarkVisualManager.finishNextTick(context.world(), execution, consumed);
        return consumed > 0;
    }

    private int consumeSoulrenderMarks(ServerWorld world, LivingEntity user, ItemStack stack,
                                       StormSoulMasteryTuning tuning, UniqueAbilityExecution execution) {
        double hradius = tuning.get(StormSoulMasteryTuning.Setting.RADIUS, Config.uniqueEffects.soulrender.radius);
        int targetCap = Math.max(1, tuning.integer(StormSoulMasteryTuning.Setting.REAP_TARGET_CAP,
                Config.uniqueEffects.soulrender.targetCap));
        SoulrenderAbilityManager.reachPull(world, user, hradius, tuning);

        double stackBonus = tuning.get(StormSoulMasteryTuning.Setting.REAP_STACK_BONUS, 0);
        double stackBonusCap = tuning.get(StormSoulMasteryTuning.Setting.REAP_STACK_BONUS_CAP, 0);
        double damageMultiplier = tuning.get(StormSoulMasteryTuning.Setting.DAMAGE_MULTIPLIER, 1);
        double quietusThreshold = tuning.get(StormSoulMasteryTuning.Setting.QUIETUS_HEALTH_THRESHOLD, 0);
        int quietusPerMark = tuning.integer(StormSoulMasteryTuning.Setting.QUIETUS_ABSORPTION, 0);
        int quietusCap = tuning.integer(StormSoulMasteryTuning.Setting.QUIETUS_ABSORPTION_CAP, 0);

        SoulrenderAbilityManager.OwnerReapState reap = new SoulrenderAbilityManager.OwnerReapState();
        int healStacks = 0;
        int consumed = 0;
        int kills = 0;
        int quietusAbsorption = 0;
        float totalDealt = 0f;
        for (LivingEntity le : markedTargets(world, user, hradius, targetCap)) {
            StatusEffectInstance slowness = le.getStatusEffect(StatusEffects.SLOWNESS);
            if (slowness == null || !le.isAlive()) continue;
            int stacks = slowness.getAmplifier() + 1;
            healStacks += stacks;
            boolean lowHealth = quietusPerMark > 0 && quietusThreshold > 0
                    && le.getHealth() <= le.getMaxHealth() * quietusThreshold;

            float damage = HelperMethods.abilityScaledDamage("soul", user, stack,
                    Config.uniqueEffects.soulrender.damageScaling, Config.uniqueEffects.soulrender.spellScaling);
            SoulrenderMarkVisualManager.consumeMark(world, le, user);
            var damageSource = user.getDamageSources().indirectMagic(user, user);
            float multiplier = (float) (damageMultiplier + Math.min(stackBonusCap, stacks * stackBonus));
            float dealt = stacks * damage * multiplier;
            le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, le, damageSource, dealt));
            totalDealt += dealt;
            if (execution != null) UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT,
                    StormSoulMasteryAbilities.HIT, le, 1, dealt);
            if (!le.isAlive()) {
                kills++;
                if (execution != null) UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT,
                        StormSoulMasteryAbilities.KILL, le, 1, dealt);
                SoulrenderAbilityManager.sharedEnding(world, user, stack, le, dealt, tuning, reap);
            }
            if (lowHealth) quietusAbsorption = Math.min(quietusCap, quietusAbsorption + quietusPerMark);
            le.removeStatusEffect(StatusEffects.WEAKNESS);
            le.removeStatusEffect(StatusEffects.SLOWNESS);
            world.playSoundFromEntity(null, le, SoundRegistry.DARK_SWORD_SPELL.get(),
                    le.getSoundCategory(), 0.1f, 2f);
            consumed++;
        }
        if (healStacks > 0) {
            double ratio = (tuning.get(StormSoulMasteryTuning.Setting.HEAL_RATIO,
                    Config.uniqueEffects.soulrender.healMulti)
                    + tuning.get(StormSoulMasteryTuning.Setting.HEAL_RATIO_BONUS, 0))
                    * tuning.get(StormSoulMasteryTuning.Setting.HEAL_MULTIPLIER, 1);
            float heal = (float) (healStacks * ratio);
            if (heal > 0f && heal < 1f) heal = 1f;
            float cap = (float) tuning.get(StormSoulMasteryTuning.Setting.HEAL_CAP, 6);
            if (heal > cap) heal = cap;
            if (heal > 0f) user.heal(heal);
        }
        SoulrenderAbilityManager.recordReap(world, user, tuning, consumed, totalDealt, kills, quietusAbsorption);
        return consumed;
    }

    private List<LivingEntity> markedTargets(ServerWorld world, LivingEntity user, double radius, int limit) {
        double vradius = radius / 2.0;
        Box box = new Box(user.getX() + radius, user.getY() + vradius, user.getZ() + radius,
                user.getX() - radius, user.getY() - vradius, user.getZ() - radius);
        List<LivingEntity> found = new ArrayList<>();
        for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if (entity instanceof LivingEntity le && HelperMethods.checkAbilityTarget(le, user)
                    && SoulrenderAbilityManager.isMarked(le)) {
                found.add(le);
            }
        }
        found.sort(Comparator.comparingDouble(user::squaredDistanceTo));
        return found.size() > limit ? found.subList(0, limit) : found;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SOUL, ParticleTypes.SCULK_SOUL,
                ParticleTypes.WARPED_SPORE, true);
        if (world instanceof ServerWorld serverWorld && entity instanceof LivingEntity holder
                && (selected || holder.getOffHandStack() == stack)) {
            SoulrenderAbilityManager.tickHolder(serverWorld, holder, stack);
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULRENDER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 85;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 500;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 8;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 10.0;
        @ValidatedInt.Restrict(min = 1, max = 64)
        public int targetCap = 32;

        @ValidatedFloat.Restrict(min = 0f)
        public float healMulti = 0.5f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.26f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.31f;
    }
}
