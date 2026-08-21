package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.WickpiercerEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.item.interfaces.RevivalWeapon;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;
import net.sweenus.simplyswords.world.RevivalCandleVisualManager;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

import java.util.List;

public class WickpiercerSwordItem extends UniqueSwordItem implements RevivalWeapon, UniqueWeaponActiveAbility {
    public WickpiercerSwordItem(ToolMaterial toolMaterial, Settings settings) {
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
        int baseEffectDuration = Config.uniqueEffects.wickpiercer.duration;
        int effectDuration = baseEffectDuration;
        ItemStack mainhand = user.getMainHandStack();
        ItemStack offhand  = user.getOffHandStack();
        if (mainhand.isOf(this) && offhand.isOf(this))
            effectDuration = baseEffectDuration * 2;

        world.playSound(null, user.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),
                user.getSoundCategory(), 0.1f, 1.0f);

        HelperMethods.incrementStatusEffect(user, EffectRegistry.getReference(EffectRegistry.FRENZY), effectDuration, 1, 4);

        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            itemStack = user.getStackInHand(hand);
            WickpiercerEntity wickpiercerEntity = new WickpiercerEntity(world, user, itemStack.copy() );
            wickpiercerEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            wickpiercerEntity.setYaw(user.getYaw());
            wickpiercerEntity.setPitch(user.getPitch()-90);
            wickpiercerEntity.primaryBaseDamage = HelperMethods.abilityScaledDamage("fire", user, itemStack,
                    Config.uniqueEffects.wickpiercer.throwDamageScaling,
                    Config.uniqueEffects.wickpiercer.throwSpellScaling);
            wickpiercerEntity.hasLoyalty = 3;
            if (hand == Hand.OFF_HAND)
                wickpiercerEntity.offhandThrow = true;
            wickpiercerEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(wickpiercerEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        user.swingHand(hand);

        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        LivingEntity actor = context.actor();
        HelperMethods.incrementStatusEffect(actor, EffectRegistry.getReference(EffectRegistry.FRENZY),
                Config.uniqueEffects.wickpiercer.duration, 1, 4);
        WickpiercerEntity wickpiercerEntity = new WickpiercerEntity(context.world(), actor, context.stack().copy());
        Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(actor, context.target());
        wickpiercerEntity.setVelocity(direction.x, direction.y, direction.z, 1.65F, 1.0F);
        wickpiercerEntity.setYaw(actor.getYaw());
        wickpiercerEntity.setPitch(actor.getPitch() - 90);
        wickpiercerEntity.primaryBaseDamage = HelperMethods.abilityScaledDamage("fire", actor, context.stack(),
                Config.uniqueEffects.wickpiercer.throwDamageScaling,
                Config.uniqueEffects.wickpiercer.throwSpellScaling);
        wickpiercerEntity.hasLoyalty = 0;
        wickpiercerEntity.setPos(actor.getX(), actor.getEyeY() - 0.5, actor.getZ());
        wickpiercerEntity.markNonReturning(80);
        context.world().spawnEntity(wickpiercerEntity);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.wickpiercer.cooldown);
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
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.wickpiercer.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip6", Config.uniqueEffects.wickpiercer.duration / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip7").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
    }

    @Override
    public boolean canRevive(LivingEntity entity, ItemStack stack, DamageSource source) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (entity instanceof PlayerEntity player) {
            return !player.getItemCooldownManager().isCoolingDown(this);
        }
        return entity.getWorld() instanceof ServerWorld serverWorld
                && !WeaponAbilityCooldownManager.isCoolingDown(serverWorld, entity, stack);
    }

    @Override
    public void postRevive(LivingEntity entity, ItemStack stack, DamageSource source) {
        int skillCooldown = Config.uniqueEffects.waxweaver.cooldown;
        if (entity instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            RevivalCandleVisualManager.activate(serverPlayer, stack);
        }
        SimplySwordsAPI.setWeaponCooldown(entity, stack, skillCooldown);
        HelperMethods.incrementStatusEffect(entity, StatusEffects.RESISTANCE, 100, 2, 3);

        World world = entity.getWorld();
        world.playSound(null, entity.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                entity.getSoundCategory(), 0.7f, 1.0f);
        world.playSound(null, entity.getBlockPos(), SoundRegistry.SPELL_MISC_02.get(),
                entity.getSoundCategory(), 0.8f, 1.0f);
    }

    @Override
    public float getReviveHealth(LivingEntity entity, ItemStack stack, DamageSource source) {
        return entity.getMaxHealth();
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WICKPIERCER::get));
        }

        @ValidatedInt.Restrict(min = 1)
        public int cooldown = 33;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 4.1312f;
        @ValidatedFloat.Restrict(min = 0f)
        public float throwDamageScaling = 0.4f;
        @ValidatedFloat.Restrict(min = 0f)
        public float throwSpellScaling = 2.0656f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 80;

    }
}
