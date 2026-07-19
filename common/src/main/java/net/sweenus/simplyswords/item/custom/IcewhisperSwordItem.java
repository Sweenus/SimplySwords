package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.ChargedLocationComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class IcewhisperSwordItem extends UniqueSwordItem implements TwoHandedWeapon {
    public IcewhisperSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        itemStack.set(ComponentTypeRegistry.CHARGED_LOCATION.get(), new ChargedLocationComponent(user.getX(), user.getY(), user.getZ()));

        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (HelperMethods.isHolding(stack, user) && user instanceof PlayerEntity) {

            ChargedLocationComponent location = stack.getOrDefault(ComponentTypeRegistry.CHARGED_LOCATION.get(), ChargedLocationComponent.DEFAULT);
            int radius = Config.uniqueEffects.icewhisper.radius;
            float abilityDamage = HelperMethods.spellScaledDamage("frost", user, Config.uniqueEffects.icewhisper.spellScaling, Config.uniqueEffects.icewhisper.damage);

            AbilityMethods.tickAbilityPermafrost(stack, world, user, remainingUseTicks, abilityDamage,
                    radius, location.lastX(), location.lastY(), location.lastZ());
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return Config.uniqueEffects.icewhisper.duration;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && (user instanceof PlayerEntity player)) {
            int skillCooldown = Config.uniqueEffects.icewhisper.cooldown;
            player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && (entity instanceof PlayerEntity player)) {
            int radius = Config.uniqueEffects.icewhisper.radius;
            //AOE Aura
            if (player.age % 35 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                Box box = new Box(player.getX() + radius, player.getY() + radius, player.getZ() + radius,
                        player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                for (Entity otherEntity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if ((otherEntity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, player)) {
                        if (le.hasStatusEffect(StatusEffects.SLOWNESS)) {
                            int a = (le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, Math.max(a, 3)), player);
                        } else {
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 0), player);
                        }
                        float choose = (float) (Math.random() * 1);
                        world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(), le.getSoundCategory(), 0.1f, choose);
                        float abilityDamage = HelperMethods.spellScaledDamage("frost", entity, Config.uniqueEffects.icewhisper.spellScaling, Config.uniqueEffects.icewhisper.damage);
                        le.damage(player.getDamageSources().indirectMagic(entity, entity), abilityDamage);
                    }
                }
                world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_02.get(),
                        player.getSoundCategory(), 0.1f, 0.6f);
                double xpos = player.getX() - (radius + 1);
                double ypos = player.getY();
                double zpos = player.getZ() - (radius + 1);

                for (int i = radius * 2; i > 0; i--) {
                    for (int j = radius * 2; j > 0; j--) {
                        float choose = (float) (Math.random() * 1);
                        HelperMethods.spawnParticle(world, ParticleTypes.SNOWFLAKE,
                                xpos + i + choose, ypos + 0.4, zpos + j + choose,
                                0, 0.1, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.CLOUD,
                                xpos + i + choose, ypos + 0.1, zpos + j + choose,
                                0, 0, 0);
                        HelperMethods.spawnParticle(world, ParticleTypes.WHITE_ASH,
                                xpos + i + choose, ypos + 2, zpos + j + choose,
                                0, 0, 0);
                    }
                }
            }
        }
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE,
                ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        double radius = Config.uniqueEffects.icewhisper.radius;
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip2", radius).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.icewhispersworditem.tooltip4", radius * 2).setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.icewhisper.cooldown);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "frost");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.ICEWHISPER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 600;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 1f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 200;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 4;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.9f;
    }
}
