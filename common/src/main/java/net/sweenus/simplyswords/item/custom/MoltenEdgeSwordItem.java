package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.MoltenEdgeAbilityManager;

import java.util.List;

public class MoltenEdgeSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {

    public MoltenEdgeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            MoltenEdgeAbilityManager.gainHeatFromMelee(stack, attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.stack().isOf(ItemsRegistry.MOLTEN_EDGE.get())
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && MoltenEdgeAbilityManager.isHeldMoltenEdge(context.actor(), context.stack())
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && MoltenEdgeAbilityManager.getHeat(context.actor(), context.stack()) > 0
                && !MoltenEdgeAbilityManager.isVenting(context.actor(), context.stack());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return MoltenEdgeAbilityManager.startVent(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.molten_edge.cooldown;
    }

    public void onSwing(ItemStack stack, ServerWorld world, LivingEntity user, Hand hand) {
        MoltenEdgeAbilityManager.tryFireRupture(world, user, stack, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        MoltenEdgeAbilityManager.tickHeldStack(stack, world, entity);
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_LAVA,
                ParticleTypes.LAVA, ParticleTypes.LARGE_SMOKE, true, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.moltenedgesworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.molten_edge.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MOLTEN_EDGE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 40;
        @ValidatedInt.Restrict(min = 1, max = 100)
        public int heatPerHit = 5;
        @ValidatedInt.Restrict(min = 1, max = 100)
        public int heatPerDamageTaken = 5;
        @ValidatedInt.Restrict(min = 1, max = 100)
        public int ventDrainPerTick = 1;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 5.0;
        @ValidatedInt.Restrict(min = 1)
        public int shockwaveTicks = 8;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float shockwaveDamageScaling = 0.8F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float shockwaveSpellScaling = 4.1306F;
        @ValidatedDouble.Restrict(min = 0.0)
        public double shockwaveKnockback = 0.9;
        @ValidatedInt.Restrict(min = 0)
        public int shockwaveIgniteSeconds = 4;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float ruptureDamageScaling = 0.65F;
        @ValidatedFloat.Restrict(min = 0.0F)
        public float ruptureSpellScaling = 3.0242F;
        @ValidatedDouble.Restrict(min = 1.0)
        public double ruptureLength = 7.0;
        @ValidatedDouble.Restrict(min = 0.5)
        public double ruptureWidth = 2.0;
        @ValidatedDouble.Restrict(min = 0.25)
        public double ruptureStepDistance = 1.0;
        @ValidatedDouble.Restrict(min = 0.0)
        public double ruptureKnockUp = 0.25;
        @ValidatedInt.Restrict(min = 0)
        public int ruptureIgniteSeconds = 2;
        @ValidatedInt.Restrict(min = 1)
        public int minimumSwingCooldownTicks = 4;
    }
}
