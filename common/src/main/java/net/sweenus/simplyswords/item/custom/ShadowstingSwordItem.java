package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;

import java.util.List;

public class ShadowstingSwordItem extends UniqueSwordItem {

    public ShadowstingSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient()
                && attacker instanceof ServerPlayerEntity serverPlayer
                && attacker.getWorld() instanceof ServerWorld serverWorld
                && target.isAlive()
                && attacker.getRandom().nextInt(100) < Config.uniqueEffects.shadowsting.chance) {
            ShadowstingShadowDanceManager.schedulePassiveCloneStrike(serverWorld, serverPlayer, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient() && world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity serverPlayer) {
            if (!ShadowstingShadowDanceManager.start(serverWorld, serverPlayer)) {
                return TypedActionResult.fail(itemStack);
            }
            user.getItemCooldownManager().set(itemStack.getItem(), Config.uniqueEffects.shadowsting.cooldown);
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.shadowsting.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SHADOWSTING::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 40;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 200;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 100;
        @ValidatedInt.Restrict(min = 1)
        public int strikeInterval = 8;
        @ValidatedInt.Restrict(min = 1)
        public int strikeRadius = 10;

    }
}
