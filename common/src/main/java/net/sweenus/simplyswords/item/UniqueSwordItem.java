package net.sweenus.simplyswords.item;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.RunicMethods;

import java.util.List;

public class UniqueSwordItem extends SwordItem {

    public static int maxUseTime;
    String iRarity = "UNIQUE";

    public UniqueSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings.fireproof());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        //Socket rolling
        if (stack.getOrCreateNbt().getString("runic_power").isEmpty() && stack.getOrCreateNbt().getString("nether_power").isEmpty()) {
            float socketChance = (float) (Math.random() * 100);
            float socketChance2 = (float) (Math.random() * 100);
            if (socketChance > 49)
                stack.getOrCreateNbt().putString("runic_power", "socket_empty");
            else if (socketChance < 50)
                stack.getOrCreateNbt().putString("runic_power", "no_socket");
            if (socketChance2 > 49)
                stack.getOrCreateNbt().putString("nether_power", "socket_empty");
            else if (socketChance2 < 50)
                stack.getOrCreateNbt().putString("nether_power", "no_socket");
        }


        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {
        if (stack.getOrCreateNbt().getString("runic_power").equals("socket_empty") ||
                (stack.getOrCreateNbt().getString("nether_power").equals("socket_empty")) &&
                        SimplySwords.generalConfig.enableUniqueGemSockets) {

            if (otherStack.isOf(ItemsRegistry.RUNEFUSED_GEM.get())) {
                //When clicked on with a Runefused Gem, copy the runic power and delete the gem
                String runicPowerSelection = otherStack.getOrCreateNbt().getString("runic_power");
                stack.getOrCreateNbt().putString("runic_power", runicPowerSelection);
                player.getWorld().playSoundFromEntity(null, player, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1, 1);
                otherStack.decrement(1);
            }
            else if (otherStack.isOf(ItemsRegistry.NETHERFUSED_GEM.get())) {
                //When clicked on with a Netherfused Gem, copy the nether power and delete the gem
                String netherPowerSelection = otherStack.getOrCreateNbt().getString("nether_power");
                stack.getOrCreateNbt().putString("nether_power", netherPowerSelection);
                player.getWorld().playSoundFromEntity(null, player, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1, 1);
                otherStack.decrement(1);
            }
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



            //ECHO
            if (stack.getOrCreateNbt().getString("nether_power").equals("echo")) {
                RunicMethods.postHitNetherEcho(stack, target, attacker);
            }
            //BERSERK
            if (stack.getOrCreateNbt().getString("nether_power").equals("berserk")) {
                RunicMethods.postHitNetherBerserk(stack, target, attacker);
            }
            //RADIANCE
            if (stack.getOrCreateNbt().getString("nether_power").equals("radiance")) {
                RunicMethods.postHitNetherRadiance(stack, target, attacker);
            }
            //ONSLAUGHT
            if (stack.getOrCreateNbt().getString("nether_power").equals("onslaught")) {
                RunicMethods.postHitNetherOnslaught(stack, target, attacker);
            }
            //NULLIFICATION
            if (stack.getOrCreateNbt().getString("nether_power").equals("nullification")) {
                RunicMethods.postHitNetherNullification(stack, target, attacker);
            }

        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public Text getName(ItemStack stack) {

        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())
        || this.getDefaultStack().isOf(ItemsRegistry.HARBINGER.get())
        || this.getDefaultStack().isOf(ItemsRegistry.SUNFIRE.get())) {
            this.iRarity = "LEGENDARY";
        }

        Style COMMON = HelperMethods.getStyle("common");
        Style UNIQUE = HelperMethods.getStyle("unique");
        Style LEGENDARY = HelperMethods.getStyle("legendary");

        if (this.iRarity.contains("LEGENDARY"))
            return Text.translatable(this.getTranslationKey(stack)).setStyle(LEGENDARY);
        if (this.iRarity.contains("UNIQUE"))
            return Text.translatable(this.getTranslationKey(stack)).setStyle(UNIQUE);
        else
            return Text.translatable(this.getTranslationKey(stack)).setStyle(COMMON);

    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        Style RUNIC = HelperMethods.getStyle("runic");
        Style NETHERFUSED = HelperMethods.getStyle("legendary");
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        if (Screen.hasAltDown()) {
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("greater"))
                tooltip.add(Text.translatable("item.simplyswords.greater_runic_power").setStyle(RUNIC));

            if (itemStack.getOrCreateNbt().getString("runic_power").contains("socket_empty")) {

                tooltip.add(Text.translatable("item.simplyswords.empty_runic_slot").formatted(Formatting.GRAY));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").equals("freeze")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.freeze").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.freezesworditem.tooltip2").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").equals("wildfire")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.wildfire").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.wildfiresworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("slow")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.slow").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.slownesssworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("swiftness")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.swiftness").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.speedsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("float")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.float").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.levitationsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("zephyr")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.zephyr").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.zephyrsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("shielding")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.shielding").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.shieldingsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("stoneskin")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.stoneskin").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.stoneskinsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").equals("frost_ward")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.frost_ward").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.frostwardsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("trailblaze")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.trailblaze").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.trailblazesworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").equals("active_defence")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.active_defence").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.activedefencesworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("weaken")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.weaken").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.weakensworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").equals("unstable")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.unstable").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.unstablesworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("momentum")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.momentum").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.momentumsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("imbued")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.imbued").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.imbuedsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").contains("pincushion")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.pincushion").setStyle(RUNIC));
                tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.pincushionsworditem.tooltip3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").equals("ward")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.ward").setStyle(RUNIC));
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
                tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip3").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.wardsworditem.tooltip4").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("runic_power").equals("immolation")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.runefused_power.immolation").setStyle(RUNIC));
                tooltip.add(Text.literal(""));
                tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
                tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip3").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.immolationsworditem.tooltip4").setStyle(TEXT));

            }

            tooltip.add(Text.literal(""));
            if (itemStack.getOrCreateNbt().getString("nether_power").contains("socket_empty")) {

                tooltip.add(Text.translatable("item.simplyswords.empty_nether_slot").formatted(Formatting.GRAY));

            }

            if (itemStack.getOrCreateNbt().getString("nether_power").equals("echo")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo").setStyle(NETHERFUSED));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("nether_power").equals("berserk")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk").setStyle(NETHERFUSED));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("nether_power").equals("radiance")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance").setStyle(NETHERFUSED));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description3").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("nether_power").equals("onslaught")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught").setStyle(NETHERFUSED));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description3").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description4").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description5").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description6").setStyle(TEXT));

            }
            if (itemStack.getOrCreateNbt().getString("nether_power").equals("nullification")) {

                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification").setStyle(NETHERFUSED));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description2").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description3").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description4").setStyle(TEXT));
                tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description5").setStyle(TEXT));

            }

        } else if (!itemStack.getOrCreateNbt().getString("runic_power").contains("no_socket") ||
                !itemStack.getOrCreateNbt().getString("nether_power").contains("no_socket")) {
            tooltip.add(Text.translatable("item.simplyswords.common.showtooltip").formatted(Formatting.GRAY));
        }
    }
}