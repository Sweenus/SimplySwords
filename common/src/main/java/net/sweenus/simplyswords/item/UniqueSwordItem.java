package net.sweenus.simplyswords.item;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.AwakeningFormRarity;
import net.sweenus.simplyswords.api.AwakeningFormStage;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.LegacyUniqueMigration;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public abstract class UniqueSwordItem extends SwordItem {

    String iRarity = "UNIQUE";

    public UniqueSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, LegacyWeaponAttributes.attackDamage(settings),
                LegacyWeaponAttributes.attackSpeed(settings), settings.fireproof());
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            if (LegacyUniqueMigration.migratePlayerSlot(stack, entity, slot)) {
                return;
            }
            AwakeningApi.ensureInitialized(stack);
            WeaponImplicitRegistry.getOrCreateWeaponImplicit(stack);
        }
        SimplySwordsAPI.inventoryTickGemSocketLogic(stack, world, entity, 100, 100);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player,
                             StackReference cursorStackReference) {
        SimplySwordsAPI.onClickedGemSocketLogic(
                stack,
                otherStack,
                player,
                cursorStackReference
        );
        return false;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            SimplySwordsAPI.postHitGemSocketLogic(stack, target, attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public Text getName(ItemStack stack) {
        AwakeningFormStage form = AwakeningApi.getFormStage(stack).orElse(null);
        if (form != null && form.displayTranslationKey().isPresent()) {
            MutableText name = Text.translatable(form.displayTranslationKey().get());
            if (form.rarity() == AwakeningFormRarity.LEGENDARY) {
                return name.setStyle(Styles.LEGENDARY);
            }
            if (form.rarity() == AwakeningFormRarity.UNIQUE) {
                return name.setStyle(Styles.UNIQUE);
            }
        }

        if (this.getDefaultStack().isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.HARBINGER.get())
                || this.getDefaultStack().isOf(ItemsRegistry.SUNFIRE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGISPEAR.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGIBLADE.get())
                || this.getDefaultStack().isOf(ItemsRegistry.MAGISCYTHE.get())) {
            this.iRarity = "LEGENDARY";
            return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.LEGENDARY);
        }

        if (this.iRarity.equals("UNIQUE")) return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.UNIQUE);
        else return Text.translatable(this.getTranslationKey(stack)).setStyle(Styles.COMMON);
    }

    /** Returns the rarity string for this item ({@code "UNIQUE"} or {@code "LEGENDARY"}). */
    public String getItemRarity() { return iRarity; }

    /** Stack-aware rarity used by tooltip integrations for awakening-dependent weapons. */
    public String getItemRarity(ItemStack stack) {
        AwakeningFormRarity formRarity = AwakeningApi.getFormStage(stack)
                .map(AwakeningFormStage::rarity)
                .orElse(AwakeningFormRarity.UNCHANGED);
        if (formRarity == AwakeningFormRarity.LEGENDARY) return "LEGENDARY";
        if (formRarity == AwakeningFormRarity.UNIQUE) return "UNIQUE";
        return getItemRarity();
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.addAll(WeaponImplicitRegistry.buildTooltipLines(itemStack, Screen.hasAltDown()));
        generateDynamicTooltip(itemStack, world, tooltip, tooltipContext);
    }

    protected static void appendAbilityCooldownTooltip(List<Text> tooltip, int cooldownTicks) {
        tooltip.add(Text.translatable("tooltip.simplyswords.ability_cooldown", formatCooldown(cooldownTicks)).setStyle(Styles.COOLDOWN));
    }

    private static String formatCooldown(int cooldownTicks) {
        if (cooldownTicks > 0 && cooldownTicks < 20) {
            return "<1s";
        }
        return (cooldownTicks / 20) + "s";
    }

    protected Identifier getConfigPath() {
        return new Identifier("simplyswords.unique_effects."+ this.asItem().getRegistryEntry().registryKey().getValue().getPath());
    }

    // Override this with your own id & paths
    protected void generateDynamicTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, world, tooltip, tooltipContext,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                getConfigPath());
    }

}
