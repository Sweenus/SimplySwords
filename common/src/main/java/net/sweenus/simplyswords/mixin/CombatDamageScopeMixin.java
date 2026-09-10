package net.sweenus.simplyswords.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.api.combat.DamageProvenanceCarrier;
import net.sweenus.simplyswords.api.combat.FireProvenanceCarrier;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LivingEntity.class, priority = 2000)
public abstract class CombatDamageScopeMixin {
    @WrapMethod(method = "damage")
    private boolean simplyswords$damageScope(DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target.getWorld().isClient()) return original.call(source, amount);
        var provenance = CombatProvenanceApi.damage(source, target);
        if (source.isOf(DamageTypes.ON_FIRE)) {
            provenance = ((FireProvenanceCarrier) target).simplyswords$getFireProvenance();
        }
        if (source instanceof DamageProvenanceCarrier carrier) {
            boolean matches = carrier.simplyswords$matches(target);
            var bound = carrier.simplyswords$take();
            if (matches) provenance = bound;
        }
        try (var ignored = CombatProvenanceApi.scope(provenance)) { return original.call(source, amount); }
    }
}
