package net.sweenus.simplyswords.item.custom;

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
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.WickpiercerEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.RevivalWeapon;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class WickpiercerSwordItem extends UniqueSwordItem implements RevivalWeapon {
    public WickpiercerSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);

            ServerWorld world = (ServerWorld) attacker.getWorld();
            DamageSource damageSource;
            if (attacker instanceof PlayerEntity player) {
                damageSource = attacker.getDamageSources().playerAttack(player);
                double[] doubles = HelperMethods.getAttackFromSlot(player, stack, attacker.getActiveHand());
                float damageModifier = (float) doubles[0] * Config.uniqueEffects.wickpiercer.damage;

                if (attacker.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FRENZY))) {
                    target.timeUntilRegen = 0;
                    HelperMethods.decrementStatusEffect(player, EffectRegistry.getReference(EffectRegistry.FRENZY));
                    target.damage(damageSource, damageModifier);
                    //world.playSound(null, attacker.getBlockPos(), SoundRegistry.SPELL_FIRE.get(),attacker.getSoundCategory(), 0.2f, 1.9f);
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
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
            double[] doubles = HelperMethods.getAttackFromSlot(user, itemStack, user.getActiveHand());
            wickpiercerEntity.primaryBaseDamage = (float) doubles[0]  *0.5f;
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
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip8", Config.uniqueEffects.waxweaver.cooldown / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip6", Config.uniqueEffects.wickpiercer.duration / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip7").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    @Override
    public boolean canRevive(PlayerEntity player, ItemStack stack, DamageSource source) {
        return !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) &&
                !player.getItemCooldownManager().isCoolingDown(this);
    }

    @Override
    public void postRevive(PlayerEntity player, ItemStack stack, DamageSource source) {
        int skillCooldown = Config.uniqueEffects.waxweaver.cooldown;
        HelperMethods.incrementStatusEffect(player, StatusEffects.RESISTANCE, 100, 2, 3);
        player.getItemCooldownManager().set(stack.getItem(), skillCooldown);

        World world = player.getWorld();
        world.playSound(null, player.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                player.getSoundCategory(), 0.7f, 1.0f);
        world.playSound(null, player.getBlockPos(), SoundRegistry.SPELL_MISC_02.get(),
                player.getSoundCategory(), 0.8f, 1.0f);
    }

    @Override
    public float getReviveHealth(PlayerEntity player, ItemStack stack, DamageSource source) {
        return player.getMaxHealth();
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WICKPIERCER::get));
        }

        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 1.0f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 80;

    }
}
