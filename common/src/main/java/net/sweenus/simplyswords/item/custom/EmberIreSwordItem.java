package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.DefaultParticleType;
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
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class EmberIreSwordItem extends SwordItem {
    public EmberIreSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    private final int radius = 8;
    private final int abilityTimerMax = 80;
    private final int abilityDamage = 1;
    private final int abilityChance = 5;
    private static int abilityTimer;
    private static DefaultParticleType particleWalk = ParticleTypes.FALLING_LAVA;
    private static DefaultParticleType particleSprint = ParticleTypes.FALLING_LAVA;
    private static DefaultParticleType particlePassive = ParticleTypes.SMOKE;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            int fhitchance = (int) SimplySwordsConfig.getFloatValue("ember_ire_chance");
            int fduration = (int) SimplySwordsConfig.getFloatValue("ember_ire_duration");

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


            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                attacker.setOnFireFor(fduration / 20);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, fduration, 0), attacker);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, fduration, 0), attacker);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 0), attacker);
                world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_01.get(), SoundCategory.PLAYERS, 0.5f, 2f);
                particlePassive = ParticleTypes.LAVA;
                particleWalk = ParticleTypes.CAMPFIRE_COSY_SMOKE;
                particleSprint = ParticleTypes.CAMPFIRE_COSY_SMOKE;
            }
        }

            return super.postHit(stack, target, attacker);

        }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!user.world.isClient()) {
            if (user.hasStatusEffect(StatusEffects.STRENGTH) && user.hasStatusEffect(StatusEffects.HASTE) && abilityTimer < 1) {

                abilityTimer = abilityTimerMax;
                world.playSound(null, user.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.3f, 2f);
                user.removeStatusEffect(StatusEffects.HASTE);
                user.removeStatusEffect(StatusEffects.SPEED);
                user.removeStatusEffect(StatusEffects.STRENGTH);
                user.extinguish();


            }
        }
        return super.use(world,user,hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if ((entity instanceof PlayerEntity player)) {
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                if (abilityTimer > 0)
                    abilityTimer--;
                if (abilityTimer > 1) {
                    if (!world.isClient) {

                        //AOE Aura
                        if (player.age % 15 == 0) {
                            world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.1f, 2);
                            Box box = new Box(player.getX() + radius, player.getY() + radius, player.getZ() + radius, player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                            for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                                if (entities != null) {
                                    if (entities instanceof LivingEntity le) {
                                        float choose = (float) (Math.random() * 1);
                                        le.damage(DamageSource.IN_FIRE, abilityDamage);

                                        if (le.getRandom().nextInt(100) <= abilityChance) {

                                            FireballEntity fireball = new FireballEntity(world, player, 1.2, -2, 1.2, 1);
                                            fireball.setPosition(le.getX()-7, le.getY()+10, le.getZ()-7);
                                            world.spawnEntity(fireball);
                                            //fireball.setVelocity(0, -5, 0);


                                            int choose_sound = (int) (Math.random() * 30);
                                            if (choose_sound <= 10)
                                                world.playSound(le.getX()-7, le.getY()+10, le.getZ()-7, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(), SoundCategory.HOSTILE, 0.8f, 1f, true);
                                            if (choose_sound <= 20 && choose_sound > 10)
                                                world.playSound(le.getX()-7, le.getY()+10, le.getZ()-7, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(), SoundCategory.HOSTILE, 0.8f, 1f, true);
                                            if (choose_sound <= 30 && choose_sound > 20)
                                                world.playSound(le.getX()-7, le.getY()+10, le.getZ()-7, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.HOSTILE, 0.8f, 1f, true);
                                        }


                                    }
                                }
                            }

                            world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_SWORD_ICE_ATTACK_02.get(), SoundCategory.PLAYERS, 0.1f, 0.6f);
                            double xpos = player.getX() - (radius + 1);
                            double ypos = player.getY();
                            double zpos = player.getZ() - (radius + 1);

                            for (int i = radius * 2; i > 0; i--) {
                                for (int j = radius * 2; j > 0; j--) {
                                    float choose = (float) (Math.random() * 1);
                                    HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, xpos + i + choose,
                                            ypos,
                                            zpos + j + choose,
                                            0, 0, 0);
                                }
                            }
                        }
                    }
                }
            }
        }


        if ((entity instanceof PlayerEntity player)) {
            if (!player.hasStatusEffect(StatusEffects.STRENGTH) && !player.isOnFire()) {
                particlePassive = ParticleTypes.SMOKE;
                particleWalk = ParticleTypes.FALLING_LAVA;
                particleSprint = ParticleTypes.FALLING_LAVA;
            }
        }


                if (stepMod > 0)
                    stepMod--;
                if (stepMod <= 0)
                    stepMod = 7;
                HelperMethods.createFootfalls(entity, stack, world, stepMod, particleWalk, particleSprint, particlePassive, true);

        super.inventoryTick(stack, world, entity, slot, selected);
}

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19.x

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip3"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip5"));
        /*

        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip3"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip4"));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip5"));

         */
    }

}
