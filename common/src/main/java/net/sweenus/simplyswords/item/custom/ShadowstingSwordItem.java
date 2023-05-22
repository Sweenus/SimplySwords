package net.sweenus.simplyswords.item.custom;


import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class ShadowstingSwordItem extends UniqueSwordItem {

    private static int stepMod = 0;
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("shadowmist_cooldown"));
    int abilityChance =  (int) (SimplySwordsConfig.getFloatValue("shadowmist_chance"));
    int damageArmorMultiplier = (int) (SimplySwordsConfig.getFloatValue("shadowmist_damage_multiplier"));
    int blindDuration = (int) (SimplySwordsConfig.getFloatValue("shadowmist_blind_duration"));
    int radius = (int) (SimplySwordsConfig.getFloatValue("shadowmist_radius"));

    public ShadowstingSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }


    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {



        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.world.isClient()) {

            if (attacker.getRandom().nextInt(100) <= abilityChance && (attacker instanceof PlayerEntity player)) {
                attacker.world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(), SoundCategory.PLAYERS, 0.3f, 1.8f);
                int extraDamage = target.getArmor() * damageArmorMultiplier;
                target.damage(DamageSource.MAGIC,  extraDamage);
            }
        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        ItemStack itemStack = user.getStackInHand(hand);
        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(), SoundCategory.PLAYERS, 0.4f, 1.6f);
        user.getItemCooldownManager().set(this.getDefaultStack().getItem(), skillCooldown);

        Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius, user.getX() - radius, user.getY() - radius, user.getZ() - radius);
        for (Entity entities : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {

            if (entities != null) {
                if ((entities instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, blindDuration, 3), user);
                }
            }
        }

        double xpos = user.getX() - (radius + 1);
        double ypos = user.getY();
        double zpos = user.getZ() - (radius + 1);

        for (int i = radius * 2; i > 0; i--) {
            for (int j = radius * 2; j > 0; j--) {
                float choose = (float) (Math.random() * 1);
                HelperMethods.spawnParticle(world, ParticleTypes.LARGE_SMOKE, xpos + i + choose,
                        ypos + 0.4,
                        zpos + j + choose,
                        0, 0.1, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, xpos + i + choose,
                        ypos + 0.1,
                        zpos + j + choose,
                        0, 0, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.SMOKE, xpos + i + choose,
                        ypos,
                        zpos + j + choose,
                        0, 0.1, 0);
            }
        }

        double chooseX = Math.random() * (radius * 2); double chooseZ = Math.random() * (radius * 2);

        BlockPos poscheck = new BlockPos(xpos + chooseX, ypos, zpos + chooseX);
        BlockPos poscheck2 = new BlockPos(xpos + chooseX, ypos + 1, zpos + chooseX);
        BlockState currentState = world.getBlockState(poscheck);
        BlockState currentState2 = world.getBlockState(poscheck2);

        if (currentState == Blocks.AIR.getDefaultState() && currentState2 == Blocks.AIR.getDefaultState())  {
            user.teleport((user.getX() -radius) + chooseX, user.getY(), (user.getZ() -radius) + chooseZ);
        } else {
            chooseX = Math.random() * (radius * 2);
            chooseZ = Math.random() * (radius * 2);
            user.teleport((user.getX() -radius) + chooseX, user.getY(), (user.getZ() -radius) + chooseZ);
        }

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

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
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip3"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip5"));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
