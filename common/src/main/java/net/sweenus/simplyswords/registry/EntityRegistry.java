package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.entity.ThrownSwordEntity;

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



}
