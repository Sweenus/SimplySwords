package net.sweenus.simplyswords.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.sweenus.simplyswords.api.combat.CombatProvenance;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.api.combat.FireProvenanceCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class CombatFireProvenanceMixin implements FireProvenanceCarrier {
    @Unique private CombatProvenance simplyswords$fire;

    @WrapMethod(method = "setOnFireForTicks")
    private void simplyswords$ignite(int ticks, Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        int before = self.getFireTicks();
        original.call(ticks);
        if (!self.getWorld().isClient() && self.getFireTicks() > before) {
            var source = CombatProvenanceApi.current();
            simplyswords$fire = source == null ? null : source.deliveredBy(CombatProvenance.DAMAGE_OVER_TIME);
        }
    }

    @Inject(method = "extinguish", at = @At("TAIL"))
    private void simplyswords$extinguish(CallbackInfo ci) { simplyswords$fire = null; }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void simplyswords$save(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if (simplyswords$getFireProvenance() != null) nbt.put("simplyswords_fire_origin", simplyswords$fire.write());
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void simplyswords$load(NbtCompound nbt, CallbackInfo ci) {
        simplyswords$fire = nbt.contains("simplyswords_fire_origin")
                ? CombatProvenance.read(nbt.getCompound("simplyswords_fire_origin")) : null;
    }

    @Override public CombatProvenance simplyswords$getFireProvenance() {
        return ((Entity) (Object) this).getFireTicks() > 0 ? simplyswords$fire : null;
    }
}
