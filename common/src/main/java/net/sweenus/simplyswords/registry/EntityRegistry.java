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

    public static final RegistrySupplier<EntityType<SimplySwordsSkeletonMinionEntity>> SKELETON_MINION = ENTITIES.register(
            "skeleton_minion",
            () -> EntityType.Builder.create(SimplySwordsSkeletonMinionEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.99f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(3)
                    .build(Identifier.of(SimplySwords.MOD_ID, "skeleton_minion").toString())
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

    public static final RegistrySupplier<EntityType<DancingBladeVisualEntity>> DANCING_BLADE_VISUAL = ENTITIES.register(
            "dancing_blade_visual",
            () -> EntityType.Builder.<DancingBladeVisualEntity>create(DancingBladeVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(6.0f, 3.0f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "dancing_blade_visual").toString())
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

    public static final RegistrySupplier<EntityType<SoulkeeperLanternVisualEntity>> SOULKEEPER_LANTERN_VISUAL = ENTITIES.register(
            "soulkeeper_lantern_visual",
            () -> EntityType.Builder.<SoulkeeperLanternVisualEntity>create(SoulkeeperLanternVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(3.0f, 2.4f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "soulkeeper_lantern_visual").toString())
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

    public static final RegistrySupplier<EntityType<ImplicitStatusVisualEntity>> IMPLICIT_STATUS_VISUAL = ENTITIES.register(
            "implicit_status_visual",
            () -> EntityType.Builder.<ImplicitStatusVisualEntity>create(ImplicitStatusVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "implicit_status_visual").toString())
    );

    public static final RegistrySupplier<EntityType<ShadowstingAfterimageVisualEntity>> SHADOWSTING_AFTERIMAGE_VISUAL = ENTITIES.register(
            "shadowsting_afterimage_visual",
            () -> EntityType.Builder.<ShadowstingAfterimageVisualEntity>create(ShadowstingAfterimageVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.8f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "shadowsting_afterimage_visual").toString())
    );

    public static final RegistrySupplier<EntityType<IcewhisperCometVisualEntity>> ICEWHISPER_COMET_VISUAL = ENTITIES.register(
            "icewhisper_comet_visual",
            () -> EntityType.Builder.<IcewhisperCometVisualEntity>create(IcewhisperCometVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.55f, 0.55f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "icewhisper_comet_visual").toString())
    );

    public static final RegistrySupplier<EntityType<BrimstoneClaymoreVisualEntity>> BRIMSTONE_CLAYMORE_VISUAL = ENTITIES.register(
            "brimstone_claymore_visual",
            () -> EntityType.Builder.<BrimstoneClaymoreVisualEntity>create(BrimstoneClaymoreVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.75f, 0.75f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "brimstone_claymore_visual").toString())
    );

    public static final RegistrySupplier<EntityType<ChainLightningVisualEntity>> CHAIN_LIGHTNING_VISUAL = ENTITIES.register(
            "chain_lightning_visual",
            () -> EntityType.Builder.<ChainLightningVisualEntity>create(ChainLightningVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35f, 0.35f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "chain_lightning_visual").toString())
    );

    public static final RegistrySupplier<EntityType<VerdantTrailVisualEntity>> VERDANT_TRAIL_VISUAL = ENTITIES.register(
            "verdant_trail_visual",
            () -> EntityType.Builder.<VerdantTrailVisualEntity>create(VerdantTrailVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.45f, 0.7f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "verdant_trail_visual").toString())
    );

    public static final RegistrySupplier<EntityType<FaultlineSpikeVisualEntity>> FAULTLINE_SPIKE_VISUAL = ENTITIES.register(
            "faultline_spike_visual",
            () -> EntityType.Builder.<FaultlineSpikeVisualEntity>create(FaultlineSpikeVisualEntity::new, SpawnGroup.MISC)
                    .dimensions(0.45f, 1.8f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "faultline_spike_visual").toString())
    );

}
