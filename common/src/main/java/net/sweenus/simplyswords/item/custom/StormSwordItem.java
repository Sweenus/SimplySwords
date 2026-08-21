package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
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
import net.sweenus.simplyswords.world.MjolnirStormManager;

import java.util.List;

public class StormSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public StormSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (attacker.getWorld() instanceof ServerWorld world) {
            HelperMethods.playHitSounds(attacker, target);
            MjolnirStormManager.onMeleeHit(world, stack, attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return MjolnirStormManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return MjolnirStormManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.mjolnir.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FIREWORK, ParticleTypes.FIREWORK, ParticleTypes.ELECTRIC_SPARK, false);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stormsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stormsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stormsworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.mjolnir.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "lightning");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MJOLNIR::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 700;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 200;
        @ValidatedInt.Restrict(min = 1)
        public int frequency = 10;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 10;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.45f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 2.12f;

        @ValidatedInt.Restrict(min = 1)
        public int conductiveDuration = 120;
        @ValidatedDouble.Restrict(min = 1.0)
        public double skyHeight = 12.0;

        @ValidatedFloat.Restrict(min = 0f)
        public float conductiveBurstDamageScaling = 0.25f;
        @ValidatedFloat.Restrict(min = 0f)
        public float conductiveBurstSpellScaling = 1.18f;
        @ValidatedDouble.Restrict(min = 0.1)
        public double conductiveBurstRadius = 2.5;
        @ValidatedDouble.Restrict(min = 0.0)
        public double conductiveBurstKnockback = 0.35;
        @ValidatedDouble.Restrict(min = 0.0)
        public double conductiveBurstKnockUp = 0.08;

        @ValidatedInt.Restrict(min = 0)
        public int finalBoltCount = 3;
        @ValidatedInt.Restrict(min = 1)
        public int finalBoltInterval = 3;
        @ValidatedFloat.Restrict(min = 0f)
        public float finalThunderclapDamageScaling = 0.9f;
        @ValidatedFloat.Restrict(min = 0f)
        public float finalThunderclapSpellScaling = 4.33f;
        @ValidatedDouble.Restrict(min = 0.1)
        public double finalThunderclapRadius = 6.0;
        @ValidatedDouble.Restrict(min = 0.0)
        public double finalThunderclapKnockback = 1.1;
        @ValidatedDouble.Restrict(min = 0.0)
        public double finalThunderclapKnockUp = 0.25;

    }
}
