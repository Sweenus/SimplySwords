package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class VolcanicFurySwordItem extends SwordItem {
    public VolcanicFurySwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    int radius = (int) (SimplySwordsConfig.getFloatValue("volcanic_fury_radius"));
    int abilityDamage = (int) (SimplySwordsConfig.getFloatValue("volcanic_fury_damage"));
    int ability_timer_max = 120;
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("volcanic_fury_cooldown"));
    int chargeChance =  (int) (SimplySwordsConfig.getFloatValue("volcanic_fury_chance"));
    double lastY;
    int ability_timer;
    int chargePower;



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

            if (attacker.getRandom().nextInt(100) <= chargeChance) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10, 1), attacker);
                target.setVelocity(target.getX() - attacker.getX(), 0.5, target.getZ() - attacker.getZ());
                target.setOnFireFor(5);
                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(), SoundCategory.PLAYERS, 0.5f, 1.2f);
                if (choose_sound <= 20 && choose_sound > 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.5f, 1.2f);
                if (choose_sound <= 30 && choose_sound > 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.5f, 1.2f);
            }

        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!user.world.isClient()) {

            if (ability_timer < 1) {
                ability_timer = ability_timer_max;
                lastY = user.getY();
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 5), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20, 8), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), user);
            } else if (ability_timer > 12) {
                ability_timer = 12;
            }

        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (ability_timer > 0) {
            if (!world.isClient && (entity instanceof PlayerEntity player)) {
                ability_timer --;


                //Player dash control
                if (ability_timer == 10) {
                    player.getItemCooldownManager().set(this, skillCooldown);
                    int choose_sound = (int) (Math.random() * 30);
                    if (choose_sound <= 10)
                        world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(), SoundCategory.PLAYERS, 0.6f, 1.2f);
                    if (choose_sound <= 20 && choose_sound > 10)
                        world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.6f, 1.2f);
                    if (choose_sound <= 30 && choose_sound > 20)
                        world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.6f, 1.2f);

                    //Damage
                    Box box = new Box(player.getX() + radius, player.getY() + radius, player.getZ() + radius, player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                    for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if (entities instanceof LivingEntity le) {

                                float choose = (float) (Math.random() * 1);
                                le.damage(DamageSource.MAGIC, abilityDamage * chargePower);
                                le.setOnFireFor(6);
                                world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(), SoundCategory.PLAYERS, 0.1f, choose);
                                chargePower = 0;
                                le.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 10, 1), player);
                                le.setVelocity(le.getX() - player.getX(), 0.5, le.getZ() - player.getZ());
                                le.setOnFireFor(5);
                            }
                        }
                    }

                }
                //Player dash end
                if (ability_timer < 5) {
                    player.setVelocity(0, 0, 0); // Stop player at end of charge
                    player.velocityModified = true;

                }

                //AOE Damage & charge control
                if (player.age % 20 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {

                    //Player charge control
                    if (ability_timer > 10) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 5), player);
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20, 5), player);
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 5), player);
                        world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_EARTH_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.8f, 0.1f * chargePower);
                        chargePower += 2;
                        if (player.getHealth() > 2 && !player.isCreative())
                            player.setHealth(player.getHealth() - 1);
                    }

                    Box box = new Box(player.getX() + radius * 8, player.getY() + radius, player.getZ() + radius * 8, player.getX() - radius * 8, player.getY() - radius, player.getZ() - radius * 8);
                    for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if (entities instanceof LivingEntity le) {

                                float choose = (float) (Math.random() * 1);

                                if (ability_timer > 12) {
                                    le.damage(DamageSource.MAGIC, abilityDamage);
                                    le.setVelocity((player.getX() - le.getX()) /10,  (player.getY() - le.getY()) /10, (player.getZ() - le.getZ()) /10);
                                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3), player);

                                }
                            }
                        }
                    }

                    double xpos = player.getX() - (radius + 1);
                    double ypos = player.getY();
                    double zpos = player.getZ() - (radius + 1);

                    for (int i = radius * 2; i > 0; i--) {
                        for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            HelperMethods.spawnParticle(world, ParticleTypes.WARPED_SPORE, xpos + i + choose,
                                    ypos + 0.4,
                                    zpos + j + choose,
                                    0, 0.1, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, xpos + i + choose,
                                    ypos + 0.1,
                                    zpos + j + choose,
                                    0, 0, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.LAVA, xpos + i + choose,
                                    ypos,
                                    zpos + j + choose,
                                    0, 0.1, 0);
                        }
                    }
                }
            }
        }


        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip3"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip6"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip7"));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip8"));
        tooltip.add(Text.translatable("item.simplyswords.volcanicfurysworditem.tooltip9"));
    }

}
