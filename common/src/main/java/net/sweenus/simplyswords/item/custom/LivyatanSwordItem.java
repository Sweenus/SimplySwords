package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.LivyatanEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;
import net.sweenus.simplyswords.world.LivyatanWaveManager;

import java.util.List;

public class LivyatanSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public LivyatanSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (attacker.getWorld().isClient()) return super.postHit(stack, target, attacker);
        HelperMethods.playHitSounds(attacker, target);

        return super.postHit(stack, target, attacker);
    }

    public void onSwing(ItemStack stack, ServerWorld world, LivingEntity user, Hand hand) {
        LivyatanWaveManager.tryFire(world, user, stack);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        float abilityDamage = HelperMethods.abilityScaledDamage("frost", user, itemStack,
                Config.uniqueEffects.livyatan.damageScaling, Config.uniqueEffects.livyatan.spellScaling);
        int duration = Config.uniqueEffects.livyatan.duration;
        float returnDamage = HelperMethods.abilityScaledDamage("frost", user, itemStack,
                Config.uniqueEffects.livyatan.returnDamageScaling,
                Config.uniqueEffects.livyatan.returnSpellScaling);
        double radius = Config.uniqueEffects.livyatan.radius;
        if (!world.isClient) {
            itemStack = user.getStackInHand(hand);
            LivyatanEntity livyatanEntity = new LivyatanEntity(world, user, itemStack.copy() );
            livyatanEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            livyatanEntity.setYaw(user.getYaw());
            livyatanEntity.setPitch(user.getPitch());
            livyatanEntity.primaryBaseDamage = abilityDamage;
            livyatanEntity.slownessDuration = duration;
            livyatanEntity.primaryReturnDamage = returnDamage;
            livyatanEntity.primaryReturnDamageRadius = radius;
            if (hand == Hand.OFF_HAND)
                livyatanEntity.offhandThrow = true;
            livyatanEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(livyatanEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        user.swingHand(hand);

        SimplySwordsAPI.setWeaponCooldown(user, itemStack, 1);
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        float abilityDamage = HelperMethods.abilityScaledDamage("frost", context.actor(), context.stack(),
                Config.uniqueEffects.livyatan.damageScaling, Config.uniqueEffects.livyatan.spellScaling);
        LivyatanEntity livyatanEntity = new LivyatanEntity(context.world(), context.actor(), context.stack().copy());
        Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(context.actor(), context.target());
        livyatanEntity.setVelocity(direction.x, direction.y, direction.z, 1.65F, 1.0F);
        livyatanEntity.setYaw(context.actor().getYaw());
        livyatanEntity.setPitch(context.actor().getPitch());
        livyatanEntity.primaryBaseDamage = abilityDamage;
        livyatanEntity.slownessDuration = Config.uniqueEffects.livyatan.duration;
        livyatanEntity.primaryReturnDamage = HelperMethods.abilityScaledDamage("frost", context.actor(), context.stack(),
                Config.uniqueEffects.livyatan.returnDamageScaling,
                Config.uniqueEffects.livyatan.returnSpellScaling);
        livyatanEntity.primaryReturnDamageRadius = Config.uniqueEffects.livyatan.radius;
        livyatanEntity.setPos(context.actor().getX(), context.actor().getEyeY() - 0.5, context.actor().getZ());
        livyatanEntity.markNonReturning(Config.uniqueEffects.livyatan.duration + 80);
        context.world().spawnEntity(livyatanEntity);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.livyatan.cooldown);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "frost");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.LIVYATAN::get));
        }

        @ValidatedFloat.Restrict(min = 0)
        public int cooldown = 65;
        public float returnDamageScaling = 0.64f;
        @ValidatedFloat.Restrict(min = 0)
        public float returnSpellScaling = 3.32f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.64f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 100;
        @ValidatedDouble.Restrict(min = 0.5)
        public double radius = 6.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 3.32f;

        @ValidatedFloat.Restrict(min = 0f)
        public float waveDamageScaling = 0.64f;
        @ValidatedInt.Restrict(min = 1)
        public int swingWaveMinimumCooldownTicks = 2;
        @ValidatedDouble.Restrict(min = 0.5)
        public double waveWidthBlocks = 5.0;
        @ValidatedDouble.Restrict(min = 0.25)
        public double waveSegmentThickness = 1.25;
        @ValidatedDouble.Restrict(min = 0.1)
        public double waveStepDistance = 0.8;
        @ValidatedInt.Restrict(min = 1)
        public int waveStepIntervalTicks = 1;
        @ValidatedDouble.Restrict(min = 0.1)
        public double waveForwardStartOffset = 1.2;
        @ValidatedInt.Restrict(min = 1)
        public int waveLengthSteps = 7;
        @ValidatedDouble.Restrict(min = 0.0)
        public double waveKnockback = 0.52;
        @ValidatedDouble.Restrict(min = 0.0)
        public double waveKnockUp = 0.14;

        @ValidatedDouble.Restrict(min = 0.0)
        public double returnWavePullStrength = 0.42;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int returnLightningChance = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float returnLightningDamageScaling = 0.35f;
        @ValidatedFloat.Restrict(min = 0f)
        public float returnLightningSpellScaling = 1.82f;
        @ValidatedDouble.Restrict(min = 1.0)
        public double returnLightningSkyHeight = 12.0;
    }
}
