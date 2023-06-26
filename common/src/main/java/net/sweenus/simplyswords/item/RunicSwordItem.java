package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.RunicMethods;

import java.util.List;

public class RunicSwordItem extends SwordItem {

    public static int maxUseTime;

    public RunicSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings.fireproof());
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {

        if(stack.getOrCreateNbt().getString("runic_power").isEmpty()) {

            String runicPowerSelection = HelperMethods.chooseRunicPower();
            stack.getOrCreateNbt().putString("runic_power", runicPowerSelection);
        }

        return false;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);

            //FREEZE
            if (stack.getOrCreateNbt().getString("runic_power").equals("freeze")) {
                RunicMethods.postHitRunicFreeze(stack, target, attacker);
            }
            //WILDFIRE
            if (stack.getOrCreateNbt().getString("runic_power").equals("wildfire")) {
                RunicMethods.postHitRunicWildfire(stack, target, attacker);
            }
            //SLOW
            if (stack.getOrCreateNbt().getString("runic_power").equals("slow")) {
                RunicMethods.postHitRunicSlow(stack, target, attacker);
            }
            //GREATER SLOW
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_slow")) {
                RunicMethods.postHitRunicGreaterSlow(stack, target, attacker);
            }
            //SWIFTNESS
            if (stack.getOrCreateNbt().getString("runic_power").equals("swiftness")) {
                RunicMethods.postHitRunicSwiftness(stack, target, attacker);
            }
            //GREATER SWIFTNESS
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_swiftness")) {
                RunicMethods.postHitRunicGreaterSwiftness(stack, target, attacker);
            }
            //FLOAT
            if (stack.getOrCreateNbt().getString("runic_power").equals("float")) {
                RunicMethods.postHitRunicFloat(stack, target, attacker);
            }
            //GREATER FLOAT
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_float")) {
                RunicMethods.postHitRunicGreaterFloat(stack, target, attacker);
            }
            //ZEPHYR
            if (stack.getOrCreateNbt().getString("runic_power").equals("zephyr")) {
                RunicMethods.postHitRunicZephyr(stack, target, attacker);
            }
            //GREATER ZEPHYR
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_zephyr")) {
                RunicMethods.postHitRunicGreaterZephyr(stack, target, attacker);
            }
            //SHIELDING
            if (stack.getOrCreateNbt().getString("runic_power").equals("shielding")) {
                RunicMethods.postHitRunicShielding(stack, target, attacker);
            }
            //GREATER SHIELDING
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_shielding")) {
                RunicMethods.postHitRunicGreaterShielding(stack, target, attacker);
            }
            //STONESKIN
            if (stack.getOrCreateNbt().getString("runic_power").equals("stoneskin")) {
                RunicMethods.postHitRunicStoneskin(stack, target, attacker);
            }
            //GREATER STONESKIN
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_stoneskin")) {
                RunicMethods.postHitRunicGreaterStoneskin(stack, target, attacker);
            }
            //TRAILBLAZE
            if (stack.getOrCreateNbt().getString("runic_power").equals("trailblaze")) {
                RunicMethods.postHitRunicTrailblaze(stack, target, attacker);
            }
            //GREATER TRAILBLAZE
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_trailblaze")) {
                RunicMethods.postHitRunicGreaterTrailblaze(stack, target, attacker);
            }
            //WEAKEN
            if (stack.getOrCreateNbt().getString("runic_power").equals("weaken")) {
                RunicMethods.postHitRunicWeaken(stack, target, attacker);
            }
            //GREATER WEAKEN
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_weaken")) {
                RunicMethods.postHitRunicGreaterWeaken(stack, target, attacker);
            }
            //IMBUED
            if (stack.getOrCreateNbt().getString("runic_power").equals("imbued")) {
                RunicMethods.postHitRunicImbued(stack, target, attacker);
            }
            //GREATER IMBUED
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_imbued")) {
                RunicMethods.postHitRunicGreaterImbued(stack, target, attacker);
            }
            //PINCUSHION
            if (stack.getOrCreateNbt().getString("runic_power").equals("pincushion")) {
                RunicMethods.postHitRunicPinCushion(stack, target, attacker);
            }
            //PINCUSHION
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_pincushion")) {
                RunicMethods.postHitRunicGreaterPinCushion(stack, target, attacker);
            }

        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient) {
            if (stack.getOrCreateNbt().getString("runic_power").contains("momentum"))
                RunicMethods.stoppedUsingRunicMomentum(stack, world, user, remainingUseTicks);
        }
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient) {
            if (stack.getOrCreateNbt().getString("runic_power").equals("momentum"))
                RunicMethods.usageTickRunicMomentum(stack, world, user, remainingUseTicks);
            if (stack.getOrCreateNbt().getString("runic_power").equals("greater_momentum"))
                RunicMethods.usageTickRunicGreaterMomentum(stack, world, user, remainingUseTicks);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (itemStack.getOrCreateNbt().getString("runic_power").contains("momentum")) {

            if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
                return TypedActionResult.fail(itemStack);
            }
            user.setCurrentHand(hand);
            world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_WIND_SHOOT_FLYBY_02.get(),
                    SoundCategory.PLAYERS, 0.3f, 1.2f);
            return TypedActionResult.consume(itemStack);
        }

        if (itemStack.getOrCreateNbt().getString("runic_power").equals("ward")) {

            if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
                return TypedActionResult.fail(itemStack);
            }
            user.setCurrentHand(hand);
            user.addStatusEffect(new StatusEffectInstance(EffectRegistry.WARD.get(), 120, 0), user);
            user.getItemCooldownManager().set(itemStack.getItem(), 120);
            user.setHealth(user.getHealth() / 2);
            world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.3f, 1.2f);
            return TypedActionResult.consume(itemStack);
        }

        if (itemStack.getOrCreateNbt().getString("runic_power").equals("immolation")) {

            if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
                return TypedActionResult.fail(itemStack);
            }
            user.setCurrentHand(hand);
            user.addStatusEffect(new StatusEffectInstance(EffectRegistry.IMMOLATION.get(), 36000, 0), user);
            user.getItemCooldownManager().set(itemStack.getItem(), 40);
            world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    SoundCategory.PLAYERS, 0.3f, 0.6f);
            return TypedActionResult.consume(itemStack);
        }

        return TypedActionResult.fail(itemStack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        if (stack.getOrCreateNbt().getString("runic_power").contains("momentum"))
            maxUseTime = 15;
        if (stack.getOrCreateNbt().getString("runic_power").equals("ward"))
            maxUseTime = 1;
        if (stack.getOrCreateNbt().getString("runic_power").equals("immolation"))
            maxUseTime = 1;

        return maxUseTime;
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }


    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity.age % 4 == 0 && SimplySwordsConfig.getBooleanValue("enable_passive_particles") && (entity instanceof PlayerEntity player)) {
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                float randomx = (float) (Math.random() * 6);
                float randomz = (float) (Math.random() * 6);

                world.addParticle(ParticleTypes.ENCHANT, player.getX() + player.getHandPosOffset(this).getX(),
                        player.getY() + player.getHandPosOffset(this).getY() + 1.3,
                        player.getZ() + player.getHandPosOffset(this).getZ(),
                        -3 + randomx, 0.0, -3 + randomz);

            }
        }
        if (!world.isClient && (entity instanceof PlayerEntity player)) {

        //UNSTABLE
        if (stack.getOrCreateNbt().getString("runic_power").equals("unstable")) {
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                RunicMethods.inventoryTickRunicUnstable(stack, world, player, slot, selected);
            }
        }
        //ACTIVE DEFENCE
        if (stack.getOrCreateNbt().getString("runic_power").equals("active_defence")) {
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                RunicMethods.inventoryTickRunicActiveDefence(stack, world, player, slot, selected);
            }
        }
        //FROST WARD
        if (stack.getOrCreateNbt().getString("runic_power").equals("frost_ward")) {
            if (player.getEquippedStack(EquipmentSlot.MAINHAND) == stack || player.getEquippedStack(EquipmentSlot.OFFHAND) == stack) {
                RunicMethods.inventoryTickRunicFrostWard(stack, world, player, slot, selected);
            }
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }
}

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player)
    {
        if(world.isClient) return;

        String runicPowerSelection = HelperMethods.chooseRunicPower();
        stack.getOrCreateNbt().putString("runic_power", runicPowerSelection);

    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("greater"))
            tooltip.add(Text.translatable("item.simplyswords.greater_runic_power").formatted(Formatting.DARK_AQUA, Formatting.BOLD));

        if (itemStack.getOrCreateNbt().getString("runic_power").isEmpty()) {

            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("freeze")) {

            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("wildfire")) {

            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("slow")) {

            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("swiftness")) {

            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("float")) {

            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("zephyr")) {

            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("shielding")) {

            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("stoneskin")) {

            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("frost_ward")) {

            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("trailblaze")) {

            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("active_defence")) {

            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("weaken")) {

            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("unstable")) {

            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("momentum")) {

            tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("imbued")) {

            tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("pincushion")) {

            tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("ward")) {

            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip3"));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip4"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("immolation")) {

            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip3"));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip4"));

        }

    }
}