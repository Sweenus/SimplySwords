package net.sweenus.simplyswords.api;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.UniqueWeaponItem;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.GemPowerFiller;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;

//
// Config-driven gem socket support
//
public final class AdditionalGemSocketApi {

    private AdditionalGemSocketApi() {
    }

    //
    // Returns whether this item is currently selected by the additional socket
    // config. Native unique and runic weapons retain their existing handling.
    //
    public static boolean isConfigured(ItemStack stack) {
        if (!canReceiveAdditionalSockets(stack)) {
            return false;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        for (String rawSelector : Config.general.additionalGemSocketItems) {
            if (rawSelector == null || rawSelector.isBlank()) {
                continue;
            }

            boolean tagSelector = rawSelector.charAt(0) == '#';
            String value = tagSelector ? rawSelector.substring(1) : rawSelector;
            Identifier selectorId = Identifier.tryParse(value);
            if (selectorId == null) {
                continue;
            }

            if (tagSelector) {
                TagKey<net.minecraft.item.Item> tag = TagKey.of(Registries.ITEM.getKey(), selectorId);
                if (stack.isIn(tag)) {
                    return true;
                }
            } else if (selectorId.equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    //
    // Returns whether this stack was initialized by the additional socket
    // system. The persistent marker deliberately survives later config edits.
    //
    public static boolean hasAdditionalSockets(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && ComponentTypeRegistry.ADDITIONAL_GEM_SOCKETS.getOrDefault(stack, false);
    }

    //
    // Returns whether generic socket hooks should process this stack.
    //
    public static boolean isManaged(ItemStack stack) {
        return hasAdditionalSockets(stack) || isConfigured(stack);
    }

    //
    // Adds both socket types to a configured stack while preserving any powers
    // already stored in its gem component.
    //
    // return true when the stack is managed after this call
    //
    public static boolean ensureInitialized(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (hasAdditionalSockets(stack)) {
            openBothSockets(stack);
            return true;
        }
        if (!isConfigured(stack)) {
            return false;
        }

        openBothSockets(stack);
        if (!hasAdditionalSockets(stack)) {
            ComponentTypeRegistry.ADDITIONAL_GEM_SOCKETS.set(stack, true);
        }
        return true;
    }

    //
    // Marks both sockets as present, writing only when that is not already the case.
    //
    // This runs from ItemStack#inventoryTick for every stack in the game, so an
    // unconditional set would rewrite the component every tick — making storage mods
    // (Refined Storage, Create, AE2) see the stack as perpetually changed.
    //
    private static void openBothSockets(ItemStack stack) {
        GemPowerComponent existing = ComponentTypeRegistry.GEM_POWER.getOrDefault(stack, GemPowerComponent.DEFAULT);
        if (existing.hasRunicPower() && existing.hasNetherPower()) {
            return;
        }
        ComponentTypeRegistry.GEM_POWER.set(stack, new GemPowerComponent(
                true,
                true,
                existing.runicPower(),
                existing.netherPower()
        ));
    }

    public static GemPowerComponent getTooltipComponent(ItemStack stack) {
        if (hasAdditionalSockets(stack)) {
            GemPowerComponent existing = ComponentTypeRegistry.GEM_POWER.getOrDefault(stack, GemPowerComponent.DEFAULT);
            return new GemPowerComponent(true, true, existing.runicPower(), existing.netherPower());
        }
        if (!isConfigured(stack)) {
            return GemPowerComponent.DEFAULT;
        }

        GemPowerComponent existing = ComponentTypeRegistry.GEM_POWER.getOrDefault(stack, GemPowerComponent.DEFAULT);
        return new GemPowerComponent(true, true, existing.runicPower(), existing.netherPower());
    }

    private static boolean canReceiveAdditionalSockets(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getMaxCount() != 1) {
            return false;
        }
        if (stack.getItem() instanceof UniqueWeaponItem
                || stack.getItem() instanceof RunicSwordItem
                || stack.getItem() instanceof GemPowerFiller) {
            return false;
        }
        return true;
    }
}
