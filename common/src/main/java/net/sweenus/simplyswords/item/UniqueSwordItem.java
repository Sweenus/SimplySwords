package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.sound.SoundManager;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.RunicMethods;

import java.util.List;

public class UniqueSwordItem extends SwordItem {

    public static int maxUseTime;

    public UniqueSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings.fireproof());
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {
        if (stack.getOrCreateNbt().getString("runic_power").isEmpty() && otherStack.isOf(ItemsRegistry.RUNEFUSED_GEM.get())) {

            //When clicked on with a Runefused Gem, copy the runic power and delete the gem
            String runicPowerSelection = otherStack.getOrCreateNbt().getString("runic_power");
            stack.getOrCreateNbt().putString("runic_power", runicPowerSelection);
            player.world.playSoundFromEntity(null, player, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1, 1);
            otherStack.decrement(1);
        }

        return false;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
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
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("greater"))
            tooltip.add(Text.translatable("item.simplyswords.greater_runic_power").formatted(Formatting.DARK_AQUA));

        if (itemStack.getOrCreateNbt().getString("runic_power").isEmpty()) {

            //tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.empty_runic_slot").formatted(Formatting.GRAY));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("freeze")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.freeze").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("wildfire")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.wildfire").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("slow")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.slow").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("swiftness")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.swiftness").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("float")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.float").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("zephyr")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.zephyr").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("shielding")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.shielding").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("stoneskin")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.stoneskin").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("frost_ward")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.frost_ward").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("trailblaze")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.trailblaze").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("active_defence")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.active_defence").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("weaken")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.weaken").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("unstable")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.unstable").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("momentum")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.momentum").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("imbued")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.imbued").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").contains("pincushion")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.pincushion").formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("ward")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.ward").formatted(Formatting.AQUA));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip3"));
            tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip4"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("immolation")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.immolation").formatted(Formatting.AQUA));
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip3"));
            tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip4"));

        }

    }
}