package net.sweenus.simplyswords.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.item.component.WeaponImplicitComponent;

public record WeaponImplicitDefinition(
        Identifier id,
        Identifier weaponType,
        int minValue,
        int maxValue,
        DamageHandler damageHandler,
        HitHandler hitHandler,
        IncomingDamageHandler incomingDamageHandler,
        TooltipFormatter tooltipFormatter
) {
    public WeaponImplicitDefinition {
        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue cannot be greater than maxValue");
        }
    }

    public Text formatTooltip(WeaponImplicitComponent component) {
        if (tooltipFormatter != null) {
            return tooltipFormatter.format(component);
        }
        return Text.translatable("tooltip.simplyswords.implicit.generic", component.value());
    }

    @FunctionalInterface
    public interface DamageHandler {
        float modify(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface HitHandler {
        void onHit(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, LivingEntity attacker, float damage);
    }

    @FunctionalInterface
    public interface IncomingDamageHandler {
        boolean shouldCancel(ItemStack stack, WeaponImplicitComponent component, LivingEntity bearer, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface TooltipFormatter {
        Text format(WeaponImplicitComponent component);
    }
}
