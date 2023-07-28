package net.sweenus.simplyswords.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class StealSwordItem extends UniqueSwordItem {
    public StealSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;
    public static boolean scalesWithSpellPower;
    float abilityDamage = 5;
    float spellScalingModifier = SimplySwords.uniqueEffectsConfig.stealSpellScaling;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld sworld = (ServerWorld) attacker.getWorld();
            int fhitchance = (int) SimplySwords.uniqueEffectsConfig.stealChance;
            int fduration = (int) SimplySwords.uniqueEffectsConfig.stealDuration;
            attacker.setVelocity(attacker.getRotationVector().multiply(+1));
            attacker.velocityModified = true;

            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10) {
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(),
                            target.getSoundCategory(), 0.5f, 2f);
                } else if (choose_sound <= 20) {
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(),
                            target.getSoundCategory(), 0.5f, 2f);
                } else {
                    sworld.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(),
                            target.getSoundCategory(), 0.5f, 2f);
                }

                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 2), attacker);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, fduration, 1), attacker);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, fduration, 1), attacker);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            int sradius = (int) SimplySwords.uniqueEffectsConfig.stealRadius;
            int vradius = (int) (SimplySwords.uniqueEffectsConfig.stealRadius / 2);

            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld sworld = (ServerWorld) user.getWorld();
            Box box = new Box(x + sradius, y + vradius, z + sradius,
                    x - sradius, y - vradius, z - sradius);
            for (Entity entity : sworld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                    int iduration = (int) SimplySwords.uniqueEffectsConfig.stealInvisDuration;
                    int bduration = (int) SimplySwords.uniqueEffectsConfig.stealBlindDuration;

                    if (le.hasStatusEffect(StatusEffects.SLOWNESS) && le.hasStatusEffect(StatusEffects.GLOWING)) {
                        if (le.distanceTo(user) > 5) { //can we check target here?
                            le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, bduration, 1), user);
                            user.teleport(le.getX(), le.getY(), le.getZ());
                            sworld.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                                    le.getSoundCategory(), 0.3f, 1.5f);
                            le.damage(user.getDamageSources().freeze(), abilityDamage);
                        } else {
                            user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, iduration, 1), user);
                            user.setVelocity(user.getRotationVector().multiply(+2));
                            user.velocityModified = true;
                            sworld.playSoundFromEntity(null, entity, SoundRegistry.MAGIC_BOW_SHOOT_MISS_01.get(),
                                    entity.getSoundCategory(), 0.3f, 1.5f);
                        }
                        le.removeStatusEffect(StatusEffects.SLOWNESS);
                        le.removeStatusEffect(StatusEffects.GLOWING);
                    }
                }
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (HelperMethods.commonSpellAttributeScaling(spellScalingModifier, entity, "soul") > 0) {
            abilityDamage = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, entity, "soul");
            scalesWithSpellPower = true;
        }
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.NAUTILUS, ParticleTypes.NAUTILUS,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        return false;
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip7").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip8").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip9").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip10").setStyle(TEXT));
        if (scalesWithSpellPower) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleSoul"));
        }
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
