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
import net.sweenus.simplyswords.api.ability.Phase7AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase7UniqueAbilities;
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
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;
import net.sweenus.simplyswords.world.Phase7CombatManager;

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
            UniqueAbilityExecution execution = Phase7CombatManager.beginPassive(
                    Phase7UniqueAbilities.CHOMPOLOTL_PROC, serverWorld, stack, attacker, target);
            Phase7AbilityTuning tuning = Phase7UniqueAbilities.tuning(execution);
            int skillCooldown = tuning.integer(Phase7AbilityTuning.Setting.COOLDOWN_TICKS,
                    Config.uniqueEffects.chompolotl.cooldown);
            float skillDamage = Config.uniqueEffects.chompolotl.damageScaling;
            HelperMethods.playHitSounds(attacker, target);

            boolean coolingDown = attacker instanceof PlayerEntity player
                    ? player.getItemCooldownManager().isCoolingDown(stack.getItem())
                    : WeaponAbilityCooldownManager.isCoolingDown(serverWorld, attacker, stack);
            if (!coolingDown && target != null && HelperMethods.checkAbilityTarget(target, attacker)) {
                int count = Math.max(1, tuning.integer(Phase7AbilityTuning.Setting.COUNT, 1));
                boolean spawned = false;
                for (int i = 0; i < count; i++) {
                    spawned |= spawnAxolotl(serverWorld, attacker, target, stack, skillDamage,
                            false, tuning) != null;
                }
                if (spawned) {
                    SimplySwordsAPI.setWeaponCooldown(attacker, stack, skillCooldown);
                }
            }
            UniqueAbilityApi.finish(execution, Phase7UniqueAbilities.FINISH, 0);
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
        UniqueAbilityExecution execution = Phase7CombatManager.beginActive(
                Phase7UniqueAbilities.CHOMPOLOTL_RALLY, context, Config.uniqueEffects.chompolotl.cooldown * 10);
        Phase7AbilityTuning tuning = Phase7UniqueAbilities.tuning(execution);
        if (tuning.flag(1 << 10)) {
            for (net.minecraft.entity.Entity entity : context.world().iterateEntities()) {
                if (entity instanceof SimplySwordsAxolotlEntity axolotl
                        && context.actor().getUuid().equals(axolotl.getOwnerUuid())) {
                    axolotl.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                            net.minecraft.entity.effect.StatusEffects.SPEED,
                            tuning.integer(Phase7AbilityTuning.Setting.STATUS_DURATION_TICKS, 100),
                            tuning.integer(Phase7AbilityTuning.Setting.STATUS_AMPLIFIER, 1),
                            false, true, true));
                }
            }
        }
        int count = tuning.flag(1 << 17) ? tuning.integer(Phase7AbilityTuning.Setting.COUNT, 3) : 1;
        boolean spawned = false;
        for (int i = 0; i < count; i++) {
            spawned |= spawnAxolotl(context.world(), context.actor(), summonTarget, context.stack(),
                    Config.uniqueEffects.chompolotl.damageScaling, true, tuning) != null;
        }
        if (spawned) {
            UniqueAbilityApi.start(execution);
            UniqueAbilityApi.finish(execution, Phase7UniqueAbilities.FINISH, count);
        } else {
            UniqueAbilityApi.cancel(execution);
        }
        return spawned;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.chompolotl.cooldown * 10;
    }

    private static SimplySwordsAxolotlEntity spawnAxolotl(ServerWorld serverWorld, LivingEntity owner,
                                                          LivingEntity target, ItemStack stack, float skillDamage,
                                                          boolean activeSummon, Phase7AbilityTuning tuning) {
        SimplySwordsAxolotlEntity axolotlEntity = EntityRegistry.SIMPLYAXOLOTLENTITY.get().spawn(
                serverWorld,
                owner.getBlockPos().up(2).offset(owner.getMovementDirection(), 3),
                SpawnReason.MOB_SUMMONED);
        if (axolotlEntity == null) {
            return null;
        }
        axolotlEntity.setTarget(target);
        axolotlEntity.setOwner(owner);
        if (activeSummon) {
            axolotlEntity.setVariant(AxolotlEntity.Variant.values()[4]);
        }
        double attackDamage = 0.5f + HelperMethods.abilityScaledDamage("nature", owner, stack,
                skillDamage, Config.uniqueEffects.chompolotl.spellScaling);
        attackDamage *= tuning.get(Phase7AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
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
        int duration = tuning.integer(Phase7AbilityTuning.Setting.DURATION_TICKS,
                Config.uniqueEffects.chompolotl.duration);
        boolean canPerch = !(tuning.flag(1 << 8) || tuning.flag(1 << 17) || tuning.flag(1 << 26));
        boolean canAttack = !tuning.flag(1 << 25);
        double auraRadius = tuning.get(Phase7AbilityTuning.Setting.RADIUS, 16);
        int graceDuration = tuning.integer(Phase7AbilityTuning.Setting.STATUS_DURATION_TICKS, 200);
        axolotlEntity.configureMastery(duration,
                (float) Math.max(0, tuning.get(Phase7AbilityTuning.Setting.OUTGOING_MULTIPLIER, 1) - 1),
                (float) tuning.get(Phase7AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 0),
                tuning.get(Phase7AbilityTuning.Setting.RADIUS, 0),
                tuning.integer(Phase7AbilityTuning.Setting.TARGET_CAP, 4), canPerch, canAttack,
                auraRadius, graceDuration);
        axolotlEntity.configureGuardian(tuning.flag(1 << 21) && !tuning.flag(1 << 26)
                        ? tuning.get(Phase7AbilityTuning.Setting.RANGE, 6) : 0,
                tuning.flag(1 << 23) && !tuning.flag(1 << 26));
        axolotlEntity.configurePack(tuning.get(Phase7AbilityTuning.Setting.WIDTH, 0),
                (float) tuning.get(Phase7AbilityTuning.Setting.PER_STACK_MULTIPLIER, 0),
                tuning.integer(Phase7AbilityTuning.Setting.STACK_CAP, 0),
                tuning.flag(1 << 6) ? tuning.get(Phase7AbilityTuning.Setting.RANGE, 8) : 0,
                tuning.flag(1 << 6) ? tuning.integer(Phase7AbilityTuning.Setting.DURATION_TICKS, 60) : 0,
                activeSummon && tuning.flag(1 << 24)
                        ? tuning.integer(Phase7AbilityTuning.Setting.REFUND_TICKS, 25) : 0);
        axolotlEntity.configureRally(tuning.flag(1 << 15)
                        ? tuning.integer(Phase7AbilityTuning.Setting.COUNT, 3) : 0,
                tuning.flag(1 << 15) ? tuning.integer(Phase7AbilityTuning.Setting.LOCKOUT_TICKS, 200) : 0,
                tuning.flag(1 << 15) ? tuning.integer(Phase7AbilityTuning.Setting.REFUND_TICKS, 100) : 0,
                tuning.flag(1 << 14) ? tuning.get(Phase7AbilityTuning.Setting.WIDTH, 4) : 0,
                tuning.flag(1 << 14) ? tuning.integer(Phase7AbilityTuning.Setting.INTERVAL_TICKS, 40) : 0);
        if (activeSummon && tuning.flag(1 << 22) && !tuning.flag(1 << 26)) {
            owner.getStatusEffects().stream()
                    .filter(effect -> effect.getEffectType().value().getCategory()
                            == net.minecraft.entity.effect.StatusEffectCategory.HARMFUL)
                    .findFirst().ifPresent(effect -> owner.removeStatusEffect(effect.getEffectType()));
        }
        return axolotlEntity;
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
