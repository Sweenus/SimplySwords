package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.ChargedLocationComponent;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class FrostfallSwordItem extends UniqueSwordItem {
    public FrostfallSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient()) super.postHit(stack, target, attacker);

        ServerWorld world = (ServerWorld) attacker.getWorld();
        HelperMethods.playHitSounds(attacker, target);
        //AOE freeze
        int proc_chance = Config.uniqueEffects.frostfall.chance;
        double radius = Config.uniqueEffects.frostfall.radius;
        int shatter_timer_max = Config.uniqueEffects.frostfall.duration;
        if (attacker.getRandom().nextInt(100) <= proc_chance) {
            Box box = new Box(target.getX() + radius, target.getY() + radius, target.getZ() + radius,
                    target.getX() - radius, target.getY() - radius, target.getZ() - radius);
            for (Entity entity : world.getOtherEntities(attacker, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, attacker)) {
                    le.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FREEZE), shatter_timer_max + 10, 0), attacker);
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
            stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), new StoredChargeComponent(shatter_timer_max));
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user.getWorld().isClient()) return super.use(world, user, hand);

        double lastX = user.getX();
        double lastY = user.getY();
        double lastZ = user.getZ();

        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_03.get(),
                user.getSoundCategory(), 0.6f, 2f);

        double xPos = user.getX() - 2;
        double yPos = user.getY();
        double zPos = user.getZ() - 2;
        user.setVelocity(0, 0, 0); // Stop player in place
        user.velocityModified = true;
        user.teleport(lastX, lastY, lastZ, false); // Ensure they don't end up stuck inside a block

        for (int i = 3; i > 0; i--) {
            for (int j = 3; j > 0; j--) {
                BlockPos poscheck = BlockPos.ofFloored(xPos + i, yPos, zPos + j);
                BlockPos poscheck2 = BlockPos.ofFloored(xPos + i, yPos + 1, zPos + j);
                BlockPos poscheck3 = BlockPos.ofFloored(xPos + i, yPos + 2, zPos + j);
                BlockPos poscheck4 = BlockPos.ofFloored(xPos + i, yPos - 1, zPos + j);

                BlockState currentState = world.getBlockState(poscheck);
                BlockState currentState2 = world.getBlockState(poscheck2);
                BlockState currentState3 = world.getBlockState(poscheck3);
                BlockState currentState4 = world.getBlockState(poscheck4);
                BlockState state = Blocks.ICE.getDefaultState();
                if (i + j != 4) {
                    if (currentState == Blocks.AIR.getDefaultState() || currentState == Blocks.SNOW.getDefaultState() || currentState == Blocks.SHORT_GRASS.getDefaultState()
                            || currentState == Blocks.LARGE_FERN.getDefaultState() || currentState == Blocks.FERN.getDefaultState() || currentState4 == Blocks.TALL_GRASS.getDefaultState())
                        world.setBlockState(poscheck, state);
                    if (currentState2 == Blocks.AIR.getDefaultState() || currentState2 == Blocks.SNOW.getDefaultState() || currentState2 == Blocks.SHORT_GRASS.getDefaultState()
                            || currentState2 == Blocks.LARGE_FERN.getDefaultState() || currentState2 == Blocks.FERN.getDefaultState() || currentState4 == Blocks.TALL_GRASS.getDefaultState())
                        world.setBlockState(poscheck2, state);
                }
                if (currentState3 == Blocks.AIR.getDefaultState() || currentState3 == Blocks.SNOW.getDefaultState() || currentState3 == Blocks.SHORT_GRASS.getDefaultState()
                        || currentState3 == Blocks.LARGE_FERN.getDefaultState() || currentState3 == Blocks.FERN.getDefaultState() || currentState4 == Blocks.TALL_GRASS.getDefaultState())
                    world.setBlockState(poscheck3, state);
                if (currentState4 == Blocks.AIR.getDefaultState() || currentState4 == Blocks.SNOW.getDefaultState() || currentState4 == Blocks.SHORT_GRASS.getDefaultState()
                        || currentState4 == Blocks.LARGE_FERN.getDefaultState() || currentState4 == Blocks.FERN.getDefaultState() || currentState4 == Blocks.TALL_GRASS.getDefaultState())
                    world.setBlockState(poscheck4, state);
            }
        }
        user.teleport(lastX, lastY, lastZ, false);
        int shatter_timer_max = Config.uniqueEffects.frostfall.duration;
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, shatter_timer_max, 4), user);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, shatter_timer_max, 4), user);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, shatter_timer_max, 4), user);
        user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, shatter_timer_max, 2), user);
        ItemStack stack = user.getStackInHand(hand);
        stack.set(ComponentTypeRegistry.CHARGED_LOCATION.get(), new ChargedLocationComponent(shatter_timer_max, lastX, lastY, lastZ));
        user.getItemCooldownManager().set(this, Config.uniqueEffects.frostfall.cooldown);
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && (entity instanceof PlayerEntity player)) {
            StoredChargeComponent shatterComponent = stack.apply(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT, StoredChargeComponent::decrement);
            ChargedLocationComponent chargedLocationComponent = stack.apply(ComponentTypeRegistry.CHARGED_LOCATION.get(), ChargedLocationComponent.DEFAULT, ChargedLocationComponent::decrement);
            if (shatterComponent != null && shatterComponent.charge() == 1) {
                double radius = Config.uniqueEffects.frostfall.radius;
                Box box = new Box(player.getX() + radius + 10, player.getY() + radius + 10, player.getZ() + radius + 10,
                        player.getX() - radius - 10, player.getY() - radius - 10, player.getZ() - radius - 10);
                for (Entity otherEntity : world.getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    //Ice shatter
                    if (otherEntity instanceof LivingEntity le) {
                        if (le.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FREEZE))) {
                            float abilityDamage = HelperMethods.spellScaledDamage("frost", player, Config.uniqueEffects.frostfall.spellScaling, Config.uniqueEffects.frostfall.damage);
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
                stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), null);
            }
            if (chargedLocationComponent != null && chargedLocationComponent.charge() == 1) {
                world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_ICE_SHOOT_IMPACT_02.get(),
                        player.getSoundCategory(), 0.6f, 3f);
                double xPos = chargedLocationComponent.lastX() - 2;
                double yPos = chargedLocationComponent.lastY();
                double zPos = chargedLocationComponent.lastZ() - 2;

                for (int i = 3; i > 0; i--) {
                    for (int j = 3; j > 0; j--) {
                        BlockPos poscheck = BlockPos.ofFloored(xPos + i, yPos, zPos + j);
                        BlockPos poscheck2 = BlockPos.ofFloored(xPos + i, yPos + 1, zPos + j);
                        BlockPos poscheck3 = BlockPos.ofFloored(xPos + i, yPos + 2, zPos + j);
                        BlockPos poscheck4 = BlockPos.ofFloored(xPos + i, yPos - 1, zPos + j);

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
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE,
                ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip3").setStyle(Styles.TEXT));
        float abilityDamage = HelperMethods.spellScaledDamage("frost", MinecraftClient.getInstance().player, Config.uniqueEffects.frostfall.spellScaling, Config.uniqueEffects.frostfall.damage);
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip4", Config.uniqueEffects.frostfall.duration / 20, abilityDamage).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip5", Config.uniqueEffects.frostfall.duration / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.frostfallsworditem.tooltip6").setStyle(Styles.TEXT));
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "frost");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.FROSTFALL::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 380;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 18f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 80;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 3.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.4f;
    }
}