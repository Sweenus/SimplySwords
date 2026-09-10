package net.sweenus.simplyswords.api.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.entity.CaelestisTentacleEntity;
import net.sweenus.simplyswords.entity.DancingBladeVisualEntity;
import net.sweenus.simplyswords.entity.DawnquiverArrowEntity;
import net.sweenus.simplyswords.entity.MagibladeWardenHeadVisualEntity;
import net.sweenus.simplyswords.entity.RunicSlashProjectileEntity;
import net.sweenus.simplyswords.entity.SoulstalkerCleaveEntity;
import net.sweenus.simplyswords.entity.WraithmawCutlassEntity;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;

import java.util.Objects;

public final class CombatProvenanceApi {
    private static final ThreadLocal<CombatProvenance> ACTIVE = new ThreadLocal<>();
    private static volatile Provider provider = new Provider() {
        @Override public CombatProvenance capture(LivingEntity actor, ItemStack stack, int source) { return null; }
        @Override public boolean matches(ItemStack stack, CombatProvenance provenance) { return false; }
    };

    private CombatProvenanceApi() {
    }

    public static void register(Provider value) { provider = Objects.requireNonNull(value); }
    public static CombatProvenance current() { return ACTIVE.get(); }
    public static CombatProvenance entity(Entity entity) {
        return entity instanceof ProvenanceCarrier carrier ? carrier.simplyswords$getProvenance() : null;
    }

    public static CombatProvenance from(ItemStack stack, Entity actor) {
        CombatProvenance inherited = entity(actor);
        if (inherited != null) return inherited;
        CombatProvenance snapshot = stack == null || stack.isEmpty() ? null
                : stack.get(ComponentTypeRegistry.COMBAT_PROVENANCE.get());
        CombatProvenance current = current();
        if (current != null && (snapshot != null && snapshot.identity().equals(current.identity())
                || stack == null || stack.isEmpty() || matches(stack, current)
                || (current.deliveries() & (CombatProvenance.SUMMON | CombatProvenance.DAMAGE_OVER_TIME | CombatProvenance.PROJECTILE)) != 0)) return current;
        return snapshot;
    }

    public static Scope origin(LivingEntity actor, ItemStack stack, int source) {
        CombatProvenance inherited = entity(actor);
        if (inherited != null) inherited = inherited.deliveredBy(source);
        if (inherited == null && actor != null && !actor.getWorld().isClient()) inherited = provider.capture(actor, stack, source);
        if (inherited == null) inherited = from(stack, actor);
        return scope(inherited);
    }

    public static Scope scope(CombatProvenance provenance) { return new Scope(provenance); }

    public static void bind(DamageSource source, ItemStack stack, Entity target) {
        if (target != null && source instanceof DamageProvenanceCarrier carrier) {
            carrier.simplyswords$bind(target, from(stack, source.getAttacker()));
        }
    }

    public static CombatProvenance damage(DamageSource source, Entity target) {
        CombatProvenance value = current();
        if (value == null && source.getSource() != target) value = entity(source.getSource());
        if (value == null && source.getAttacker() != target) value = entity(source.getAttacker());
        return value;
    }

    public static boolean damage(ItemStack stack, Entity actor, LivingEntity target, DamageSource source, float amount) {
        CombatProvenance value = from(stack, actor);
        try (Scope ignored = scope(value)) {
            if (source instanceof DamageProvenanceCarrier carrier) carrier.simplyswords$bind(target, value);
            return target.damage(source, amount);
        }
    }

    public static boolean damage(CombatProvenance provenance, LivingEntity target, DamageSource source, float amount) {
        try (Scope ignored = scope(provenance)) {
            if (source instanceof DamageProvenanceCarrier carrier) carrier.simplyswords$bind(target, provenance);
            return target.damage(source, amount);
        }
    }

    public static void attach(Entity entity, ItemStack stack) {
        if (!(entity instanceof ProvenanceCarrier carrier) || entity.getWorld().isClient()) return;
        CombatProvenance value = from(stack, null);
        if (value == null) return;
        value = deliveredBy(entity, value);
        carrier.simplyswords$setProvenance(value);
    }

    public static CombatProvenance deliveredBy(Entity entity, CombatProvenance value) {
        if (value == null) return null;
        if (entity instanceof ProjectileEntity
                || entity instanceof DawnquiverArrowEntity
                || entity instanceof RunicSlashProjectileEntity
                || entity instanceof SoulstalkerCleaveEntity
                || entity instanceof WraithmawCutlassEntity) value = value.deliveredBy(CombatProvenance.PROJECTILE);
        if (entity instanceof MobEntity
                || entity instanceof DancingBladeVisualEntity
                || entity instanceof MagibladeWardenHeadVisualEntity
                || entity instanceof CaelestisTentacleEntity) value = value.deliveredBy(CombatProvenance.SUMMON);
        return value;
    }

    public static boolean matches(ItemStack stack, CombatProvenance provenance) { return provider.matches(stack, provenance); }

    public interface Provider {
        CombatProvenance capture(LivingEntity actor, ItemStack stack, int source);
        boolean matches(ItemStack stack, CombatProvenance provenance);
    }

    public static final class Scope implements AutoCloseable {
        private final CombatProvenance previous;
        private boolean closed;
        private Scope(CombatProvenance value) {
            previous = ACTIVE.get();
            if (value == null) ACTIVE.remove(); else ACTIVE.set(value);
        }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
        }
    }
}
