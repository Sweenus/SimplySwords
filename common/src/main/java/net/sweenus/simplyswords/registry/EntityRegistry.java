package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.entity.*;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<BattleStandardEntity>> BATTLESTANDARD = ENTITIES.register("battlestandard", BattleStandardEntity.TYPE);
    public static final RegistrySupplier<EntityType<BattleStandardDarkEntity>> BATTLESTANDARDDARK = ENTITIES.register("battlestandarddark", BattleStandardDarkEntity.TYPE);
    public static final RegistrySupplier<EntityType<SimplySwordsBeeEntity>> SIMPLYBEEENTITY = ENTITIES.register(
            "simplybeeentity",
            () -> EntityType.Builder.create(SimplySwordsBeeEntity::new, SpawnGroup.CREATURE)
                    .build(Identifier.of(SimplySwords.MOD_ID, "simplybeeentity").toString())
    );

    public static final RegistrySupplier<EntityType<ThrownSwordEntity>> THROWNSWORDENTITY = ENTITIES.register(
            "thrown_sword",
            () -> EntityType.Builder.<ThrownSwordEntity>create(ThrownSwordEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(Identifier.of(SimplySwords.MOD_ID, "thrown_sword").toString())
    );

    public static final RegistrySupplier<EntityType<FrostfallEntity>> FROSTFALLENTITY = ENTITIES.register(
            "frostfall_entity",
            () -> EntityType.Builder.<FrostfallEntity>create(FrostfallEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(Identifier.of(SimplySwords.MOD_ID, "frostfall_entity").toString())
    );

    public static final RegistrySupplier<EntityType<LivyatanEntity>> LIVYATANENTITY = ENTITIES.register(
            "livyatan_entity",
            () -> EntityType.Builder.<LivyatanEntity>create(LivyatanEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(Identifier.of(SimplySwords.MOD_ID, "livyatan_entity").toString())
    );

    public static final RegistrySupplier<EntityType<ThrownSpearEntity>> SPEAR = ENTITIES.register(
            "spear_entity",
            () -> EntityType.Builder.<ThrownSpearEntity>create(ThrownSpearEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(Identifier.of(SimplySwords.MOD_ID, "spear_entity").toString())
    );

    public static final RegistrySupplier<EntityType<ThrownRunicEntity>> THROWNRUNICENTITY = ENTITIES.register(
            "runic_entity",
            () -> EntityType.Builder.<ThrownRunicEntity>create(ThrownRunicEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(Identifier.of(SimplySwords.MOD_ID, "runic_entity").toString())
    );



}
