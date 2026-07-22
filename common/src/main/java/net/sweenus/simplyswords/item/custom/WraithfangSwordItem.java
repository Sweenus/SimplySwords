package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.WraithfangEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.LivingEntityAbilityMovementManager;

import java.util.List;

public class WraithfangSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public WraithfangSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) return super.postHit(stack, target, attacker);
        HelperMethods.playHitSounds(attacker, target);

        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return startPlayerAbility(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            itemStack = user.getStackInHand(hand);
            double[] damage = HelperMethods.getAttackFromSlot(user, itemStack, user.getActiveHand());
            WraithfangEntity wraithfangEntity = new WraithfangEntity(world, user, itemStack.copy() );
            wraithfangEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            wraithfangEntity.setYaw(user.getYaw());
            wraithfangEntity.setPitch(user.getPitch());
            wraithfangEntity.primaryBaseDamage = (float) damage[0];
            wraithfangEntity.hasLoyalty = 1;
            if (hand == Hand.OFF_HAND)
                wraithfangEntity.offhandThrow = true;
            wraithfangEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(wraithfangEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
            world.playSound(wraithfangEntity, user.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(),
                    user.getSoundCategory(), 0.1f, 1.0f);
            world.playSound(wraithfangEntity, user.getBlockPos(), SoundRegistry.DISTORTION_ARC_03.get(),
                    user.getSoundCategory(), 0.1f, 1.0f);
        }

        user.swingHand(hand);

        user.getItemCooldownManager().set(this, 1);
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (context.target() == null || !HelperMethods.checkAbilityTarget(context.target(), context.actor())) {
            return false;
        }
        LivingEntity actor = context.actor();
        WraithfangEntity wraithfangEntity = new WraithfangEntity(context.world(), actor, context.stack().copy());
        Vec3d direction = LivingEntityAbilityMovementManager.getLobbedTargetDirection(actor, context.target());
        wraithfangEntity.setVelocity(direction.x, direction.y, direction.z, 1.65F, 1.0F);
        wraithfangEntity.setYaw(actor.getYaw());
        wraithfangEntity.setPitch(actor.getPitch());
        wraithfangEntity.primaryBaseDamage = (float) Math.max(1.0, HelperMethods.getAttackFromStack(context.stack(), net.minecraft.component.type.AttributeModifierSlot.MAINHAND));
        wraithfangEntity.hasLoyalty = 0;
        wraithfangEntity.setPos(actor.getX(), actor.getEyeY() - 0.5, actor.getZ());
        wraithfangEntity.markNonReturning(80);
        context.world().spawnEntity(wraithfangEntity);
        LivingEntityAbilityMovementManager.dashTowardTarget(context.world(), actor, context.target(), 1.35, 10);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1), actor);
        context.world().playSound(wraithfangEntity, actor.getBlockPos(), SoundRegistry.DARK_SWORD_SPELL.get(), actor.getSoundCategory(), 0.1f, 1.0f);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return 20;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.OMINOUS_SPAWNING, ParticleTypes.OMINOUS_SPAWNING, ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wraithfangsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.wraithfangsworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.wraithfangsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wraithfangsworditem.tooltip5").setStyle(Styles.TEXT));
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WRAITHFANG::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int hasteAmplifier = 1;
        @ValidatedInt.Restrict(min = 10)
        public int duration = 80;
    }
}
