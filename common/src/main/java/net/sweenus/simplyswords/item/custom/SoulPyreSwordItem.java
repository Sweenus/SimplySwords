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
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.SoulPyreAbilityManager;

import java.util.List;

public class SoulPyreSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public SoulPyreSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return SoulPyreAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return SoulPyreAbilityManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.soulpyre.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SOUL_FIRE_FLAME,
                ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.MYCELIUM, true);
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SMALL_FLAME,
                ParticleTypes.SMALL_FLAME, ParticleTypes.MYCELIUM, false);
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SMOKE, ParticleTypes.SMOKE,
                ParticleTypes.MYCELIUM, false);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip6").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.soulpyre.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULPYRE::get));
        }

        @ValidatedInt.Restrict(min = 3)
        public int pulseCount = 10;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 600;
        @ValidatedInt.Restrict(min = 1)
        public int collapseDuration = 60;
        @ValidatedFloat.Restrict(min = 1.5f)
        public float startingRadius = 3.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float radiusGrowthPerKill = 1.0f;
        @ValidatedDouble.Restrict(min = 11.0)
        public double radius = 8.0;
        @ValidatedFloat.Restrict(min = 2.0f)
        public float verticalRange = 8.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.74f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 5.29f;
        @ValidatedInt.Restrict(min = 1)
        public int heal = 1;
        @ValidatedInt.Restrict(min = 1)
        public int wispVolleySize = 5;
        @ValidatedFloat.Restrict(min = 0f)
        public float pulseDamageBonusPerSoul = 0.05f;
        @ValidatedFloat.Restrict(min = 0f)
        public float requiemDamageBonusPerSoul = 0.12f;
        @ValidatedFloat.Restrict(min = 0f)
        public float wispDamageMultiplier = 0.30f;
        @ValidatedFloat.Restrict(min = 0f)
        public float requiemHealingPerSoul = 1.0f;
        @ValidatedInt.Restrict(min = 1)
        public int cooldown = 1400;
    }
}
