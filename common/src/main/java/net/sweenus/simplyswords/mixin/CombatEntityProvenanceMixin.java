package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.sweenus.simplyswords.api.combat.CombatProvenance;
import net.sweenus.simplyswords.api.combat.CombatProvenanceApi;
import net.sweenus.simplyswords.api.combat.ProvenanceCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class CombatEntityProvenanceMixin implements ProvenanceCarrier {
    @Unique private CombatProvenance simplyswords$provenance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void simplyswords$capture(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isClient() || self instanceof PlayerEntity) return;
        CombatProvenance origin = CombatProvenanceApi.current();
        simplyswords$provenance = CombatProvenanceApi.deliveredBy(self, origin);
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void simplyswords$save(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if (simplyswords$provenance != null) nbt.put("simplyswords_provenance", simplyswords$provenance.write());
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void simplyswords$load(NbtCompound nbt, CallbackInfo ci) {
        simplyswords$provenance = nbt.contains("simplyswords_provenance")
                ? CombatProvenance.read(nbt.getCompound("simplyswords_provenance")) : null;
    }

    @Override public CombatProvenance simplyswords$getProvenance() { return simplyswords$provenance; }
    @Override public void simplyswords$setProvenance(CombatProvenance value) { simplyswords$provenance = value; }
}
