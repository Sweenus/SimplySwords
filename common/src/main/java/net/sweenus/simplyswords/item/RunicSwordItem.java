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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
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
        if (stack.getOrCreateNbt().getString("runic_power").isEmpty()) {
            String runicPowerSelection = HelperMethods.chooseRunicPower();
            stack.getOrCreateNbt().putString("runic_power", runicPowerSelection);
        }
        return false;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);

            switch (stack.getOrCreateNbt().getString("runic_power")) {
                case "freeze" -> RunicMethods.postHitRunicFreeze(target, attacker);
                case "wildfire" -> RunicMethods.postHitRunicWildfire(target, attacker);
                case "slow" -> RunicMethods.postHitRunicSlow(target, attacker);
                case "greater_slow" -> RunicMethods.postHitRunicGreaterSlow(target, attacker);
                case "swiftness" -> RunicMethods.postHitRunicSwiftness(attacker);
                case "greater_swiftness" -> RunicMethods.postHitRunicGreaterSwiftness(attacker);
                case "float" -> RunicMethods.postHitRunicFloat(target, attacker);
                case "greater_float" -> RunicMethods.postHitRunicGreaterFloat(target, attacker);
                case "zephyr" -> RunicMethods.postHitRunicZephyr(attacker);
                case "greater_zephyr" -> RunicMethods.postHitRunicGreaterZephyr(attacker);
                case "shielding" -> RunicMethods.postHitRunicShielding(attacker);
                case "greater_shielding" -> RunicMethods.postHitRunicGreaterShielding(attacker);
                case "stoneskin" -> RunicMethods.postHitRunicStoneskin(attacker);
                case "greater_stoneskin" -> RunicMethods.postHitRunicGreaterStoneskin(attacker);
                case "trailblaze" -> RunicMethods.postHitRunicTrailblaze(attacker);
                case "greater_trailblaze" -> RunicMethods.postHitRunicGreaterTrailblaze(attacker);
                case "weaken" -> RunicMethods.postHitRunicWeaken(target, attacker);
                case "greater_weaken" -> RunicMethods.postHitRunicGreaterWeaken(target, attacker);
                case "imbued" -> RunicMethods.postHitRunicImbued(stack, target, attacker);
                case "greater_imbued" -> RunicMethods.postHitRunicGreaterImbued(stack, target, attacker);
                case "pincushion" -> RunicMethods.postHitRunicPinCushion(target, attacker);
                case "greater_pincushion" -> RunicMethods.postHitRunicGreaterPinCushion(target, attacker);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && stack.getOrCreateNbt().getString("runic_power").contains("momentum"))
            RunicMethods.stoppedUsingRunicMomentum(stack, user);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient) {
            if (stack.getOrCreateNbt().getString("runic_power").equals("momentum"))
                RunicMethods.usageTickRunicMomentum(stack, user, remainingUseTicks);
            else if (stack.getOrCreateNbt().getString("runic_power").equals("greater_momentum"))
                RunicMethods.usageTickRunicGreaterMomentum(stack, user, remainingUseTicks);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (itemStack.getDamage() < itemStack.getMaxDamage() - 1) {
            switch (itemStack.getOrCreateNbt().getString("runic_power")) {
                case "momentum", "greater_momentum" -> {
                    user.setCurrentHand(hand);
                    world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_WIND_SHOOT_FLYBY_02.get(),
                            user.getSoundCategory(), 0.3f, 1.2f);
                    return TypedActionResult.consume(itemStack);
                }
                case "ward" -> {
                    user.setCurrentHand(hand);
                    user.addStatusEffect(new StatusEffectInstance(EffectRegistry.WARD.get(), 120, 0), user);
                    user.getItemCooldownManager().set(itemStack.getItem(), 120);
                    user.setHealth(user.getHealth() / 2);
                    world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                            user.getSoundCategory(), 0.3f, 1.2f);
                    return TypedActionResult.consume(itemStack);
                }
                case "immolation" -> {
                    user.setCurrentHand(hand);
                    user.addStatusEffect(new StatusEffectInstance(EffectRegistry.IMMOLATION.get(), 36000, 0), user);
                    user.getItemCooldownManager().set(itemStack.getItem(), 40);
                    world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                            user.getSoundCategory(), 0.3f, 0.6f);
                    return TypedActionResult.consume(itemStack);
                }
            }
        }
        return TypedActionResult.fail(itemStack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        String power = stack.getOrCreateNbt().getString("runic_power");
        if (power.equals("momentum")) maxUseTime = 15;
        else if (power.equals("ward") || power.equals("immolation")) maxUseTime = 1;

        return maxUseTime;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }


    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if ((entity instanceof LivingEntity user) && (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack || user.getEquippedStack(EquipmentSlot.OFFHAND) == stack)) {
            if (entity.age % 4 == 0 && Config.getBoolean("enablePassiveParticles", "General", ConfigDefaultValues.enablePassiveParticles)) {
                float randomx = (float) (Math.random() * 6);
                float randomz = (float) (Math.random() * 6);

                world.addParticle(ParticleTypes.ENCHANT,
                        user.getX() + user.getHandPosOffset(this).getX(),
                        user.getY() + user.getHandPosOffset(this).getY() + 1.3,
                        user.getZ() + user.getHandPosOffset(this).getZ(),
                        -3 + randomx, 0.0, -3 + randomz);
            }
            if (!world.isClient) {
                switch (stack.getOrCreateNbt().getString("runic_power")) {
                    case "unstable" -> RunicMethods.inventoryTickRunicUnstable(user);
                    case "active_defence" ->
                            RunicMethods.inventoryTickRunicActiveDefence(world, user);
                    case "frost_ward" -> RunicMethods.inventoryTickRunicFrostWard(world, user);
                }
            }
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if (world.isClient) return;

        String runicPowerSelection = HelperMethods.chooseRunicPower();
        stack.getOrCreateNbt().putString("runic_power", runicPowerSelection);
    }

    @Override
    public Text getName(ItemStack stack) {
        Style RUNIC = HelperMethods.getStyle("runic");
        return Text.translatable(this.getTranslationKey(stack)).setStyle(RUNIC);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style RUNIC = HelperMethods.getStyle("runic");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal("").setStyle(TEXT));

        if (itemStack.getOrCreateNbt().getString("runic_power").isEmpty()) {
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").setStyle(RUNIC));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2").setStyle(TEXT));
        } else {
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("greater")) {
                tooltip.add(Text.translatable("item.simplyswords.greater_runic_power").setStyle(RUNIC).formatted(Formatting.BOLD));
            }
            switch (itemStack.getOrCreateNbt().getString("runic_power")) {
                case "freeze" -> {
                    tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2").setStyle(TEXT));
                }
                case "wildfire" -> {
                    tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3").setStyle(TEXT));
                }
                case "slow", "greater_slow" -> {
                    tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3").setStyle(TEXT));
                }
                case "swiftness", "greater_swiftness" -> {
                    tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3").setStyle(TEXT));
                }
                case "float", "greater_float" -> {
                    tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3").setStyle(TEXT));
                }
                case "zephyr", "greater_zephyr" -> {
                    tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3").setStyle(TEXT));
                }
                case "shielding", "greater_shielding" -> {
                    tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip3").setStyle(TEXT));
                }
                case "stoneskin", "greater_stoneskin" -> {
                    tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip3").setStyle(TEXT));
                }
                case "frost_ward" -> {
                    tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip3").setStyle(TEXT));
                }
                case "trailblaze", "greater_trailblaze" -> {
                    tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip3").setStyle(TEXT));
                }
                case "active_defence" -> {
                    tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3").setStyle(TEXT));
                }
                case "weaken", "greater_weaken" -> {
                    tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip3").setStyle(TEXT));
                }
                case "unstable" -> {
                    tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip3").setStyle(TEXT));
                }
                case "momentum", "greater_momentum" -> {
                    tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip3").setStyle(TEXT));
                }
                case "imbued", "greater_imbued" -> {
                    tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip3").setStyle(TEXT));
                }
                case "pincushion", "greater_pincushion" -> {
                    tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip3").setStyle(TEXT));
                }
                case "ward" -> {
                    tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.literal(""));
                    tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
                    tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip3").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip4").setStyle(TEXT));
                }
                case "immolation" -> {
                    tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip1").setStyle(RUNIC));
                    tooltip.add(Text.literal(""));
                    tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
                    tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip3").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip4").setStyle(TEXT));
                }
            }
        }
    }
}