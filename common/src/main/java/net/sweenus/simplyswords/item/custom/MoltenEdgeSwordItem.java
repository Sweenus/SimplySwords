package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class MoltenEdgeSwordItem extends SwordItem {
    public MoltenEdgeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    private static DefaultParticleType particleWalk = ParticleTypes.FALLING_LAVA;
    private static DefaultParticleType particleSprint = ParticleTypes.FALLING_LAVA;
    private static DefaultParticleType particlePassive = ParticleTypes.SMOKE;

    private final int abilityCooldown = (int) SimplySwordsConfig.getFloatValue("moltenroar_cooldown");
    int radius = (int) (SimplySwordsConfig.getFloatValue("moltenroar_radius"));
    int knockbackStrength = (int) (SimplySwordsConfig.getFloatValue("moltenroar_knockback_strength"));
    int proc_chance = (int) (SimplySwordsConfig.getFloatValue("moltenroar_chance"));
    int roar_timer_max = (int) (SimplySwordsConfig.getFloatValue("moltenroar_duration"));



    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            boolean impactsounds_enabled = (SimplySwordsConfig.getBooleanValue("enable_weapon_impact_sounds"));

            if (impactsounds_enabled) {
                int choose_sound = (int) (Math.random() * 30);
                float choose_pitch = (float) Math.random() * 2;
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 20 && choose_sound > 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 30 && choose_sound > 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 40 && choose_sound > 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
            }


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


        if (!user.world.isClient()) {
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
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, roar_timer_max, amp), user);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, roar_timer_max, amp), user);
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

        //1.19

        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.moltenedgesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.moltenedgesworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.moltenedgesworditem.tooltip3"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.moltenedgesworditem.tooltip4"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.moltenedgesworditem.tooltip5"));
        tooltip.add(new TranslatableText("item.simplyswords.moltenedgesworditem.tooltip6"));
        tooltip.add(new TranslatableText("item.simplyswords.moltenedgesworditem.tooltip7", roar_timer_max /20));
        tooltip.add(new LiteralText(""));

    }

}
