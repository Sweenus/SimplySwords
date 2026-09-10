package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.sweenus.simplyswords.api.combat.CombatProvenance;
import net.sweenus.simplyswords.api.combat.DamageProvenanceCarrier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(DamageSource.class)
public abstract class DamageSourceProvenanceMixin implements DamageProvenanceCarrier {
    @Unique private CombatProvenance simplyswords$provenance;
    @Unique private UUID simplyswords$target;
    @Unique private long simplyswords$tick;
    @Override public void simplyswords$bind(Entity target, CombatProvenance value) {
        simplyswords$provenance = value;
        simplyswords$target = target.getUuid();
        simplyswords$tick = target.getWorld().getTime();
    }
    @Override public boolean simplyswords$matches(Entity target) {
        return target.getUuid().equals(simplyswords$target) && target.getWorld().getTime() == simplyswords$tick;
    }
    @Override public CombatProvenance simplyswords$take() {
        CombatProvenance value = simplyswords$provenance;
        simplyswords$provenance = null;
        simplyswords$target = null;
        return value;
    }
}
