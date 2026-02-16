package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class MagibladeSwordItem extends UniqueSwordItem {
    public MagibladeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }


    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return ActionResult.FAIL;
        }
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient()) {
            if ((remainingUseTicks %5 == 0 && remainingUseTicks < getMaxUseTime(stack, user) - 5)) {
                if (remainingUseTicks < 10) {
                    onStoppedUsing(stack, world, user, remainingUseTicks);
                }

            }
            if (remainingUseTicks == getMaxUseTime(stack, user) - 1) {
                world.playSoundFromEntity(null, user,  SoundEvents.ENTITY_WARDEN_SONIC_CHARGE,
                        user.getSoundCategory(), 0.6f, 1.4f);
            }
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 40;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!user.getEntityWorld().isClient() && user instanceof  PlayerEntity player) {
            int skillCooldown = Config.uniqueEffects.magiblade.cooldown;
            float damageModifier = Config.uniqueEffects.magiblade.damageModifier;
            float damage = (float) (HelperMethods.getEntityAttackDamage(user) * damageModifier);
            float distance = Config.uniqueEffects.magiblade.sonicDistance;
            DamageSource damageSource = player.getDamageSources().playerAttack(player);

            if (remainingUseTicks < 11) {
                world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                        user.getSoundCategory(), 0.8f, 1.1f);
                HelperMethods.spawnDirectionalParticles((ServerWorld) world, ParticleTypes.SONIC_BOOM, player, 10, distance);
                HelperMethods.damageEntitiesInTrajectory((ServerWorld) world, player, distance, damage, damageSource);
                user.setVelocity(user.getRotationVector().negate().multiply(+1.1));
                user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z);
            }
        player.getItemCooldownManager().set(this.getDefaultStack(), skillCooldown);

        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.ASH, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip8").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.magibladesworditem.tooltip9").setStyle(Styles.TEXT));

        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MAGIBLADE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 35;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageModifier = 0.7f;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int repelChance = 55;
        @ValidatedFloat.Restrict(min = 1f)
        public float repelRadius = 4f;
        @ValidatedFloat.Restrict(min = 1f)
        public float sonicDistance = 16f;

    }
}
