package net.sweenus.simplyswords.item.custom;

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
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class RendSwordItem extends UniqueSwordItem {
    public RendSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    float abilityDamage = Config.getFloat("soulrendDamageMulti", "UniqueEffects", ConfigDefaultValues.soulrendDamageMulti);
    float spellScalingModifier = Config.getFloat("soulrendDamageSpellScaling", "UniqueEffects", ConfigDefaultValues.soulrendDamageSpellScaling);

    private static int stepMod = 0;
    public static boolean scalesWithSpellPower;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            int fhitchance = (int) Config.getFloat("soulrendChance", "UniqueEffects", ConfigDefaultValues.soulrendChance);
            int fduration = (int) Config.getFloat("soulrendDuration", "UniqueEffects", ConfigDefaultValues.soulrendDuration);
            int maxstacks = (int) Config.getFloat("soulrendMaxStacks", "UniqueEffects", ConfigDefaultValues.soulrendMaxStacks);

            if (attacker.getRandom().nextInt(100) <= fhitchance) {

                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_01.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);
                else if (choose_sound <= 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);
                else if (choose_sound <= 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);

                if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                    int a = (target.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1);

                    if ((target.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() <= 0)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, fduration, a), attacker);
                    }
                } else {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, fduration, 0), attacker);
                }

                if (target.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    int a = (target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);

                    if ((target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() < maxstacks)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, fduration, a), attacker);
                    }
                } else {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, fduration, 0), attacker);
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            float heal_amount = Config.getFloat("soulrendHealMulti", "UniqueEffects", ConfigDefaultValues.soulrendHealMulti);
            int healamp = 0;
            boolean cantrigger = false;
            int hradius = (int) Config.getFloat("soulrendRadius", "UniqueEffects", ConfigDefaultValues.soulrendRadius);
            int vradius = (int) (Config.getFloat("soulrendRadius", "UniqueEffects", ConfigDefaultValues.soulrendRadius) / 2);
            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld sworld = (ServerWorld) user.getWorld();
            Box box = new Box(x + hradius, y + vradius, z + hradius, x - hradius, y - vradius, z - hradius);

            for (Entity entity : sworld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && le.hasStatusEffect(StatusEffects.SLOWNESS)
                        && le.hasStatusEffect(StatusEffects.WEAKNESS) && HelperMethods.checkFriendlyFire(le, user)) {

                    healamp += (le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier());
                    cantrigger = true;
                    le.damage(user.getDamageSources().indirectMagic(user, user), le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() * abilityDamage);
                    le.removeStatusEffect(StatusEffects.WEAKNESS);
                    le.removeStatusEffect(StatusEffects.SLOWNESS);
                    world.playSoundFromEntity(null, entity, SoundRegistry.DARK_SWORD_SPELL.get(),
                            entity.getSoundCategory(), 0.1f, 2f);
                }
            }
            if (cantrigger) {
                healamp = (int) (healamp * heal_amount);

                if (healamp < 1) healamp = 1;
                else if (healamp > 6) healamp = 6;

                user.heal(healamp);
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
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SOUL, ParticleTypes.SCULK_SOUL,
                ParticleTypes.WARPED_SPORE, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip6").setStyle(TEXT));
        if (scalesWithSpellPower) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleSoul"));
        }
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
