package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class MoltenEdgeSwordItem extends UniqueSwordItem {
    public MoltenEdgeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    private static DefaultParticleType particleWalk = ParticleTypes.FALLING_LAVA;
    private static DefaultParticleType particleSprint = ParticleTypes.FALLING_LAVA;
    private static DefaultParticleType particlePassive = ParticleTypes.SMOKE;

    private final int abilityCooldown = (int) SimplySwords.uniqueEffectsConfig.moltenRoarCooldown;
    int radius = (int) SimplySwords.uniqueEffectsConfig.moltenRoarRadius;
    int knockbackStrength = (int) SimplySwords.uniqueEffectsConfig.moltenRoarKnockbackStrength;
    int proc_chance = (int) SimplySwords.uniqueEffectsConfig.moltenRoarChance;
    int roar_timer_max = (int) SimplySwords.uniqueEffectsConfig.moltenRoarDuration;



    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);


            if (attacker.getRandom().nextInt(100) <= proc_chance) {
                world.playSoundFromEntity(null, attacker, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.6f, 2f);
                if (attacker.getRandom().nextInt(100) <= 50) {
                    if (attacker.getHealth() > 2)
                        attacker.setOnFireFor(3);
                    if (attacker.getHealth() < 4)
                        attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 3), attacker);
                }
                else if (attacker.getRandom().nextInt(100) > 50) {
                    target.setOnFireFor(3);
                }


            }


        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {


        if (!user.getWorld().isClient()) {
            int amp = 0;
            Box box = new Box(user.getX() + radius , user.getY() + radius, user.getZ() + radius, user.getX() - radius, user.getY() - radius, user.getZ() - radius);
            for (Entity entities : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                if (entities != null) {
                    if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                        amp ++;
                        le.setVelocity((le.getX() - user.getX()) / knockbackStrength, 0.6, (le.getZ() - user.getZ()) / knockbackStrength);
                        le.setOnFireFor(3);
                    }
                }
            }
            world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ENCHANT.get(), SoundCategory.PLAYERS, 0.7f, 1.5f);
            int duration = roar_timer_max * amp / 2;
            user.addStatusEffect(new StatusEffectInstance(EffectRegistry.ONSLAUGHT.get(), duration, 0), user);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 3), user);
            user.getItemCooldownManager().set(this, abilityCooldown);
            particlePassive = ParticleTypes.LARGE_SMOKE;
            particleWalk = ParticleTypes.LAVA;
            particleSprint = ParticleTypes.LAVA;

        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && (entity instanceof PlayerEntity player)) {
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                int amp = 0;
                if (player.age % 40 == 0) {
                    if (player.getHealth() < player.getMaxHealth() / 2 && player.getHealth() > player.getMaxHealth() / 3) {
                        amp = 1;
                        particlePassive = ParticleTypes.LARGE_SMOKE;
                        particleWalk = ParticleTypes.FALLING_LAVA;
                        particleSprint = ParticleTypes.FALLING_LAVA;
                    }
                    else if (player.getHealth() < player.getMaxHealth() / 3 && player.getHealth() > 2) {
                        amp = 2;
                        particlePassive = ParticleTypes.LARGE_SMOKE;
                        particleWalk = ParticleTypes.FALLING_LAVA;
                        particleSprint = ParticleTypes.FALLING_LAVA;
                    }
                    else if (player.getHealth() <= 2) {
                        amp = 3;
                        particlePassive = ParticleTypes.LARGE_SMOKE;
                        particleWalk = ParticleTypes.LAVA;
                        particleSprint = ParticleTypes.LAVA;
                    }
                    else {
                        particlePassive = ParticleTypes.SMOKE;
                        particleWalk = ParticleTypes.FALLING_LAVA;
                        particleSprint = ParticleTypes.FALLING_LAVA;
                    }
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, amp), player);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, amp -1), player);
                }
            }
        }
        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, particleWalk, particleSprint, particlePassive, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }



    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip7", roar_timer_max /20).setStyle(TEXT));
        tooltip.add(Text.literal(""));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);

    }

}
