package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.RunicMethods;

import java.util.List;

public class RunicSwordItem extends SwordItem {
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
            //SPEED
            if (stack.getOrCreateNbt().getString("runic_power").equals("speed")) {
                RunicMethods.postHitRunicSpeed(stack, target, attacker);
            }
            //LEVITATION
            if (stack.getOrCreateNbt().getString("runic_power").equals("levitation")) {
                RunicMethods.postHitRunicLevitation(stack, target, attacker);
            }
            //ZEPHYR
            if (stack.getOrCreateNbt().getString("runic_power").equals("zephyr")) {
                RunicMethods.postHitRunicZephyr(stack, target, attacker);
            }
            //SHIELDING
            if (stack.getOrCreateNbt().getString("runic_power").equals("shielding")) {
                RunicMethods.postHitRunicShielding(stack, target, attacker);
            }
            //STONESKIN
            if (stack.getOrCreateNbt().getString("runic_power").equals("stoneskin")) {
                RunicMethods.postHitRunicStoneskin(stack, target, attacker);
            }
            //TRAILBLAZE
            if (stack.getOrCreateNbt().getString("runic_power").equals("trailblaze")) {
                RunicMethods.postHitRunicTrailblaze(stack, target, attacker);
            }
            //WEAKEN
            if (stack.getOrCreateNbt().getString("runic_power").equals("weaken")) {
                RunicMethods.postHitRunicWeaken(stack, target, attacker);
            }
        }

        return super.postHit(stack, target, attacker);

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

        if (itemStack.getOrCreateNbt().getString("runic_power").isEmpty()) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("freeze")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("wildfire")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("slow")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("speed")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("levitation")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("zephyr")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("shielding")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("stoneskin")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("frost_ward")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("trailblaze")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("active_defence")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("weaken")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip3"));

        }
        if (itemStack.getOrCreateNbt().getString("runic_power").equals("unstable")) {

            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip1").formatted(Formatting.AQUA, Formatting.BOLD));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip2"));
            tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip3"));

        }

    }
}
