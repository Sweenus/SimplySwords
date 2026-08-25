package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
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
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.HivemindSwarmManager;
import net.sweenus.simplyswords.world.Phase7CombatManager;

import java.util.List;

public class HiveheartSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public HiveheartSwordItem(ToolMaterial toolMaterial, Settings settings) {
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
                    Phase7UniqueAbilities.HIVEHEART_PROC, serverWorld, stack, attacker, target);
            Phase7AbilityTuning tuning = Phase7UniqueAbilities.tuning(execution);
            int skillCooldown = tuning.integer(Phase7AbilityTuning.Setting.COOLDOWN_TICKS,
                    Config.uniqueEffects.hiveheart.cooldown);
            float skillDamage = Config.uniqueEffects.hiveheart.beeDamageScaling;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker instanceof PlayerEntity player && (!player.getItemCooldownManager().isCoolingDown(stack.getItem())
                    || tuning.flag(1 << 8))) {
                if (tuning.flag(1 << 8)) {
                    int existing = 0;
                    for (Entity entity : serverWorld.iterateEntities()) {
                        if (entity instanceof SimplySwordsBeeEntity bee && !bee.isHivemindSwarmBee()
                                && attacker.getUuid().equals(bee.getOwnerUuid())) existing++;
                    }
                    if (existing >= tuning.integer(Phase7AbilityTuning.Setting.TARGET_CAP, 4)) {
                        UniqueAbilityApi.finish(execution, Phase7UniqueAbilities.FINISH, 0);
                        return super.postHit(stack, target, attacker);
                    }
                }
                int proc = Phase7CombatManager.nextHiveProc(attacker);
                int count = tuning.flag(1 << 5) && proc == 3
                        ? Math.max(2, tuning.integer(Phase7AbilityTuning.Setting.COUNT, 2)) : 1;
                boolean spawned = false;
                for (int i = 0; i < count; i++) {
                    SimplySwordsBeeEntity beeEntity = EntityRegistry.SIMPLYBEEENTITY.get().spawn(
                            serverWorld, attacker.getBlockPos().up(4).offset(attacker.getMovementDirection(), 3),
                            SpawnReason.MOB_SUMMONED);
                if (beeEntity != null && target != null) {
                    beeEntity.setTarget(target);
                    beeEntity.setAngryAt(target.getUuid());
                    beeEntity.setAngerTime(200);
                    beeEntity.shouldAngerAt(target);
                    beeEntity.setInvulnerable(true);
                    beeEntity.setOwner(attacker);
                    beeEntity.setMasteryPoisonTicks(tuning.integer(
                            Phase7AbilityTuning.Setting.STATUS_DURATION_TICKS, 0));
                    beeEntity.setMasteryCooldownRefund(tuning.integer(
                            Phase7AbilityTuning.Setting.REFUND_TICKS, 0));
                    double attackDamage = 1 + HelperMethods.abilityScaledDamage("nature", attacker, stack,
                            skillDamage, Config.uniqueEffects.hiveheart.beeSpellScaling);
                    attackDamage *= tuning.get(Phase7AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
                    attackDamage *= tuning.get(Phase7AbilityTuning.Setting.OUTGOING_MULTIPLIER, 1);
                    if (tuning.flag(1 << 7) && target.getHealth() / target.getMaxHealth()
                            >= tuning.get(Phase7AbilityTuning.Setting.HEALTH_THRESHOLD, .35)) {
                        attackDamage /= 2.2;
                    }
                    if (count > 1) attackDamage *= tuning.get(
                            Phase7AbilityTuning.Setting.SECONDARY_DAMAGE_MULTIPLIER, 1);
                    EntityAttributeInstance attackAttribute = beeEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    if (attackAttribute != null)
                        attackAttribute.setBaseValue(attackDamage);
                    spawned = true;
                    UniqueAbilityApi.emit(execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                            Phase7UniqueAbilities.PULSE, target, 1, attackDamage);
                }
                }
                if (spawned && !tuning.flag(1 << 8)) {
                    SimplySwordsAPI.setWeaponCooldown(player, stack, skillCooldown);
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
        if (!canActivate(context)) {
            return false;
        }
        return HivemindSwarmManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.hiveheart.activeCooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_HONEY,
                ParticleTypes.LANDING_HONEY, ParticleTypes.LANDING_HONEY, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip5",
                net.sweenus.simplyswords.client.util.TooltipUtils.getEffectiveWeaponCooldownTicks(
                        itemStack, Config.uniqueEffects.hiveheart.cooldown) / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip7").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.hiveheart.activeCooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "nature");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.HIVEHEART::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 60;
        @ValidatedFloat.Restrict(min = 0f)
        public float beeDamageScaling = 0.88f;
        @ValidatedFloat.Restrict(min = 0f)
        public float beeSpellScaling = 5.903f;
        @ValidatedInt.Restrict(min = 0)
        public int activeCooldown = 300;
        @ValidatedInt.Restrict(min = 0)
        public int swarmBeeCount = 8;
        @ValidatedDouble.Restrict(min = 1.0)
        public double swarmRadius = 8.0;
        @ValidatedInt.Restrict(min = 20)
        public int swarmLifetime = 240;
        @ValidatedInt.Restrict(min = 1)
        public int stingsPerBee = 10;
        @ValidatedDouble.Restrict(min = 0.0)
        public double stingDamageScaling = 0.01;
        @ValidatedDouble.Restrict(min = 0.0)
        public double stingSpellScaling = 0.07;
        @ValidatedInt.Restrict(min = 1)
        public int stingIntervalTicks = 10;
        @ValidatedInt.Restrict(min = 0)
        public int maxSlowAmplifier = 3;

    }
}
