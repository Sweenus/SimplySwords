package net.sweenus.simplyswords.item;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.client.api.SimplySwordsClientAPI;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimplySwordsSwordItem extends SwordItem {
    String[] repairIngredient;

    public SimplySwordsSwordItem(ToolMaterial toolMaterial, Settings settings, String... repairIngredient) {
        super(toolMaterial, LegacyWeaponAttributes.attackDamage(settings), LegacyWeaponAttributes.attackSpeed(settings), settings);
        this.repairIngredient = repairIngredient;
    }

    public SimplySwordsSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, LegacyWeaponAttributes.attackDamage(settings), LegacyWeaponAttributes.attackSpeed(settings), settings);
        this.repairIngredient = new String[]{};
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        if (repairIngredient.length == 0) return super.canRepair(stack, ingredient);

        List<Item> potentialIngredients = new ArrayList<>(List.of());
        Arrays.stream(repairIngredient).toList().forEach(repIngredient ->
            potentialIngredients.add(
                    Registries.ITEM.get(new Identifier(repIngredient))));


        return potentialIngredients.contains(ingredient.getItem());
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient) {
            WeaponImplicitRegistry.getOrCreateWeaponImplicit(stack);
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        tooltip.addAll(WeaponImplicitRegistry.buildTooltipLines(itemStack, Screen.hasAltDown()));
        generateDynamicTooltip(itemStack, world, tooltip, tooltipContext);
    }

    // Override this with your own id & paths
    protected void generateDynamicTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        SimplySwordsClientAPI.generateDynamicTooltip(itemStack, world, tooltip, tooltipContext,
                SimplySwords.MOD_ID,
                "oracle_index:books/simplyswords/weapon-types",
                "oracle_index:books/simplyswords/unique-weapons",
                "oracle_index:books/simplyswords/runic-powers",
                null);
    }


}
