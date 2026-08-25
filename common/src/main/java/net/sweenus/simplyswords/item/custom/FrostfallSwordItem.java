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
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.ability.Phase6AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase6UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.FrostfallEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;
import net.sweenus.simplyswords.world.Phase6CombatManager;

import java.util.List;

public class FrostfallSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public FrostfallSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (attacker.getWorld().isClient()) super.postHit(stack, target, attacker);
        HelperMethods.playHitSounds(attacker, target);
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        if (user.getWorld().isClient()) return super.use(world, user, hand);

        ItemStack itemStack = user.getStackInHand(hand);
        int cooldown = Config.uniqueEffects.frostfall.cooldown;
        if (!world.isClient) {
            itemStack = user.getStackInHand(hand);
            var serverWorld = (net.minecraft.server.world.ServerWorld) world;
            WeaponAbilityContext context = WeaponAbilityContext.of(serverWorld, itemStack, user,
                    user instanceof net.minecraft.server.network.ServerPlayerEntity player ? player : null,
                    null, hand, WeaponAbilityActivationSource.PLAYER);
            UniqueAbilityExecution execution = Phase6CombatManager.beginActive(
                    Phase6UniqueAbilities.FROSTFALL_THROW, context, Config.uniqueEffects.frostfall.cooldown);
            UniqueAbilityApi.takeStartedExecution();
            UniqueAbilityApi.start(execution);
            Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
            cooldown = tuning.integer(s("COOLDOWN_TICKS"), cooldown);
            FrostfallEntity frostfallEntity = createFrostfallEntity(world, user, itemStack.copy(), tuning, execution);
            if (hand == Hand.OFF_HAND)
                frostfallEntity.offhandThrow = true;
            world.spawnEntity(frostfallEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        user.swingHand(hand);


        SimplySwordsAPI.setWeaponCooldown(user, itemStack, cooldown);
        return TypedActionResult.success(itemStack, world.isClient());
    }

    private static FrostfallEntity createFrostfallEntity(World world, LivingEntity user, ItemStack stack,
                                                         Phase6AbilityTuning tuning,
                                                         UniqueAbilityExecution execution) {
        float abilityDamage = HelperMethods.abilityScaledDamage("frost", user, stack,
                Config.uniqueEffects.frostfall.damageScaling, Config.uniqueEffects.frostfall.spellScaling);
        float pulseDamage = HelperMethods.abilityScaledDamage("frost", user, stack,
                Config.uniqueEffects.frostfall.pulseDamageScaling, Config.uniqueEffects.frostfall.spellScaling);
        FrostfallEntity frostfallEntity = new FrostfallEntity(world, user, stack);
        frostfallEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
        frostfallEntity.setYaw(user.getYaw());
        frostfallEntity.setPitch(user.getPitch());
        frostfallEntity.primaryBaseDamage = abilityDamage * (float) tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        frostfallEntity.detonateDamage = pulseDamage * (float) tuning.get(s("DAMAGE_MULTIPLIER"), 1);
        frostfallEntity.addedChance = Config.uniqueEffects.frostfall.chance;
        frostfallEntity.detonateRadius = tuning.get(s("RADIUS"), Config.uniqueEffects.frostfall.radius);
        frostfallEntity.duration = tuning.integer(s("STATUS_DURATION_TICKS"), Config.uniqueEffects.frostfall.duration);
        frostfallEntity.setMastery(tuning, execution);
        frostfallEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
        return frostfallEntity;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        UniqueAbilityExecution execution = Phase6CombatManager.beginActive(
                Phase6UniqueAbilities.FROSTFALL_THROW, context, Config.uniqueEffects.frostfall.cooldown);
        Phase6AbilityTuning tuning = Phase6UniqueAbilities.tuning(execution);
        FrostfallEntity frostfallEntity = createFrostfallEntity(context.world(), context.actor(),
                context.stack().copy(), tuning, execution);
        Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(context.actor(), context.target());
        frostfallEntity.setVelocity(direction.x, direction.y, direction.z, 1.65F, 1.0F);
        frostfallEntity.setYaw(context.actor().getYaw());
        frostfallEntity.setPitch(context.actor().getPitch());
        frostfallEntity.markNonReturning(Config.uniqueEffects.frostfall.duration + 80);
        context.world().spawnEntity(frostfallEntity);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.frostfall.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE,
                ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.frostfall.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "frost");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.FROSTFALL::get));
        }

        @ValidatedInt.Restrict(min = 1, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 1)
        public int cooldown = 60;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float pulseDamageScaling = 0.8f;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 80;
        @ValidatedDouble.Restrict(min = 6.0)
        public double radius = 8.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 4.61f;
    }

    private static Phase6AbilityTuning.Setting s(String name) {
        return Phase6AbilityTuning.Setting.valueOf(name);
    }
}
