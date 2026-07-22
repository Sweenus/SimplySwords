package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
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
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class SoulPyreSwordItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public SoulPyreSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
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
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            activateSoulTether(user);
            user.getItemCooldownManager().set(this, Config.uniqueEffects.soulpyre.cooldown);
        }
        return super.use(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        activateSoulTether(context.actor());
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.soulpyre.cooldown;
    }

    private void activateSoulTether(LivingEntity user) {
        int relocationDuration = 150;
        user.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.SOULTETHER), relocationDuration, 0, false, true));
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
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip6").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.soulpyre.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULPYRE::get));
        }

        @ValidatedInt.Restrict(min = 3)
        public int pulseCount = 10;
        @ValidatedDouble.Restrict(min = 11.0)
        public double radius = 12.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.92f;
        @ValidatedInt.Restrict(min = 1)
        public int heal = 1;
        @ValidatedInt.Restrict(min = 1)
        public int cooldown = 400;
    }
}
