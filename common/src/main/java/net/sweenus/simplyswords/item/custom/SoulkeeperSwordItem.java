package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
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
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.SoulkeeperLanternManager;

import java.util.List;

public class SoulkeeperSwordItem extends UniqueSwordItem implements TwoHandedWeapon {
    public SoulkeeperSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            SoulkeeperLanternManager.onSoulkeeperHit(attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient() && world instanceof ServerWorld && user instanceof ServerPlayerEntity serverPlayer) {
            if (hand != Hand.MAIN_HAND || serverPlayer.getItemCooldownManager().isCoolingDown(itemStack.getItem())) {
                return TypedActionResult.fail(itemStack);
            }
            SoulkeeperLanternManager.activate(serverPlayer, itemStack);
            serverPlayer.getItemCooldownManager().set(itemStack.getItem(), Config.uniqueEffects.soulkeeper.cooldown);
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && entity instanceof ServerPlayerEntity serverPlayer && serverPlayer.getMainHandStack().equals(stack)) {
            SoulkeeperLanternManager.tickPlayerFromItem(serverPlayer, stack);
        }
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SOUL, ParticleTypes.SOUL,
                ParticleTypes.SPORE_BLOSSOM_AIR, false);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.soulsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.soulkeeper.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULKEEPER::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 800;
        @ValidatedInt.Restrict(min = 1)
        public int activeExtraLanternDuration = 300;
        @ValidatedDouble.Restrict(min = 0.0)
        public double lanternDamageMultiplier = 0.3;
        @ValidatedDouble.Restrict(min = 0.0)
        public double speedIncreasePerHit = 0.20;
        @ValidatedDouble.Restrict(min = 0.0)
        public double speedLossPerSecond = 0.15;
        @ValidatedDouble.Restrict(min = 1.0)
        public double maxSpeedMultiplier = 6.0;
        @ValidatedDouble.Restrict(min = 0.25)
        public double orbitRadius = 2.65;
    }
}
