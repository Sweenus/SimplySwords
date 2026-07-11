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

    public static final RegistrySupplier<EntityType<SimplySwordsAxolotlEntity>> SIMPLYAXOLOTLENTITY = ENTITIES.register(
            "simplyaxolotlentity",
            () -> EntityType.Builder.create(SimplySwordsAxolotlEntity::new, SpawnGroup.CREATURE)
                    .build(Identifier.of(SimplySwords.MOD_ID, "simplyaxolotlentity").toString())
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

    public static final RegistrySupplier<EntityType<FrostfallIceSpikeVisualEntity>> FROSTFALL_ICE_SPIKE_VISUAL = ENTITIES.register(
            "frostfall_ice_spike_visual",
            () -> EntityType.Builder.<FrostfallIceSpikeVisualEntity>create(FrostfallIceSpikeVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35f, 1.0f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "frostfall_ice_spike_visual").toString())
    );

    public static final RegistrySupplier<EntityType<SoulrenderMarkVisualEntity>> SOULRENDER_MARK_VISUAL = ENTITIES.register(
            "soulrender_mark_visual",
            () -> EntityType.Builder.<SoulrenderMarkVisualEntity>create(SoulrenderMarkVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "soulrender_mark_visual").toString())
    );

    public static final RegistrySupplier<EntityType<RevivalCandleVisualEntity>> REVIVAL_CANDLE_VISUAL = ENTITIES.register(
            "revival_candle_visual",
            () -> EntityType.Builder.<RevivalCandleVisualEntity>create(RevivalCandleVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "revival_candle_visual").toString())
    );

    public static final RegistrySupplier<EntityType<FlameSeedVisualEntity>> FLAME_SEED_VISUAL = ENTITIES.register(
            "flame_seed_visual",
            () -> EntityType.Builder.<FlameSeedVisualEntity>create(FlameSeedVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "flame_seed_visual").toString())
    );

    public static final RegistrySupplier<EntityType<WhisperwindSlashVisualEntity>> WHISPERWIND_SLASH_VISUAL = ENTITIES.register(
            "whisperwind_slash_visual",
            () -> EntityType.Builder.<WhisperwindSlashVisualEntity>create(WhisperwindSlashVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35f, 0.35f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "whisperwind_slash_visual").toString())
    );

    public static final RegistrySupplier<EntityType<EmberlashSmoulderVisualEntity>> EMBERLASH_SMOULDER_VISUAL = ENTITIES.register(
            "emberlash_smoulder_visual",
            () -> EntityType.Builder.<EmberlashSmoulderVisualEntity>create(EmberlashSmoulderVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "emberlash_smoulder_visual").toString())
    );

    public static final RegistrySupplier<EntityType<ShadowstingAfterimageVisualEntity>> SHADOWSTING_AFTERIMAGE_VISUAL = ENTITIES.register(
            "shadowsting_afterimage_visual",
            () -> EntityType.Builder.<ShadowstingAfterimageVisualEntity>create(ShadowstingAfterimageVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.8f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "shadowsting_afterimage_visual").toString())
    );


}
