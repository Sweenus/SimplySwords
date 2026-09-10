package net.sweenus.simplyswords.api.combat;

import net.minecraft.nbt.NbtCompound;

public interface ProvenanceCarrier {
    CombatProvenance simplyswords$getProvenance();
    void simplyswords$setProvenance(CombatProvenance provenance);
    default void simplyswords$readProvenance(NbtCompound nbt) {
        simplyswords$setProvenance(nbt.contains("simplyswords_provenance")
                ? CombatProvenance.read(nbt.getCompound("simplyswords_provenance")) : null);
    }
}
