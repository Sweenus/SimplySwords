package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
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

public class ArcanethystSwordItem extends SwordItem {
    public ArcanethystSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }
    private static int stepMod = 0;
    int radius = (int) (SimplySwordsConfig.getFloatValue("arcaneassault_radius"));
    int arcaneDamage = (int) (SimplySwordsConfig.getFloatValue("arcaneassault_damage"));
    int arcane_timer_max = (int) (SimplySwordsConfig.getFloatValue("arcaneassault_duration"));
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("arcaneassault_cooldown"));
    int chargeChance =  (int) (SimplySwordsConfig.getFloatValue("arcaneassault_chance"));
    int chargeCount;
    int arcane_timer;



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
                if (chargeCount < 3) {
                    chargeCount++;
                    world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_01.get(), SoundCategory.PLAYERS, 0.5f, 1.2f);
                }
            }

        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!user.world.isClient()) {

            if (chargeCount > 0 && arcane_timer < 1) {
                arcane_timer = arcane_timer_max;
                world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.4f, 1.2f);
                user.getItemCooldownManager().set(this, skillCooldown);
            }

        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (chargeCount < 0)
            chargeCount = 0;
        if (arcane_timer > 0 && chargeCount > 0) {
            if (!world.isClient && (entity instanceof PlayerEntity player)) {
                arcane_timer --;
                if (arcane_timer < 2)
                    chargeCount = 0;

                //AOE Lift - 1 charge
                if (player.age % 10 == 0 && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
                    Box box = new Box(player.getX() + radius, player.getY() + radius * 2, player.getZ() + radius, player.getX() - radius, player.getY() - radius, player.getZ() - radius);
                    for (Entity entities : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {

                        if (entities != null) {
                            if (entities instanceof LivingEntity le) {

                                float choose = (float) (Math.random() * 1);

                                if (!le.hasStatusEffect(StatusEffects.LEVITATION) && arcane_timer > 30) {
                                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 20, 1), player);
                                    world.playSoundFromEntity(null, le, SoundRegistry.MAGIC_BOW_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.1f, choose);
                                }
                                if (chargeCount > 1) // DOT - 2 charges
                                    le.damage(DamageSource.MAGIC, arcaneDamage);
                                if (chargeCount > 2 && arcane_timer < 20) { //Ground Slam - 3 Charges
                                    le.removeStatusEffect(StatusEffects.LEVITATION);
                                    le.damage(DamageSource.MAGIC, arcaneDamage * 10);
                                    le.setVelocity(0, -10, 0);
                                    world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(), SoundCategory.PLAYERS, 0.3f, choose);
                                }
                            }
                        }
                    }

                    world.playSoundFromEntity(null, player, SoundRegistry.MAGIC_BOW_CHARGE_SHORT_VERSION.get(), SoundCategory.PLAYERS, 0.1f, 0.6f);
                    double xpos = player.getX() - (radius + 1);
                    double ypos = player.getY();
                    double zpos = player.getZ() - (radius + 1);

                    for (int i = radius * 2; i > 0; i--) {
                        for (int j = radius * 2; j > 0; j--) {
                            float choose = (float) (Math.random() * 1);
                            HelperMethods.spawnParticle(world, ParticleTypes.DRAGON_BREATH, xpos + i + choose,
                                    ypos + 0.4,
                                    zpos + j + choose,
                                    0, 0.1, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.PORTAL, xpos + i + choose,
                                    ypos + 0.1,
                                    zpos + j + choose,
                                    0, 0, 0);
                            HelperMethods.spawnParticle(world, ParticleTypes.REVERSE_PORTAL, xpos + i + choose,
                                    ypos + 1,
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
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.DRAGON_BREATH, ParticleTypes.DRAGON_BREATH, ParticleTypes.REVERSE_PORTAL, true);

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
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip2"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip3"));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip4"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip6"));
        tooltip.add(Text.translatable("item.simplyswords.arcanethystsworditem.tooltip7"));
        tooltip.add(Text.literal(""));

    }

}
