package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.MoltenParticleComponent;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class MoltenEdgeSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public MoltenEdgeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) return super.postHit(stack, target, attacker);

        int proc_chance = Config.uniqueEffects.molten_edge.chance;

        ServerWorld world = (ServerWorld) attacker.getWorld();
        HelperMethods.playHitSounds(attacker, target);
        if (attacker.getRandom().nextInt(100) <= proc_chance) {
            world.playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                    attacker.getSoundCategory(), 0.6f, 2f);
            if (attacker.getRandom().nextInt(100) <= 50) {
                if (attacker.getHealth() > 2)
                    attacker.setOnFireFor(3);
                if (attacker.getHealth() < 4)
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 3), attacker);
            } else if (attacker.getRandom().nextInt(100) > 50) {
                target.setOnFireFor(3);
            }
        }

        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        if (user.getWorld().isClient()) return super.use(world, user, hand);

        activateMoltenEdge(world, user, user.getStackInHand(hand));
        user.getItemCooldownManager().set(this, Config.uniqueEffects.molten_edge.cooldown);

        return super.use(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        activateMoltenEdge(context.world(), context.actor(), context.stack());
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.molten_edge.cooldown;
    }

    private void activateMoltenEdge(World world, LivingEntity user, ItemStack stack) {
        double radius = Config.uniqueEffects.molten_edge.radius;
        double knockbackStrength = Config.uniqueEffects.molten_edge.knockbackStrength;

        int amp = 0;
        Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                user.getX() - radius, user.getY() - radius, user.getZ() - radius);
        for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if ((entity instanceof LivingEntity le) && HelperMethods.checkAbilityTarget(le, user)) {
                amp++;
                le.setVelocity((le.getX() - user.getX()) / knockbackStrength, 0.6, (le.getZ() - user.getZ()) / knockbackStrength);
                le.setOnFireFor(3);
            }
        }
        world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ENCHANT.get(),
                user.getSoundCategory(), 0.7f, 1.5f);
        int duration = Config.uniqueEffects.molten_edge.duration * amp / 2;
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.ONSLAUGHT), duration, 0), user);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 3), user);

        stack.set(ComponentTypeRegistry.MOLTEN_PARTICLE.get(), new MoltenParticleComponent(ParticleTypes.LAVA, ParticleTypes.LAVA, ParticleTypes.LARGE_SMOKE));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && (entity instanceof PlayerEntity player) && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            int amp = 0;
            if (player.age % 40 == 0) {
                if (player.getHealth() < player.getMaxHealth() / 2 && player.getHealth() > player.getMaxHealth() / 3) {
                    amp = 1;
                    stack.set(ComponentTypeRegistry.MOLTEN_PARTICLE.get(), new MoltenParticleComponent(ParticleTypes.FALLING_LAVA, ParticleTypes.FALLING_LAVA, ParticleTypes.LARGE_SMOKE));
                } else if (player.getHealth() < player.getMaxHealth() / 3 && player.getHealth() > 2) {
                    amp = 2;
                    stack.set(ComponentTypeRegistry.MOLTEN_PARTICLE.get(), new MoltenParticleComponent(ParticleTypes.FALLING_LAVA, ParticleTypes.FALLING_LAVA, ParticleTypes.LARGE_SMOKE));
                } else if (player.getHealth() <= 2) {
                    amp = 3;
                    stack.set(ComponentTypeRegistry.MOLTEN_PARTICLE.get(), new MoltenParticleComponent(ParticleTypes.LAVA, ParticleTypes.LAVA, ParticleTypes.LARGE_SMOKE));
                } else {
                    stack.set(ComponentTypeRegistry.MOLTEN_PARTICLE.get(), MoltenParticleComponent.DEFAULT);
                }
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, amp), player);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, amp - 1), player);
            }
        }
        MoltenParticleComponent component = stack.getOrDefault(ComponentTypeRegistry.MOLTEN_PARTICLE.get(), MoltenParticleComponent.DEFAULT);
        HelperMethods.createFootfalls(entity, stack, world, component.walk(), component.sprint(), component.passive(), true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.molten_edge.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MOLTEN_EDGE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 320;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 100;
        @ValidatedFloat.Restrict(min = 0f)
        public double knockbackStrength = 5f;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 5.0;
    }
}
