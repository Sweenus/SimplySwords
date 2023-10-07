package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class NetherfusedGemItem extends Item {

    public NetherfusedGemItem() {
        super(new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof().maxCount(1));
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {

        if (stack.getOrCreateNbt().getString("nether_power").isEmpty()) {

            String netherfusedPowerSelection = HelperMethods.chooseNetherfusedPower();
            stack.getOrCreateNbt().putString("nether_power", netherfusedPowerSelection);
        }

        return false;
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if (world.isClient) return;

        String netherfusedPowerSelection = HelperMethods.chooseNetherfusedPower();
        stack.getOrCreateNbt().putString("nether_power", netherfusedPowerSelection);

    }

    @Override
    public Text getName(ItemStack stack) {
        Style LEGENDARY = HelperMethods.getStyle("legendary");
        return Text.translatable(this.getTranslationKey(stack)).setStyle(LEGENDARY);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style LEGENDARY = HelperMethods.getStyle("legendary");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));

        if (itemStack.getOrCreateNbt().getString("nether_power").isEmpty()) {
            tooltip.add(Text.translatable("item.simplyswords.netherfused_gem.tooltip1").setStyle(LEGENDARY));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2").setStyle(TEXT));
        } else {
            if (itemStack.getOrCreateNbt().getString("nether_power").contains("greater")) {
                tooltip.add(Text.translatable("item.simplyswords.greater_nether_power").setStyle(LEGENDARY));
            }
            switch (itemStack.getOrCreateNbt().getString("nether_power")) {
                case "echo" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description3").setStyle(TEXT));
                }
                case "berserk" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description3").setStyle(TEXT));
                }
                case "radiance" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description3").setStyle(TEXT));
                }
                case "onslaught" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description3").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description4").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description5").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description6").setStyle(TEXT));
                }
                case "nullification" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description3").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description4").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description5").setStyle(TEXT));
                }
                case "precise" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.precise").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.precise.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.precise.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.precise.description3").setStyle(TEXT));
                }
                case "mighty" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.mighty").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.mighty.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.mighty.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.mighty.description3").setStyle(TEXT));
                }
                case "stealthy" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.stealthy").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.stealthy.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.stealthy.description2").setStyle(TEXT));
                }
                case "renewed" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.renewed").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.renewed.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.renewed.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.renewed.description3").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.renewed.description4").setStyle(TEXT));
                }
                case "accelerant" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.accelerant").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.accelerant.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.accelerant.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.accelerant.description3").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.accelerant.description4").setStyle(TEXT));
                }
                case "leaping" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.leaping").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.leaping.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.leaping.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.leaping.description3").setStyle(TEXT));
                }
                case "spellshield" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellshield").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellshield.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellshield.description2").setStyle(TEXT));
                }
                case "spellforged" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellforged").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellforged.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellforged.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellforged.description3").setStyle(TEXT));
                }
                case "soulshock" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.soulshock").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.soulshock.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.soulshock.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.soulshock.description3").setStyle(TEXT));
                }
                case "spellstandard" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellstandard").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellstandard.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellstandard.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.spellstandard.description3").setStyle(TEXT));
                }
                case "warstandard" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.warstandard").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.warstandard.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.warstandard.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.warstandard.description3").setStyle(TEXT));
                }
                case "deception" -> {
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.deception").setStyle(LEGENDARY));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.deception.description").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.deception.description2").setStyle(TEXT));
                    tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.deception.description3").setStyle(TEXT));
                }
            }
        }
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gem_description").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.simplyswords.gem_description2").formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
