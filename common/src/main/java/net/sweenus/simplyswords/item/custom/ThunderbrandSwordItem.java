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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class ThunderbrandSwordItem extends SwordItem {
    public ThunderbrandSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    int radius = (int) (SimplySwordsConfig.getFloatValue("thunderblitz_radius"));
    int abilityDamage = (int) (SimplySwordsConfig.getFloatValue("thunderblitz_damage"));
    int ability_timer_max = 50;
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("thunderblitz_cooldown"));
    int chargeChance =  (int) (SimplySwordsConfig.getFloatValue("thunderblitz_chance"));
    double lastY;
    int ability_timer;



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

            if (attacker.getRandom().nextInt(100) <= chargeChance && (attacker instanceof PlayerEntity player) && player.getItemCooldownManager().getCooldownProgress(this, 1f) > 0) {
                player.getItemCooldownManager().set(this, 0);
                world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_BLOCK_01.get(), SoundCategory.PLAYERS, 0.7f, 1f);
            }
        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!user.world.isClient()) {

            if (ability_timer < 1) {
                ability_timer = ability_timer_max;
                world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_CHARGE_LONG_VERSION.get(), SoundCategory.PLAYERS, 0.4f, 0.6f);
                lastY = user.getY();
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 3), user);
                user.getItemCooldownManager().set(this, skillCooldown);
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
                if (ability_timer > (ability_timer_max - 42) && ability_timer < (ability_timer_max - 40)) {
                    player.setVelocity(player.getRotationVector().multiply(+6));
                    player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z); // Prevent player flying to the heavens
                    player.velocityModified = true;
                    world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_THUNDER_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.3f, 1.6f);
                }
                //Player dash end
                if (ability_timer < 5) {
                    player.setVelocity(0, 0, 0); // Stop player at end of charge
                    player.velocityModified = true;
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 2), player);

                }

                //AOE Damage & charge control
                if (player.age % 3 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                    Box box = new Box(player.getX() + radius, player.getY() + radius * 2, player.getZ() + radius, player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                    for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if (entities instanceof LivingEntity le) {

                                float choose = (float) (Math.random() * 1);

                                if (ability_timer > (ability_timer_max - 40)) {
                                    le.damage(DamageSource.MAGIC, abilityDamage);
                                    world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_02.get(), SoundCategory.PLAYERS, 0.1f, choose);

                                }

                                if (ability_timer < (ability_timer_max - 40)) {
                                    le.damage(DamageSource.MAGIC, abilityDamage * 10);
                                    world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_POISON_ATTACK_01.get(), SoundCategory.PLAYERS, 0.1f, choose);
                                }

                            }
                        }
                    }

                    //world.playSoundFromEntity(null, player, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(), SoundCategory.PLAYERS, 0.1f, 0.6f);
                    double xpos = player.getX() - (radius + 1);
                    double ypos = player.getY();
                    double zpos = player.getZ() - (radius + 1);

                    for (int i = radius * 2; i > 0; i--) {
                        for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            HelperMethods.spawnParticle(world, ParticleTypes.ELECTRIC_SPARK, xpos + i + choose,
                                    ypos + 0.4,
                                    zpos + j + choose,
                                    0, 0.1, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.CLOUD, xpos + i + choose,
                                    ypos + 0.1,
                                    zpos + j + choose,
                                    0, 0, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.WARPED_SPORE, xpos + i + choose,
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
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip2"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip3"));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip4"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip6"));
        tooltip.add(Text.translatable("item.simplyswords.thunderbrandsworditem.tooltip7"));
        tooltip.add(Text.literal(""));

    }

}
