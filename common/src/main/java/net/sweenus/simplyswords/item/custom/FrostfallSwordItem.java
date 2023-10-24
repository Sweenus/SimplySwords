package net.sweenus.simplyswords.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class FrostfallSwordItem extends UniqueSwordItem {
    public FrostfallSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;
    public static boolean scalesWithSpellPower;

    private final int abilityCooldown = (int) Config.getFloat("frostFuryCooldown", "UniqueEffects", ConfigDefaultValues.frostFuryCooldown);
    int radius = (int) Config.getFloat("frostFuryRadius", "UniqueEffects", ConfigDefaultValues.frostFuryRadius);
    float abilityDamage = Config.getFloat("frostFuryDamage", "UniqueEffects", ConfigDefaultValues.frostFuryDamage);
    int proc_chance = (int) Config.getFloat("frostFuryChance", "UniqueEffects", ConfigDefaultValues.frostFuryChance);
    int shatter_timer_max = (int) Config.getFloat("frostFuryDuration", "UniqueEffects", ConfigDefaultValues.frostFuryDuration);
    float spellScalingModifier = Config.getFloat("frostFurySpellScaling", "UniqueEffects", ConfigDefaultValues.frostFurySpellScaling);
    int shatter_timer;
    int player_shatter_timer;
    double lastX;
    double lastY;
    double lastZ;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);
            //AOE freeze
            if (attacker.getRandom().nextInt(100) <= proc_chance) {
                Box box = new Box(target.getX() + radius, target.getY() + radius, target.getZ() + radius,
                        target.getX() - radius, target.getY() - radius, target.getZ() - radius);
                for (Entity entity : world.getOtherEntities(attacker, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, attacker)) {
                        le.addStatusEffect(new StatusEffectInstance(EffectRegistry.FREEZE.get(), shatter_timer_max + 10, 0), attacker);
                        le.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, shatter_timer_max - 10, 4), attacker);
                        world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_01.get(),
                                le.getSoundCategory(), 0.1f, 3f);
                        BlockPos pos = BlockPos.ofFloored(le.getX(), le.getY(), le.getZ());
                        BlockPos pos2 = BlockPos.ofFloored(le.getX(), le.getY() + 1, le.getZ());
                        BlockState state = Blocks.ICE.getDefaultState();
                        if (world.getBlockState(pos) == Blocks.AIR.getDefaultState())
                            world.setBlockState(pos, state);
                        if (world.getBlockState(pos2) == Blocks.AIR.getDefaultState())
                            world.setBlockState(pos2, state);
                    }
                }
                shatter_timer = shatter_timer_max;
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            lastX = user.getX();
            lastY = user.getY();
            lastZ = user.getZ();

            world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                    user.getSoundCategory(), 0.6f, 2f);

            double xpos = user.getX() - 2;
            double ypos = user.getY();
            double zpos = user.getZ() - 2;
            user.setVelocity(0, 0, 0); // Stop player in place
            user.velocityModified = true;
            user.teleport(lastX, lastY, lastZ); //Ensure they don't end up stuck inside a block

            for (int i = 3; i > 0; i--) {
                for (int j = 3; j > 0; j--) {
                    BlockPos poscheck = BlockPos.ofFloored(xpos + i, ypos, zpos + j);
                    BlockPos poscheck2 = BlockPos.ofFloored(xpos + i, ypos + 1, zpos + j);
                    BlockPos poscheck3 = BlockPos.ofFloored(xpos + i, ypos + 2, zpos + j);
                    BlockPos poscheck4 = BlockPos.ofFloored(xpos + i, ypos - 1, zpos + j);

                    BlockState currentState = world.getBlockState(poscheck);
                    BlockState currentState2 = world.getBlockState(poscheck2);
                    BlockState currentState3 = world.getBlockState(poscheck3);
                    BlockState currentState4 = world.getBlockState(poscheck4);
                    BlockState state = Blocks.ICE.getDefaultState();
                    if (i + j != 4) {
                        if (currentState == Blocks.AIR.getDefaultState() || currentState == Blocks.SNOW.getDefaultState() || currentState == Blocks.GRASS.getDefaultState()
                                || currentState == Blocks.LARGE_FERN.getDefaultState() || currentState == Blocks.FERN.getDefaultState())
                            world.setBlockState(poscheck, state);
                        if (currentState2 == Blocks.AIR.getDefaultState() || currentState2 == Blocks.SNOW.getDefaultState() || currentState2 == Blocks.GRASS.getDefaultState()
                                || currentState2 == Blocks.LARGE_FERN.getDefaultState() || currentState2 == Blocks.FERN.getDefaultState())
                            world.setBlockState(poscheck2, state);
                    }
                    if (currentState3 == Blocks.AIR.getDefaultState() || currentState3 == Blocks.SNOW.getDefaultState() || currentState3 == Blocks.GRASS.getDefaultState()
                            || currentState3 == Blocks.LARGE_FERN.getDefaultState() || currentState3 == Blocks.FERN.getDefaultState())
                        world.setBlockState(poscheck3, state);
                    if (currentState4 == Blocks.AIR.getDefaultState() || currentState4 == Blocks.SNOW.getDefaultState() || currentState4 == Blocks.GRASS.getDefaultState()
                            || currentState4 == Blocks.LARGE_FERN.getDefaultState() || currentState4 == Blocks.FERN.getDefaultState())
                        world.setBlockState(poscheck4, state);
                }
            }
            user.teleport(lastX, lastY, lastZ);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, shatter_timer_max, 4), user);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, shatter_timer_max, 4), user);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, shatter_timer_max, 4), user);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, shatter_timer_max, 2), user);
            player_shatter_timer = shatter_timer_max;
            user.getItemCooldownManager().set(this, abilityCooldown);
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && (entity instanceof PlayerEntity player)) {
            if (shatter_timer > 0) {
                shatter_timer--;
            }
            if (player_shatter_timer > 0) {
                player_shatter_timer--;
            }
            if (shatter_timer == 1) {
                Box box = new Box(player.getX() + radius + 10, player.getY() + radius + 10, player.getZ() + radius + 10,
                        player.getX() - radius - 10, player.getY() - radius - 10, player.getZ() - radius - 10);
                for (Entity otherEntity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    //Ice shatter
                    if (otherEntity instanceof LivingEntity le) {
                        if (le.hasStatusEffect(EffectRegistry.FREEZE.get())) {
                            world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(),
                                    le.getSoundCategory(), 0.2f, 3f);
                            le.damage(player.getDamageSources().indirectMagic(entity, entity), abilityDamage);
                        }

                        double xpos = le.getX() - 2;
                        double ypos = le.getY();
                        double zpos = le.getZ() - 2;

                        for (int i = 3; i > 0; i--) {
                            for (int j = 3; j > 0; j--) {
                                BlockPos poscheck = BlockPos.ofFloored(xpos + i, ypos, zpos + j);
                                BlockPos poscheck2 = BlockPos.ofFloored(xpos + i, ypos + 1, zpos + j);
                                BlockPos poscheck3 = BlockPos.ofFloored(xpos + i, ypos + 2, zpos + j);
                                BlockPos poscheck4 = BlockPos.ofFloored(xpos + i, ypos - 1, zpos + j);

                                BlockState currentState = world.getBlockState(poscheck);
                                BlockState currentState2 = world.getBlockState(poscheck2);
                                BlockState currentState3 = world.getBlockState(poscheck3);
                                BlockState currentState4 = world.getBlockState(poscheck4);
                                BlockState state = Blocks.AIR.getDefaultState();
                                if (currentState == Blocks.ICE.getDefaultState() || currentState == Blocks.WATER.getDefaultState())
                                    world.setBlockState(poscheck, state);
                                if (currentState2 == Blocks.ICE.getDefaultState() || currentState2 == Blocks.WATER.getDefaultState())
                                    world.setBlockState(poscheck2, state);
                                if (currentState3 == Blocks.ICE.getDefaultState() || currentState3 == Blocks.WATER.getDefaultState())
                                    world.setBlockState(poscheck3, state);
                                if (currentState4 == Blocks.ICE.getDefaultState() || currentState4 == Blocks.WATER.getDefaultState())
                                    world.setBlockState(poscheck4, state);
                            }
                        }
                    }
                }
            }
            if (player_shatter_timer == 1) {
                world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(),
                        player.getSoundCategory(), 0.6f, 3f);
                double xpos = lastX - 2;
                double ypos = lastY;
                double zpos = lastZ - 2;

                for (int i = 3; i > 0; i--) {
                    for (int j = 3; j > 0; j--) {
                        BlockPos poscheck = BlockPos.ofFloored(xpos + i, ypos, zpos + j);
                        BlockPos poscheck2 = BlockPos.ofFloored(xpos + i, ypos + 1, zpos + j);
                        BlockPos poscheck3 = BlockPos.ofFloored(xpos + i, ypos + 2, zpos + j);
                        BlockPos poscheck4 = BlockPos.ofFloored(xpos + i, ypos - 1, zpos + j);

                        BlockState currentState = world.getBlockState(poscheck);
                        BlockState currentState2 = world.getBlockState(poscheck2);
                        BlockState currentState3 = world.getBlockState(poscheck3);
                        BlockState currentState4 = world.getBlockState(poscheck4);
                        BlockState state = Blocks.AIR.getDefaultState();
                        if (currentState == Blocks.ICE.getDefaultState() || currentState == Blocks.WATER.getDefaultState())
                            world.setBlockState(poscheck, state);
                        if (currentState2 == Blocks.ICE.getDefaultState() || currentState2 == Blocks.WATER.getDefaultState())
                            world.setBlockState(poscheck2, state);
                        if (currentState3 == Blocks.ICE.getDefaultState() || currentState3 == Blocks.WATER.getDefaultState())
                            world.setBlockState(poscheck3, state);
                        if (currentState4 == Blocks.ICE.getDefaultState() || currentState4 == Blocks.WATER.getDefaultState())
                            world.setBlockState(poscheck4, state);
                    }
                }
            }
        }
        if (HelperMethods.commonSpellAttributeScaling(spellScalingModifier, entity, "frost") > 0) {
            abilityDamage = HelperMethods.commonSpellAttributeScaling(spellScalingModifier, entity, "frost");
            scalesWithSpellPower = true;
        }
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE,
                ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip4", shatter_timer_max / 20, abilityDamage).setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip5", shatter_timer_max / 20).setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip6").setStyle(TEXT));
        if (scalesWithSpellPower) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleFrost"));
        }
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
