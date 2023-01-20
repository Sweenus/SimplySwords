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
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class LichbladeSwordItem extends SwordItem {
    public LichbladeSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    int radius = (int) (SimplySwordsConfig.getFloatValue("soulanguish_radius"));
    int soulDamage = (int) (SimplySwordsConfig.getFloatValue("soulanguish_damage"));
    int blizzard_timer_max = (int) (SimplySwordsConfig.getFloatValue("soulanguish_duration"));
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("soulanguish_cooldown"));
    float healAmount = (SimplySwordsConfig.getFloatValue("soulanguish_heal"));
    int range = (int) (SimplySwordsConfig.getFloatValue("soulanguish_range"));
    int blizzard_timer;
    int damageTracker;
    int chanceReduce;
    double lastX;
    double lastY;
    double lastZ;
    double targetX;
    double targetY;
    double targetZ;

    LivingEntity abilityTarget;



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
        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!user.world.isClient() && blizzard_timer < 1 && this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get()) || this.getDefaultStack().isOf(ItemsRegistry.WAKING_LICHBLADE.get())) {

            abilityTarget = (LivingEntity) HelperMethods.getTargetedEntity(user, range);
            if (abilityTarget != null) {
                abilityTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 10, 0), user);
                world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ENCHANT.get(), SoundCategory.PLAYERS, 0.5f, 0.5f);
                lastX = user.getX();
                lastY = user.getY();
                lastZ = user.getZ();
                blizzard_timer = blizzard_timer_max;
                chanceReduce = 0;
                user.getItemCooldownManager().set(this, skillCooldown);
            }

        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (blizzard_timer < 1) {
            if (!world.isClient && (entity instanceof PlayerEntity player)) {

                //AOE Aura
                if (player.age % 35 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                    Box box = new Box(player.getX() + radius, player.getY() + radius, player.getZ() + radius, player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                    for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if (entities instanceof LivingEntity le) {
                                le.damage(DamageSource.MAGIC, soulDamage);
                            }
                        }
                    }

                    world.playSoundFromEntity(null, player, SoundRegistry.DARK_SWORD_BLOCK.get(), SoundCategory.PLAYERS, 0.1f, 0.2f);
                    double xpos = player.getX() - (radius + 1);
                    double ypos = player.getY();
                    double zpos = player.getZ() - (radius + 1);

                    for (int i = radius * 2; i > 0; i--) {
                        for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            HelperMethods.spawnParticle(world, ParticleTypes.SCULK_SOUL, xpos + i + choose,
                                    ypos,
                                    zpos + j + choose,
                                    0, 0.1, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.SOUL, xpos + i + choose,
                                    ypos + 0.1,
                                    zpos + j + choose,
                                    0, 0, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM, xpos + i + choose,
                                    ypos + 2,
                                    zpos + j + choose,
                                    0, 0, 0);
                        }
                    }
                }
            }
        }
        else {
            blizzard_timer --;
            if (!world.isClient && (entity instanceof PlayerEntity player) && abilityTarget != null) {

                //Return to player after a duration & buff player
                if (blizzard_timer < 200) {
                    if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
                        if (abilityTarget != player)
                            abilityTarget = player;
                        if (player.squaredDistanceTo(lastX, lastY, lastZ) < radius) {
                            if (!player.hasStatusEffect(StatusEffects.ABSORPTION))
                                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, damageTracker, 1), player);
                            damageTracker = 0;
                            blizzard_timer = 0;
                            world.playSoundFromEntity(null, player, SoundRegistry.DARK_SWORD_SPELL.get(), SoundCategory.PLAYERS, 0.02f, 0.5f);
                        }
                    }
                    else {
                        blizzard_timer = 0;
                        damageTracker  = 0;
                    }
                }

                //3D sound control
                float soundDistance = 0.2f - (float) player.squaredDistanceTo(lastX, lastY, lastZ) / 800;

                //Target tracking cloud
                if (player.age % 5 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                    Box box = new Box(lastX + radius, lastY + radius, lastZ + radius, lastX - radius, lastY - radius, lastZ - radius);
                    for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if (entities instanceof LivingEntity le) {

                                //Heal effect
                                float choose = (float) (Math.random() * 1);
                                if (player.getRandom().nextInt(100) <= (8 - chanceReduce)) {
                                    world.playSoundFromEntity(null, le, SoundRegistry.DARK_SWORD_BREAKS.get(), SoundCategory.PLAYERS, soundDistance, choose);
                                    player.heal(healAmount);
                                    chanceReduce ++;
                                }
                                le.damage(DamageSource.MAGIC, soulDamage);
                                damageTracker += soulDamage * 3;

                            }
                        }
                    }

                    world.playSound(null, lastX, lastY, lastZ, SoundRegistry.DARK_SWORD_BLOCK.get(), SoundCategory.PLAYERS, soundDistance, 0.3f, 100);

                    //Move aura to target

                    if (abilityTarget !=null) {
                        targetX = abilityTarget.getX();
                        targetY = abilityTarget.getY();
                        targetZ = abilityTarget.getZ();

                        if (targetX > lastX)
                            lastX += 1;
                        if (targetX < lastX)
                            lastX -= 1;
                        if (targetZ > lastZ)
                            lastZ += 1;
                        if (targetZ < lastZ)
                            lastZ -= 1;
                        if (targetY > lastY)
                            lastY += 1;
                        if (targetY < lastY)
                            lastY -= 1;

                    }

                    double xpos = lastX - (radius + 1);
                    double ypos = lastY;
                    double zpos = lastZ - (radius + 1);
                    world.playSound(xpos, ypos, zpos, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.1f, 0.2f, true);

                    for (int i = radius * 2; i > 0; i--) {
                        for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            HelperMethods.spawnParticle(world, ParticleTypes.MYCELIUM, xpos + i + choose,
                                    ypos,
                                    zpos + j + choose,
                                    choose / 3, -0.3, choose / 3);
                            choose = (float) (Math.random() * 1);
                            HelperMethods.spawnParticle(world, ParticleTypes.SOUL, xpos + i + choose,
                                    ypos,
                                    zpos + j + choose,
                                    choose / 3, 0, choose / 3);
                        }
                    }
                }
            }
        }

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SOUL, ParticleTypes.SOUL, ParticleTypes.MYCELIUM, true);

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
        if (this.getDefaultStack().isOf(ItemsRegistry.SLUMBERING_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        if (this.getDefaultStack().isOf(ItemsRegistry.WAKING_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.2").formatted(Formatting.GOLD, Formatting.BOLD));
        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get()))
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip1.3").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip3", radius));
        tooltip.add(Text.literal(""));
        if (this.getDefaultStack().isOf(ItemsRegistry.WAKING_LICHBLADE.get()) || this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
            tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip4"));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip5"));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip6"));
            tooltip.add(Text.literal(""));
        }
        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip7"));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip8"));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip9"));
            tooltip.add(Text.translatable("item.simplyswords.lichbladesworditem.tooltip10"));
            tooltip.add(Text.literal(""));
        }

    }

}
