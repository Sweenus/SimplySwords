package net.sweenus.simplyswords.registry;

import com.mojang.serialization.Codec;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.component.*;
import net.sweenus.simplyswords.power.GemPowerComponent;

/**
 * Typed item-stack state used by the 1.20.1 backport.
 *
 * The field names intentionally match the 1.21 data-component registry so
 * shared gameplay code and addon-facing concepts remain equivalent.
 */
public final class ComponentTypeRegistry {
    public static final NoopRegistrar COMPONENT_TYPES = new NoopRegistrar();

    public static final StackComponentKey<GemPowerComponent> GEM_POWER = key("gem_power", GemPowerComponent.CODEC);
    public static final StackComponentKey<Boolean> ADDITIONAL_GEM_SOCKETS = key("additional_gem_sockets", Codec.BOOL);
    public static final StackComponentKey<StoredChargeComponent> STORED_CHARGE = key("stored_charge", StoredChargeComponent.CODEC);
    public static final StackComponentKey<StoredChargeComponent> STORED_BONUS = key("stored_bonus", StoredChargeComponent.CODEC);
    public static final StackComponentKey<ChargedLocationComponent> CHARGED_LOCATION = key("charged_location", ChargedLocationComponent.CODEC);
    public static final StackComponentKey<TargetedLocationComponent> TARGETED_LOCATION = key("targeted_location", TargetedLocationComponent.CODEC);
    public static final StackComponentKey<RelocationComponent> RELOCATION = key("relocation", RelocationComponent.CODEC);
    public static final StackComponentKey<MoltenParticleComponent> MOLTEN_PARTICLE = key("molten_particle", MoltenParticleComponent.CODEC);
    public static final StackComponentKey<MoltenHeatComponent> MOLTEN_HEAT = key("molten_heat", MoltenHeatComponent.CODEC);
    public static final StackComponentKey<ParryComponent> PARRY = key("parry", ParryComponent.CODEC);
    public static final StackComponentKey<WeaponImplicitComponent> WEAPON_IMPLICIT = key("weapon_implicit", WeaponImplicitComponent.CODEC);
    public static final StackComponentKey<AwakeningComponent> AWAKENING = key("awakening", AwakeningComponent.CODEC);
    public static final StackComponentKey<AwakeningRouteComponent> AWAKENING_ROUTE = key("awakening_route", AwakeningRouteComponent.CODEC);
    public static final StackComponentKey<RelicAttunementComponent> RELIC_ATTUNEMENT = key("relic_attunement", RelicAttunementComponent.CODEC);

    private ComponentTypeRegistry() {
    }

    private static <T> StackComponentKey<T> key(String path, Codec<T> codec) {
        return new StackComponentKey<>(new Identifier(SimplySwords.MOD_ID, path), codec);
    }

    public static final class NoopRegistrar {
        public void register() {
            // Data component types are native only to newer Minecraft versions.
        }
    }
}
