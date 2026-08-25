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
import net.sweenus.simplyswords.api.ability.Phase3AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase3UniqueAbilities;
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
import net.sweenus.simplyswords.world.SoulrenderMarkVisualManager;

import java.util.List;

public class SoulrenderSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
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
            UniqueAbilityExecution execution = UniqueAbilityApi.begin(Phase3UniqueAbilities.SOULRENDER_MARK,
                    UniqueAbilityContext.passive(world, stack, attacker, target, null), builder -> builder
                            .set(Phase3UniqueAbilities.TUNING, Phase3AbilityTuning.EMPTY));
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            Phase3AbilityTuning tuning = Phase3UniqueAbilities.tuning(execution);
            int hitChance = tuning.integer(Phase3AbilityTuning.Setting.CHANCE, Config.uniqueEffects.soulrender.chance);
            int duration = tuning.integer(Phase3AbilityTuning.Setting.MARK_DURATION_TICKS, Config.uniqueEffects.soulrender.duration);
            int maxStacks = tuning.integer(Phase3AbilityTuning.Setting.STACK_CAP, Config.uniqueEffects.soulrender.maxStacks);
            ParticleEffect particleSelect  = ParticleTypes.ASH;
            int particleCount = 8; // Number of particles along the line

            boolean mark = tuning.has(Phase3AbilityTuning.Setting.CHANCE)
                    ? hitChance >= 100 || hitChance > 0 && attacker.getRandom().nextInt(100) < hitChance
                    : attacker.getRandom().nextInt(100) <= hitChance;
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
                if (weakness != null) {
                    int a = (weakness.getAmplifier() + 1);

                    if ((weakness.getAmplifier() <= 0)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, a), attacker);
                    }
                } else {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0), attacker);
                }

                StatusEffectInstance slowness = target.getStatusEffect(StatusEffects.SLOWNESS);
                if (slowness != null) {
                    int a = (slowness.getAmplifier() + 1);

                    if ((slowness.getAmplifier() < maxStacks)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, a), attacker);
                    }
                } else {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0), attacker);
                }

                SoulrenderMarkVisualManager.refreshMark(world, target, duration);
                UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT, Phase3UniqueAbilities.HIT,
                        target, 1, maxStacks);
            }
            int amplifier = target.hasStatusEffect(StatusEffects.SLOWNESS)
                    ? target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1 : 0;
            float bonus = (float) Math.min(tuning.get(Phase3AbilityTuning.Setting.MELEE_BONUS_CAP, 0),
                    amplifier * tuning.get(Phase3AbilityTuning.Setting.MELEE_BONUS_PER_STACK, 0));
            if (bonus > 0) {
                var source = attacker.getDamageSources().indirectMagic(attacker, attacker);
                target.damage(source, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, source,
                        (float) attacker.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)
                                * bonus));
            }
            UniqueAbilityApi.finish(execution, Phase3UniqueAbilities.FINISH, amplifier);
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
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && hasSoulrenderMarks(context.world(), context.actor());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(Phase3UniqueAbilities.SOULRENDER_REAP,
                UniqueAbilityContext.active(context), builder -> builder
                        .set(Phase3UniqueAbilities.TUNING, Phase3AbilityTuning.EMPTY)
                        .set(Phase3UniqueAbilities.COOLDOWN_TICKS, 0));
        int consumed = consumeSoulrenderMarks(context.world(), context.actor(), context.stack(),
                Phase3UniqueAbilities.tuning(execution), execution);
        if (consumed > 0) SoulrenderMarkVisualManager.finishNextTick(context.world(), execution, consumed);
        return consumed > 0;
    }

    private boolean hasSoulrenderMarks(ServerWorld world, LivingEntity user) {
        double hradius = Config.uniqueEffects.soulrender.radius;
        double vradius = Config.uniqueEffects.soulrender.radius / 2.0;
        Box box = new Box(user.getX() + hradius, user.getY() + vradius, user.getZ() + hradius,
                user.getX() - hradius, user.getY() - vradius, user.getZ() - hradius);
        return world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                .anyMatch(entity -> entity instanceof LivingEntity le
                        && HelperMethods.checkAbilityTarget(le, user)
                        && le.hasStatusEffect(StatusEffects.SLOWNESS)
                        && le.hasStatusEffect(StatusEffects.WEAKNESS));
    }

    private int consumeSoulrenderMarks(ServerWorld world, LivingEntity user, ItemStack stack) {
        return consumeSoulrenderMarks(world, user, stack, Phase3AbilityTuning.EMPTY, null);
    }

    private int consumeSoulrenderMarks(ServerWorld world, LivingEntity user, ItemStack stack,
                                       Phase3AbilityTuning tuning, UniqueAbilityExecution execution) {
        float healAmount = (float) tuning.get(Phase3AbilityTuning.Setting.HEAL_RATIO,
                Config.uniqueEffects.soulrender.healMulti);
        int healAmp = 0;
        int consumed = 0;
        double hradius = tuning.get(Phase3AbilityTuning.Setting.RADIUS, Config.uniqueEffects.soulrender.radius);
        double vradius = hradius / 2.0;
        Box box = new Box(user.getX() + hradius, user.getY() + vradius, user.getZ() + hradius,
                user.getX() - hradius, user.getY() - vradius, user.getZ() - hradius);

        int targetCap = tuning.has(Phase3AbilityTuning.Setting.TARGET_CAP)
                ? tuning.integer(Phase3AbilityTuning.Setting.TARGET_CAP, 64) : Integer.MAX_VALUE;
        for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if ((entity instanceof LivingEntity le) && HelperMethods.checkAbilityTarget(le, user)) {
                StatusEffectInstance slowness = le.getStatusEffect(StatusEffects.SLOWNESS);
                StatusEffectInstance weakness = le.getStatusEffect(StatusEffects.WEAKNESS);
                if (slowness == null || weakness == null) {
                    continue;
                }

                healAmp += slowness.getAmplifier();
                float damage = HelperMethods.abilityScaledDamage("soul", user, stack,
                        Config.uniqueEffects.soulrender.damageScaling, Config.uniqueEffects.soulrender.spellScaling);
                SoulrenderMarkVisualManager.consumeMark(world, le, user);
                var damageSource = user.getDamageSources().indirectMagic(user, user);
                int stacks = slowness.getAmplifier() + 1;
                float multiplier = (float) (tuning.get(Phase3AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1)
                        + Math.min(tuning.get(Phase3AbilityTuning.Setting.BONUS_CAP, 0),
                        stacks * tuning.get(Phase3AbilityTuning.Setting.PER_STACK_BONUS, 0)));
                float dealt = stacks * damage * multiplier;
                le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, le, damageSource, dealt));
                if (execution != null) UniqueAbilityApi.emit(execution, UniqueAbilityPhase.HIT,
                        Phase3UniqueAbilities.HIT, le, 1, dealt);
                le.removeStatusEffect(StatusEffects.WEAKNESS);
                le.removeStatusEffect(StatusEffects.SLOWNESS);
                world.playSoundFromEntity(null, entity, SoundRegistry.DARK_SWORD_SPELL.get(),
                        entity.getSoundCategory(), 0.1f, 2f);
                consumed++;
                if (consumed >= targetCap) break;
            }
        }
        if (healAmp > 0) {
            float heal = (float) healAmp * healAmount;
            if (heal < 1f) heal = 1f;
            else if (heal > tuning.get(Phase3AbilityTuning.Setting.HEAL_CAP, 6))
                heal = (float) tuning.get(Phase3AbilityTuning.Setting.HEAL_CAP, 6);
            user.heal(heal);
        }
        return consumed;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SOUL, ParticleTypes.SCULK_SOUL,
                ParticleTypes.WARPED_SPORE, true);
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

        @ValidatedFloat.Restrict(min = 0f)
        public float healMulti = 0.5f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.26f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.31f;
    }
}
