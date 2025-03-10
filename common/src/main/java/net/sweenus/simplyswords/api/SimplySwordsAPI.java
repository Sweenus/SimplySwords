package net.sweenus.simplyswords.api;

import me.fzzyhmstrs.fzzy_config.util.ValidationResult;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.GemPowerFiller;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.List;

public class SimplySwordsAPI {


    // Battle Standard
    // Allows for the summoning of Battle Standard Entities. Can be configured with positive & negative effects (nullable)
    public static BattleStandardEntity spawnBattleStandard(PlayerEntity user, int decayRate, String standardType, int height, int distance,
                                                           String positiveEffect, String positiveEffectSecondary,
                                                           int positiveEffectAmplifier,
                                                           String negativeEffect, String negativeEffectSecondary,
                                                           int negativeEffectAmplifier,
                                                           boolean dealsDamage, boolean doesHealing) {

        if (!user.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) user.getWorld();
            BlockState currentState = world.getBlockState(user.getBlockPos().up(height).offset(user.getMovementDirection(), distance));
            BlockState state = Blocks.AIR.getDefaultState();
            if (currentState == state) {
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                        user.getSoundCategory(), 0.4f, 0.8f);
                BattleStandardEntity banner = EntityRegistry.BATTLESTANDARD.get().spawn(
                        world,
                        user.getBlockPos().up(height).offset(user.getMovementDirection(), distance),
                        SpawnReason.MOB_SUMMONED);
                if (banner != null) {
                    banner.setVelocity(0, -1, 0);
                    banner.ownerEntity = user;
                    banner.decayRate = decayRate;
                    banner.standardType = standardType;
                    banner.doesHealing = doesHealing;
                    banner.dealsDamage = dealsDamage;
                    banner.negativeEffect = negativeEffect;
                    banner.negativeEffectSecondary = negativeEffectSecondary;
                    banner.positiveEffect = positiveEffect;
                    banner.positiveEffectSecondary = positiveEffectSecondary;
                    banner.positiveEffectAmplifier = positiveEffectAmplifier;
                    banner.negativeEffectAmplifier = negativeEffectAmplifier;
                    banner.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name", user.getName()));
                    return banner;
                }
            }
        }
        return null;
    }

    public static GemPowerComponent getComponent(ItemStack stack) {
        return stack.getOrDefault(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.DEFAULT);
    }

    // Gem Sockets
    // When each method is added to an item class, allows for gem sockets to appear on the item.
    // Each method needs to be called in its respective Override method. (Eg. inventoryTickGemSocketLogic goes in inventoryTick)

    // Performs postHit socket effects
    public static void postHitGemSocketLogic(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            GemPowerComponent component = getComponent(stack);
            component.postHit(stack, target, attacker);
        }
    }

    // Adds the relevant socket information to the item tooltip
    public static void appendTooltipGemSocketLogic(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {

        GemPowerComponent component = getComponent(itemStack);

        if (!component.isEmpty()) {
            tooltip.add(Text.literal(""));
        }

        component.appendTooltip(itemStack, tooltipContext, tooltip, type);
        /* Change ALT key logic to each component
        if (Screen.hasAltDown()) {
            component.appendTooltip(itemStack, tooltipContext, tooltip, type);
        } else if (component.canBeFilled()) {
            tooltip.add(Text.translatable("item.simplyswords.common.showtooltip").formatted(Formatting.GRAY));
        }

         */
    }

    // Allows for the socketing of gems
    public static void onClickedGemSocketLogic (ItemStack stack, ItemStack otherStack, PlayerEntity player) {
        if (Config.general.enableUniqueGemSockets) {
            GemPowerComponent component = getComponent(stack);
            if (component.canBeFilled()) {
                if (otherStack.getItem() instanceof GemPowerFiller gemPowerFiller) {
                    ValidationResult<GemPowerComponent> result = gemPowerFiller.fill(otherStack, component);
                    if (result.isValid()) {
                        stack.set(ComponentTypeRegistry.GEM_POWER.get(), result.get());
                        player.getWorld().playSoundFromEntity(null, player, SoundEvents.BLOCK_ANVIL_USE, player.getSoundCategory(), 1, 1);
                        otherStack.decrement(1);
                    }
                }
            }
        }
    }

    // netherSocketChance & runeSocketChance determine how likely these sockets are to appear on the item. An int of 50 = 50% chance for the socket to appear.
    public static void inventoryTickGemSocketLogic (ItemStack stack, World world, Entity entity,
                                                    int runeSocketChance, int netherSocketChance) {
        if (!stack.contains(ComponentTypeRegistry.GEM_POWER.get()) && Config.general.enableUniqueGemSockets) {
            float runeSocketRoll = (float) (Math.random() * 100);
            float netherSocketRoll = (float) (Math.random() * 100);
            stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.createEmpty(runeSocketRoll > runeSocketChance, netherSocketRoll > netherSocketChance));
        }
        if (!world.isClient && (entity instanceof LivingEntity user) &&
                (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack || user.getEquippedStack(EquipmentSlot.OFFHAND) == stack)) {
            GemPowerComponent component = getComponent(stack);
            component.inventoryTick(stack, world, user, 0, true);
        }
    }

}