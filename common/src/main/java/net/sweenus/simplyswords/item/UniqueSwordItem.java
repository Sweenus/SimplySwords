package net.sweenus.simplyswords.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class UniqueSwordItem extends Item {

    String iRarity = "UNIQUE";

    public UniqueSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(settings.fireproof());
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        SimplySwordsAPI.inventoryTickGemSocketLogic(stack, world, entity, 50, 50);
        super.inventoryTick(stack, world, entity, slot);
    }

    // Subclasses that need World or old-style params can override this
    public void inventoryTickCompat(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {
        SimplySwordsAPI.onClickedGemSocketLogic(stack, otherStack, player);
        return false;
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            SimplySwordsAPI.postHitGemSocketLogic(stack, target, attacker);
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public Text getName(ItemStack stack) {

        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.HARBINGER.get())
                || this.getDefaultStack().isOf(ItemsRegistry.SUNFIRE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGISPEAR.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGIBLADE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGISCYTHE.get())) {
            this.iRarity = "LEGENDARY";
            return Text.translatable(this.getTranslationKey()).setStyle(Styles.LEGENDARY);
        }

        if (this.iRarity.equals("UNIQUE")) return Text.translatable(this.getTranslationKey()).setStyle(Styles.UNIQUE);
        else return Text.translatable(this.getTranslationKey()).setStyle(Styles.COMMON);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        List<Text> tooltip = new ArrayList<>();
        appendItemTooltip(itemStack, tooltipContext, tooltip, type);
        tooltip.forEach(textConsumer);
    }

    // Subclasses should override this instead of appendTooltip
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        generateDynamicTooltip(itemStack, tooltipContext, tooltip, type);
    }

    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects."+ this.asItem().getRegistryEntry().registryKey().getValue().getPath());
    }

    // Override this with your own id & paths
    protected void generateDynamicTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, tooltipContext, tooltip, type,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                getConfigPath());
    }

}
