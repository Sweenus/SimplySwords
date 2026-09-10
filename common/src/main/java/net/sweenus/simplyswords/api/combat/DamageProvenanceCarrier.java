package net.sweenus.simplyswords.api.combat;

import net.minecraft.entity.Entity;

public interface DamageProvenanceCarrier {
    void simplyswords$bind(Entity target, CombatProvenance provenance);
    boolean simplyswords$matches(Entity target);
    CombatProvenance simplyswords$take();
}
