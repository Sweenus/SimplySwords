package net.sweenus.simplyswords.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
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
        super( new Settings().group(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof().maxCount(1));
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {

        if(stack.getOrCreateNbt().getString("nether_power").isEmpty()) {

            String netherfusedPowerSelection = HelperMethods.chooseNetherfusedPower();
            stack.getOrCreateNbt().putString("nether_power", netherfusedPowerSelection);
        }

        return false;
    }

    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player)
    {
        if(world.isClient) return;

        String netherfusedPowerSelection = HelperMethods.chooseNetherfusedPower();
        stack.getOrCreateNbt().putString("nether_power", netherfusedPowerSelection);

    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD);
    }


    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));

        if (itemStack.getOrCreateNbt().getString("nether_power").contains("greater"))
            tooltip.add(Text.translatable("item.simplyswords.greater_nether_power").formatted(Formatting.DARK_AQUA, Formatting.BOLD));

        if (itemStack.getOrCreateNbt().getString("nether_power").isEmpty()) {
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2"));

        }
        if (itemStack.getOrCreateNbt().getString("nether_power").equals("echo")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo").formatted(Formatting.RED));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description2"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.echo.description3"));

        }
        if (itemStack.getOrCreateNbt().getString("nether_power").equals("berserk")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk").formatted(Formatting.RED));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description2"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.berserk.description3"));

        }
        if (itemStack.getOrCreateNbt().getString("nether_power").equals("radiance")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance").formatted(Formatting.RED));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description2"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.radiance.description3"));

        }
        if (itemStack.getOrCreateNbt().getString("nether_power").equals("onslaught")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught").formatted(Formatting.RED));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description2"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description3"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description4"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description5"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.onslaught.description6"));

        }
        if (itemStack.getOrCreateNbt().getString("nether_power").equals("nullification")) {

            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification").formatted(Formatting.RED));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description2"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description3"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description4"));
            tooltip.add(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power.nullification.description5"));

        }

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.gem_description").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.simplyswords.gem_description2").formatted(Formatting.GRAY, Formatting.ITALIC));

    }
}
