package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import dev.architectury.platform.Platform;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase9AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase9UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.Phase9CombatManager;

import java.util.List;

public class EnigmaSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public EnigmaSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {

            HelperMethods.playHitSounds(attacker, target);

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
        return context != null
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && context.world().getBlockState(getStandardPosition(context.actor())).isAir();
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) return false;
        UniqueAbilityExecution execution = Phase9CombatManager.beginActive(
                Phase9UniqueAbilities.ENIGMA_STORMCHASER, context,
                Config.uniqueEffects.enigma.enigmaCooldown, chaseBase());
        Phase9AbilityTuning tuning = Phase9UniqueAbilities.tuning(execution);
        UniqueAbilityExecution vortexExecution = Phase9CombatManager.beginPassive(
                Phase9UniqueAbilities.ENIGMA_VORTEX, context.world(), context.stack(),
                context.actor(), null, vortexBase());
        Phase9AbilityTuning vortex = Phase9UniqueAbilities.tuning(vortexExecution);
        Phase9CombatManager.finish(vortexExecution, 0);
        UniqueAbilityExecution auraExecution = Phase9CombatManager.beginPassive(
                Phase9UniqueAbilities.ENIGMA_TAILWIND, context.world(), context.stack(),
                context.actor(), null, Phase9AbilityTuning.EMPTY);
        Phase9AbilityTuning aura = Phase9UniqueAbilities.tuning(auraExecution);
        Phase9CombatManager.finish(auraExecution, 0);
        BattleStandardDarkEntity standard = spawnEnigmaStandard(
                context.world(), context.actor(), context.stack(), tuning, vortex, aura);
        if (standard == null) return false;
        Phase9CombatManager.scheduleFinish(context.world(), execution, 900, 0);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.enigma.enigmaCooldown;
    }

    private BlockPos getStandardPosition(LivingEntity user) {
        return user.getBlockPos().up(1).offset(user.getMovementDirection(), 2);
    }

    public static Phase9AbilityTuning chaseBase() {
        return Phase9AbilityTuning.EMPTY
                .with(Phase9AbilityTuning.Setting.RANGE, Config.uniqueEffects.enigma.enigmaChaseRadius)
                .with(Phase9AbilityTuning.Setting.SPEED, 1)
                .with(Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    public static Phase9AbilityTuning vortexBase() {
        return Phase9AbilityTuning.EMPTY
                .with(Phase9AbilityTuning.Setting.RADIUS, Config.uniqueEffects.enigma.enigmaTornadoRadius)
                .with(Phase9AbilityTuning.Setting.DURATION_TICKS, Config.uniqueEffects.enigma.enigmaOrbitTicks)
                .with(Phase9AbilityTuning.Setting.PULL_STRENGTH, 1)
                .with(Phase9AbilityTuning.Setting.KNOCKBACK, 1)
                .with(Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1);
    }

    private BattleStandardDarkEntity spawnEnigmaStandard(ServerWorld world, LivingEntity user, ItemStack stack) {
        return spawnEnigmaStandard(world, user, stack, Phase9AbilityTuning.EMPTY,
                Phase9AbilityTuning.EMPTY, Phase9AbilityTuning.EMPTY);
    }

    private BattleStandardDarkEntity spawnEnigmaStandard(ServerWorld world, LivingEntity user, ItemStack stack,
                                                          Phase9AbilityTuning tuning,
                                                          Phase9AbilityTuning vortex,
                                                          Phase9AbilityTuning aura) {
        BlockPos pos = getStandardPosition(user);
        if (!world.getBlockState(pos).isAir()) {
            return null;
        }
        world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                user.getSoundCategory(), 0.4f, 0.8f);
        BattleStandardDarkEntity banner = EntityRegistry.BATTLESTANDARDDARK.get().spawn(
                world,
                pos,
                SpawnReason.MOB_SUMMONED);
        if (banner != null) {
            banner.setVelocity(0, -1, 0);
            banner.ownerEntity = user;
            banner.abilityStack = stack.copy();
            banner.decayRate = Config.uniqueEffects.enigma.enigmaDecayRate;
            banner.standardType = "enigma";
            banner.configurePhase9(tuning, vortex, aura);
            banner.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name", user.getName()));
            banner.setCustomNameVisible(false);
            banner.setInvisible(true);
            SimplySwordsStatusEffectInstance status =  new SimplySwordsStatusEffectInstance(EffectRegistry.getReference(EffectRegistry.ELEMENTAL_VORTEX), 900, 11, false, false, false);
            status.setSourceEntity(user);
            banner.addStatusEffect(status);
        }
        return banner;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        //Drag weapon particles
        if (entity.isOnGround() && Platform.isModLoaded("bettercombat") && HelperMethods.isWalking(entity)
                && entity instanceof PlayerEntity player) {
            if (player.getMainHandStack().isOf(ItemsRegistry.ENIGMA.get())) {

                BlockState blockState = entity.getSteppingBlockState();
                ParticleEffect particleEffect = new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState);

                double bodyRadians = Math.toRadians(entity.getBodyYaw() + 180);
                Vec3d backwardDirection = new Vec3d(-Math.sin(bodyRadians), 0, Math.cos(bodyRadians)).multiply(1.1);

                double strafeRadians = Math.toRadians(entity.getBodyYaw() + 90);
                Vec3d strafeDirection = new Vec3d(-Math.sin(strafeRadians), 0, Math.cos(strafeRadians));

                Vec3d movementVector = entity.getVelocity();
                double strafeMagnitude = movementVector.dotProduct(strafeDirection.normalize());

                double pivotOffsetFactor = 3;
                Vec3d pivotOffset = strafeDirection.multiply(strafeMagnitude * pivotOffsetFactor);

                Vec3d adjustedBackwardDirection = backwardDirection.subtract(pivotOffset);

                Vec3d handPosOffset = entity.getHandPosOffset(stack.getItem());
                double particleX = entity.getX() + adjustedBackwardDirection.x + handPosOffset.getX();
                double particleY = entity.getY() + handPosOffset.getY();
                double particleZ = entity.getZ() + adjustedBackwardDirection.z + handPosOffset.getZ();

                particleY = entity.isOnGround() ? entity.getY() : particleY;

                world.addParticle(particleEffect, particleX, particleY, particleZ, 0, 0.0, 0);
                world.addParticle(ParticleTypes.POOF, particleX, particleY, particleZ, 0, 0.0, 0);

            }
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.enigmasworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.enigmasworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.enigmasworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.enigma.enigmaCooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "evocation");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.ENIGMA::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int enigmaCooldown = 800;
        @ValidatedDouble.Restrict(min = 1.0)
        public double enigmaChaseRadius = 16.0;
        @ValidatedDouble.Restrict(min = 1.0)
        public double enigmaTornadoRadius = 5.5;
        @ValidatedInt.Restrict(min = 1)
        public int enigmaOrbitTicks = 35;
        @ValidatedDouble.Restrict(min = 0.0)
        public double enigmaFlingStrength = 1.35;
        @ValidatedDouble.Restrict(min = 0.0)
        public double enigmaFlingUpwardStrength = 0.55;
        @ValidatedInt.Restrict(min = 1)
        public int enigmaDecayRate = 2;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.14f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.97f;


    }
}
