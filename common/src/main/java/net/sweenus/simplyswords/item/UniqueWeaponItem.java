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
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
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

public abstract class UniqueWeaponItem extends SwordItem {

    String iRarity = "UNIQUE";

    public UniqueWeaponItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, LegacyWeaponAttributes.attackDamage(settings),
                LegacyWeaponAttributes.attackSpeed(settings), settings.fireproof());
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return getUniqueWeaponMaxUseTime(stack);
    }

    protected int getUniqueWeaponMaxUseTime(ItemStack stack) {
        return 0;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useUniqueWeapon(world, user, hand);
    }

    protected TypedActionResult<ItemStack> useUniqueWeapon(World world, PlayerEntity user, Hand hand) {
        return super.use(world, user, hand);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        tickUniqueWeaponUse(world, user, stack, remainingUseTicks);
    }

    protected void tickUniqueWeaponUse(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        super.usageTick(world, user, stack, remainingUseTicks);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        return finishUniqueWeaponUse(stack, world, user);
    }

    protected ItemStack finishUniqueWeaponUse(ItemStack stack, World world, LivingEntity user) {
        return super.finishUsing(stack, world, user);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        stopUniqueWeaponUse(stack, world, user, remainingUseTicks);
    }

    protected void stopUniqueWeaponUse(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        super.onStoppedUsing(stack, world, user, remainingUseTicks);
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

    public String getItemRarity() { return iRarity; }

    public String getItemRarity(ItemStack stack) {
        AwakeningFormRarity formRarity = AwakeningApi.getFormStage(stack)
                .map(AwakeningFormStage::rarity)
                .orElse(AwakeningFormRarity.UNCHANGED);
        if (formRarity == AwakeningFormRarity.LEGENDARY) return "LEGENDARY";
        if (formRarity == AwakeningFormRarity.UNIQUE) return "UNIQUE";
        return getItemRarity();
    }

    public String getTooltipWeaponType(ItemStack stack) {
        return "";
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        appendUniqueWeaponTooltip(itemStack, world, tooltip, tooltipContext);
    }

    protected void appendUniqueWeaponTooltip(ItemStack itemStack, World world, List<Text> tooltip,
                                             net.minecraft.client.item.TooltipContext tooltipContext) {
        appendSharedUniqueWeaponTooltip(itemStack, world, tooltip, tooltipContext);
    }

    protected final void appendSharedUniqueWeaponTooltip(ItemStack itemStack, World world, List<Text> tooltip,
                                                         net.minecraft.client.item.TooltipContext tooltipContext) {
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

    protected String getConfigPathRoot() {
        return "simplyswords.unique_effects";
    }

    protected Identifier getConfigPath() {
        return new Identifier(getConfigPathRoot() + "."
                + this.asItem().getRegistryEntry().registryKey().getValue().getPath());
    }

    protected void generateDynamicTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, world, tooltip, tooltipContext,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                getConfigPath());
    }

}
