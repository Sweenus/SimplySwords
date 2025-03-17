package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class DormantRelicSwordItem extends UniqueSwordItem {

    public DormantRelicSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

	@Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }
    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects");
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.dormantrelicsworditem.tooltip2").setStyle(Styles.TEXT));
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        if (this.asItem().equals(ItemsRegistry.DECAYING_RELIC.get())) {
            if (Screen.hasAltDown()) {
                tooltip.add(Text.translatable("item.simplyswords.decayingrelicsworditem.tooltip1").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.simplyswords.decayingrelicsworditem.tooltip2").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("item.simplyswords.decayingrelicsworditem.tooltip3").formatted(Formatting.GRAY));
            }
        }
    }
}