package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class HearthflameSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public HearthflameSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    int ability_timer_max = 120;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= Config.uniqueEffects.hearthflame.chance) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10, 1), attacker);
                target.setVelocity(target.getX() - attacker.getX(), 0.5, target.getZ() - attacker.getZ());
                target.setOnFireFor(5);
                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(),
                            target.getSoundCategory(), 0.5f, 1.2f);
                if (choose_sound <= 20 && choose_sound > 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(),
                            target.getSoundCategory(), 0.5f, 1.2f);
                if (choose_sound <= 30 && choose_sound > 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                            target.getSoundCategory(), 0.5f, 1.2f);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return startPlayerAbility(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 5), user);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20, 8), user);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), user);

        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (HelperMethods.isHolding(stack, user) && user instanceof PlayerEntity player) {
            int radius = Config.uniqueEffects.hearthflame.radius;
            float abilityDamage = HelperMethods.abilityScaledDamage("fire", user, stack,
                    Config.uniqueEffects.hearthflame.damageScaling, Config.uniqueEffects.hearthflame.spellScaling);
            int chargePower = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
            AbilityMethods.tickAbilityVolcanicFury(stack, world, user, remainingUseTicks, ability_timer_max,
                    abilityDamage, Config.uniqueEffects.hearthflame.cooldown, radius, chargePower);
            if (player.age % 20 == 0) {
                stack.apply(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT, comp -> comp.add(2));
            }
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return ability_timer_max;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.CROSSBOW;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient) {
            if (user instanceof PlayerEntity player) {
                player.getItemCooldownManager().set(this, Config.uniqueEffects.hearthflame.cooldown);
            }
            int choose_sound = (int) (Math.random() * 30);
            if (choose_sound <= 10)
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(),
                        user.getSoundCategory(), 0.6f, 1.2f);
            else if (choose_sound <= 20)
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(),
                        user.getSoundCategory(), 0.6f, 1.2f);
            else if (choose_sound <= 30)
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                        user.getSoundCategory(), 0.6f, 1.2f);
            //Damage
            int radius = Config.uniqueEffects.hearthflame.radius;
            Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                    user.getX() - radius, user.getY() - radius, user.getZ() - radius);
            for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire((LivingEntity) entity, user)) {
                    float choose = (float) (Math.random() * 1);
                    float abilityDamage = HelperMethods.abilityScaledDamage("fire", user, stack,
                            Config.uniqueEffects.hearthflame.damageScaling, Config.uniqueEffects.hearthflame.spellScaling);
                    int chargePower = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
                    var damageSource = user.getDamageSources().indirectMagic(user, user);
                    le.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments((ServerWorld) world, stack, le, damageSource, abilityDamage * (chargePower * 0.3f)));
                    le.setOnFireFor(6);
                    world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(),
                            le.getSoundCategory(), 0.1f, choose);
                    stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT);
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10, 1), user);
                    le.setVelocity(le.getX() - user.getX(), 0.5, le.getZ() - user.getZ());
                }
            }
        }
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        LivingEntity actor = context.actor();
        LivingEntity target = context.target();
        if (target == null || !HelperMethods.checkAbilityTarget(target, actor)) {
            return false;
        }
        ItemStack stack = context.stack();
        int chargePower = Math.max(4, stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge());
        float abilityDamage = HelperMethods.abilityScaledDamage("fire", actor, stack,
                Config.uniqueEffects.hearthflame.damageScaling, Config.uniqueEffects.hearthflame.spellScaling);
        actor.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 3), actor);
        var damageSource = actor.getDamageSources().indirectMagic(actor, actor);
        target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(context.world(), stack, target, damageSource, abilityDamage * (chargePower * 0.3f)));
        target.setOnFireFor(6);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10, 1), actor);
        target.setVelocity(target.getX() - actor.getX(), 0.5, target.getZ() - actor.getZ());
        context.world().playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(),
                target.getSoundCategory(), 0.2f, 0.8f);
        HelperMethods.spawnOrbitParticles(context.world(), target.getPos(), ParticleTypes.LAVA, Config.uniqueEffects.hearthflame.radius, 20);
        stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT);
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.hearthflame.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip7").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.hearthflame.cooldown);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.HEARTHFLAME::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 300;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.21f;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 3;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.4f;
    }
}
