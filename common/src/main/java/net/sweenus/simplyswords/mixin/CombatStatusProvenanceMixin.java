package net.sweenus.simplyswords.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.sweenus.simplyswords.api.combat.CombatProvenance;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.api.combat.ProvenanceCarrier;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectInstance.class)
public abstract class CombatStatusProvenanceMixin implements ProvenanceCarrier {
    @Unique private CombatProvenance simplyswords$provenance;
    @Shadow private StatusEffectInstance hiddenEffect;

    @Inject(method = "<init>(Lnet/minecraft/registry/entry/RegistryEntry;IIZZZLnet/minecraft/entity/effect/StatusEffectInstance;)V", at = @At("RETURN"))
    private void simplyswords$capture(CallbackInfo ci) {
        if (simplyswords$provenance == null) simplyswords$provenance = CombatProvenanceApi.current();
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void simplyswords$copy(StatusEffectInstance other, CallbackInfo ci) {
        simplyswords$provenance = ((ProvenanceCarrier) other).simplyswords$getProvenance();
    }

    @WrapMethod(method = "upgrade")
    private boolean simplyswords$upgrade(StatusEffectInstance other, Operation<Boolean> original) {
        StatusEffectInstance self = (StatusEffectInstance) (Object) this;
        int oldAmplifier = self.getAmplifier();
        int oldDuration = self.getDuration();
        boolean changed = original.call(other);
        if (changed && (self.getAmplifier() != oldAmplifier || self.getDuration() != oldDuration)) {
            simplyswords$provenance = ((ProvenanceCarrier) other).simplyswords$getProvenance();
        }
        return changed;
    }

    @WrapMethod(method = "update")
    private boolean simplyswords$tick(LivingEntity entity, Runnable callback, Operation<Boolean> original) {
        if ((Object) this instanceof SimplySwordsStatusEffectInstance custom) custom.resolveSource(entity);
        try (var ignored = CombatProvenanceApi.scope(simplyswords$provenance == null ? null
                : simplyswords$provenance.deliveredBy(CombatProvenance.DAMAGE_OVER_TIME))) {
            return original.call(entity, callback);
        }
    }

    @WrapMethod(method = "onEntityDamage")
    private void simplyswords$damageCallback(LivingEntity entity, DamageSource source,
                                             float amount, Operation<Void> original) {
        try (var ignored = CombatProvenanceApi.scope(simplyswords$provenance)) {
            original.call(entity, source, amount);
        }
    }

    @WrapMethod(method = "onEntityRemoval")
    private void simplyswords$removal(LivingEntity entity, Entity.RemovalReason reason,
                                      Operation<Void> original) {
        try (var ignored = CombatProvenanceApi.scope(simplyswords$provenance)) { original.call(entity, reason); }
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void simplyswords$save(CallbackInfoReturnable<NbtElement> cir) {
        if (cir.getReturnValue() instanceof NbtCompound nbt) {
            if ((Object) this instanceof SimplySwordsStatusEffectInstance custom) nbt.put("simplyswords_effect", custom.writeSource());
            if (simplyswords$provenance != null) nbt.put("simplyswords_provenance", simplyswords$provenance.write());
            if (hiddenEffect != null) nbt.put("simplyswords_hidden", hiddenEffect.writeNbt());
        }
    }

    @Inject(method = "fromNbt", at = @At("RETURN"), cancellable = true)
    private static void simplyswords$load(NbtCompound nbt, CallbackInfoReturnable<StatusEffectInstance> cir) {
        StatusEffectInstance value = cir.getReturnValue();
        if (value == null) return;
        if (nbt.contains("simplyswords_effect")) {
            var custom = new SimplySwordsStatusEffectInstance(
                    value.getEffectType(), value.getDuration(), value.getAmplifier(), value.isAmbient(),
                    value.shouldShowParticles(), value.shouldShowIcon());
            custom.readSource(nbt.getCompound("simplyswords_effect"));
            value = custom;
            cir.setReturnValue(value);
        }
        ((ProvenanceCarrier) value).simplyswords$readProvenance(nbt);
    }

    @Override public void simplyswords$readProvenance(NbtCompound nbt) {
        simplyswords$provenance = nbt.contains("simplyswords_provenance")
                ? CombatProvenance.read(nbt.getCompound("simplyswords_provenance")) : null;
        if (nbt.contains("simplyswords_hidden")) hiddenEffect = StatusEffectInstance.fromNbt(nbt.getCompound("simplyswords_hidden"));
    }

    @Override public CombatProvenance simplyswords$getProvenance() { return simplyswords$provenance; }
    @Override public void simplyswords$setProvenance(CombatProvenance value) { simplyswords$provenance = value; }
}
