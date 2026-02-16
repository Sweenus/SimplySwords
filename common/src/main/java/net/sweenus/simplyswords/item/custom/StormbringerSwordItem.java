package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.ParryComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class StormbringerSwordItem extends UniqueSwordItem {

    public static boolean scalesWithSpellPower;
    int radius = 3;
    int parrySuccession;

    public StormbringerSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return ActionResult.FAIL;
        }
        int ability_timer_max = Config.uniqueEffects.stormbringer.blockDuration;
        ParryComponent parryComponent = itemStack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT);
        world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_PARRY_02.get(), user.getSoundCategory(), 0.8f, (float) (0.8f * (parryComponent.parrySuccession() * 0.1)));
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, ability_timer_max, 2), user);
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient()) {
            if (remainingUseTicks <= 2)
                user.stopUsingItem();

            Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                    user.getX() - radius, user.getY() - radius, user.getZ() - radius);
            for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                //Parry attack
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                    if (le.handSwinging && remainingUseTicks > getMaxUseTime(stack, user) - Config.uniqueEffects.stormbringer.parryDuration) {
                        ParryComponent parryComponent = stack.apply(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT, ParryComponent::success);
                        user.stopUsingItem();
                        le.handSwinging = false;
                        world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_PARRY_01.get(),
                                user.getSoundCategory(), 1f, (float) (0.8f * ((parryComponent != null ? parryComponent.parrySuccession() : 0) * 0.1)));
                    }
                }
            }
        }
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient()) {
            int skillCooldown = Config.uniqueEffects.stormbringer.cooldown;
            ParryComponent parryComponent = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT);
            if (parryComponent.parried()) {
                //Damage
                Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                        user.getX() - radius, user.getY() - radius, user.getZ() - radius);
                for (Entity entities : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                    //damage & knockback
                    if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                        float choose = (float) (Math.random() * 1);
                        float abilityDamage = HelperMethods.spellScaledDamage("lightning", user, Config.uniqueEffects.stormbringer.spellScaling, Config.uniqueEffects.stormbringer.damage);
                        le.damage((ServerWorld) user.getEntityWorld(), user.getDamageSources().indirectMagic(user, user), abilityDamage + parryComponent.parrySuccession());
                        world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(),
                                le.getSoundCategory(), 0.3f, choose);
                        le.setVelocity(le.getX() - user.getX(), 0.1, le.getZ() - user.getZ());

                        //player dodge backwards
                        user.setVelocity(le.getRotationVector().multiply(+1.5));
                        user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent player flying to the heavens
                        user.velocityDirty = true;
                    }
                }
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_01.get(),
                        user.getSoundCategory(), (float) (0.2f * (parryComponent.parrySuccession() * 0.04)), 0.8f);
                if (user instanceof PlayerEntity player) player.getItemCooldownManager().set(stack, (skillCooldown / 2) + (parryComponent.parrySuccession() * 2));
                stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent.resetParry());
            } else {
                if (user instanceof PlayerEntity player) {
                    player.getItemCooldownManager().set(stack, skillCooldown);
                }
                stack.set(ComponentTypeRegistry.PARRY.get(), parryComponent.resetFull());
            }
        }
        return true;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Config.uniqueEffects.stormbringer.cooldown;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip8").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip9").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip10").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stormbringersworditem.tooltip11").setStyle(Styles.TEXT));
        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "lightning");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.STORMBRINGER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 90;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 12f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 2.3f;

        @ValidatedInt.Restrict(min = 0)
        public int blockDuration = 35;
        @ValidatedInt.Restrict(min = 0)
        public int parryDuration = 10;

    }
}
