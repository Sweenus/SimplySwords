package net.sweenus.simplyswords.item;

import me.fzzyhmstrs.fzzy_config.util.ValidationResult;
import net.minecraft.client.gui.screen.Screen;
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
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.GemPowerFiller;
import net.sweenus.simplyswords.power.PowerType;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class RunefusedGemItem extends Item implements GemPowerFiller {

    public RunefusedGemItem() {
        super(new Settings().arch$tab(SimplySwords.SIMPLYSWORDS).rarity(Rarity.EPIC).fireproof().maxCount(1));
    }

    @Override
    public ValidationResult<GemPowerComponent> fill(ItemStack stack, GemPowerComponent component) {
        GemPowerComponent gemComponent = SimplySwordsAPI.getComponent(stack);
        if (!gemComponent.hasRunicPower()
                || !gemComponent.hasRunicSlotFilled()
                || !component.hasRunicPower()) {
            return ValidationResult.Companion.error(component, "Can't socket to the provided component");
        }
        return ValidationResult.Companion.success(component.fill(
            (hasRunic, oldRunic) -> gemComponent.runicPower(),
            (hasNether, oldNether) -> oldNether
        ));
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {

        // Must roll on BOTH sides - do not add an isClient guard here.
        //
        // Which side owns the resulting stack depends on the game mode. In survival the
        // server runs this via ClickSlotC2SPacket and syncs the result back. In creative the
        // client is authoritative: clicks go through ClientPlayerInteractionManager
        // .clickCreativeStack, and ServerPlayNetworkHandler.onCreativeInventoryAction writes
        // the client's stack straight into the slot without ever calling Item#onClicked. A
        // server-only roll is therefore silently discarded in creative and the gem can never
        // be identified.
        //
        // The two sides can pick different powers, but the authoritative side always wins and
        // the other is corrected on the next slot sync.
        if (SimplySwordsAPI.needsGemPowerRoll(stack)) {
            ComponentTypeRegistry.GEM_POWER.set(stack, GemPowerComponent.runic(GemPowerRegistry.gemRandomPower(PowerType.RUNEFUSED)));
        }
        return false;
    }


    @Override
    public void onCraft(ItemStack stack, World world, PlayerEntity player) {
        if (world.isClient) return;

        if (SimplySwordsAPI.needsGemPowerRoll(stack)) {
            ComponentTypeRegistry.GEM_POWER.set(stack, GemPowerComponent.runic(GemPowerRegistry.gemRandomPower(PowerType.RUNEFUSED)));
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.RUNIC);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));

        GemPowerComponent component = SimplySwordsAPI.getComponent(itemStack);

        if(component.isEmpty()) {
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip1").setStyle(Styles.RUNIC));
            tooltip.add(Text.translatable("item.simplyswords.unidentifiedsworditem.tooltip2").setStyle(Styles.TEXT));
        } else {
            component.appendTooltip(itemStack, world, tooltip, tooltipContext);
        }
        tooltip.add(Text.literal(""));
        generateDynamicTooltip(itemStack, world, tooltip, tooltipContext);
        if (Screen.hasAltDown()) {
            tooltip.add(Text.translatable("item.simplyswords.gem_description").formatted(Formatting.GRAY, Formatting.ITALIC));
            tooltip.add(Text.translatable("item.simplyswords.gem_description2").formatted(Formatting.GRAY, Formatting.ITALIC));
        }
    }

    protected void generateDynamicTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, world, tooltip, tooltipContext,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                null);
    }

}
