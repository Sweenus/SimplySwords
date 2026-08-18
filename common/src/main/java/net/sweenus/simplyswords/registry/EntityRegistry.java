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
                    .build(new Identifier(SimplySwords.MOD_ID, "simplybeeentity").toString())
    );

    public static final RegistrySupplier<EntityType<WatcherBatEntity>> WATCHER_BAT = ENTITIES.register(
            "watcher_bat",
            () -> EntityType.Builder.<WatcherBatEntity>create(WatcherBatEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5F, 0.9F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "watcher_bat").toString())
    );

    public static final RegistrySupplier<EntityType<SimplySwordsSkeletonMinionEntity>> SKELETON_MINION = ENTITIES.register(
            "skeleton_minion",
            () -> EntityType.Builder.create(SimplySwordsSkeletonMinionEntity::new, SpawnGroup.MONSTER)
                    .setDimensions(0.6f, 1.99f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(3)
                    .build(new Identifier(SimplySwords.MOD_ID, "skeleton_minion").toString())
    );

    public static final RegistrySupplier<EntityType<SimplySwordsWolfMinionEntity>> WOLF_MINION = ENTITIES.register(
            "wolf_minion",
            () -> EntityType.Builder.create(SimplySwordsWolfMinionEntity::new, SpawnGroup.CREATURE)
                    .setDimensions(0.6f, 0.85f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(3)
                    .build(new Identifier(SimplySwords.MOD_ID, "wolf_minion").toString())
    );

    public static final RegistrySupplier<EntityType<SimplySwordsGoatStampedeEntity>> GOAT_STAMPEDE = ENTITIES.register(
            "goat_stampede",
            () -> EntityType.Builder.create(SimplySwordsGoatStampedeEntity::new, SpawnGroup.CREATURE)
                    .setDimensions(0.9f, 1.3f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(3)
                    .build(new Identifier(SimplySwords.MOD_ID, "goat_stampede").toString())
    );

    public static final RegistrySupplier<EntityType<FallingSnifferEntity>> FALLING_SNIFFER = ENTITIES.register(
            "falling_sniffer",
            () -> EntityType.Builder.<FallingSnifferEntity>create(FallingSnifferEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.9f, 1.75f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "falling_sniffer").toString())
    );

    public static final RegistrySupplier<EntityType<SimplySwordsAxolotlEntity>> SIMPLYAXOLOTLENTITY = ENTITIES.register(
            "simplyaxolotlentity",
            () -> EntityType.Builder.create(SimplySwordsAxolotlEntity::new, SpawnGroup.CREATURE)
                    .build(new Identifier(SimplySwords.MOD_ID, "simplyaxolotlentity").toString())
    );

    public static final RegistrySupplier<EntityType<ThrownSwordEntity>> THROWNSWORDENTITY = ENTITIES.register(
            "thrown_sword",
            () -> EntityType.Builder.<ThrownSwordEntity>create(ThrownSwordEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(new Identifier(SimplySwords.MOD_ID, "thrown_sword").toString())
    );

    public static final RegistrySupplier<EntityType<FrostfallEntity>> FROSTFALLENTITY = ENTITIES.register(
            "frostfall_entity",
            () -> EntityType.Builder.<FrostfallEntity>create(FrostfallEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(new Identifier(SimplySwords.MOD_ID, "frostfall_entity").toString())
    );

    public static final RegistrySupplier<EntityType<LivyatanEntity>> LIVYATANENTITY = ENTITIES.register(
            "livyatan_entity",
            () -> EntityType.Builder.<LivyatanEntity>create(LivyatanEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(new Identifier(SimplySwords.MOD_ID, "livyatan_entity").toString())
    );

    public static final RegistrySupplier<EntityType<ThrownSpearEntity>> SPEAR = ENTITIES.register(
            "spear_entity",
            () -> EntityType.Builder.<ThrownSpearEntity>create(ThrownSpearEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(new Identifier(SimplySwords.MOD_ID, "spear_entity").toString())
    );

    public static final RegistrySupplier<EntityType<ThrownRunicEntity>> THROWNRUNICENTITY = ENTITIES.register(
            "runic_entity",
            () -> EntityType.Builder.<ThrownRunicEntity>create(ThrownRunicEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(10)
                    .build(new Identifier(SimplySwords.MOD_ID, "runic_entity").toString())
    );

    public static final RegistrySupplier<EntityType<DancingBladeVisualEntity>> DANCING_BLADE_VISUAL = ENTITIES.register(
            "dancing_blade_visual",
            () -> EntityType.Builder.<DancingBladeVisualEntity>create(DancingBladeVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(6.0f, 3.0f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "dancing_blade_visual").toString())
    );

    public static final RegistrySupplier<EntityType<FrostfallIceSpikeVisualEntity>> FROSTFALL_ICE_SPIKE_VISUAL = ENTITIES.register(
            "frostfall_ice_spike_visual",
            () -> EntityType.Builder.<FrostfallIceSpikeVisualEntity>create(FrostfallIceSpikeVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 1.0f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "frostfall_ice_spike_visual").toString())
    );

    public static final RegistrySupplier<EntityType<SoulrenderMarkVisualEntity>> SOULRENDER_MARK_VISUAL = ENTITIES.register(
            "soulrender_mark_visual",
            () -> EntityType.Builder.<SoulrenderMarkVisualEntity>create(SoulrenderMarkVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "soulrender_mark_visual").toString())
    );

    public static final RegistrySupplier<EntityType<RevivalCandleVisualEntity>> REVIVAL_CANDLE_VISUAL = ENTITIES.register(
            "revival_candle_visual",
            () -> EntityType.Builder.<RevivalCandleVisualEntity>create(RevivalCandleVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "revival_candle_visual").toString())
    );

    public static final RegistrySupplier<EntityType<SoulkeeperLanternVisualEntity>> SOULKEEPER_LANTERN_VISUAL = ENTITIES.register(
            "soulkeeper_lantern_visual",
            () -> EntityType.Builder.<SoulkeeperLanternVisualEntity>create(SoulkeeperLanternVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(3.0f, 2.4f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "soulkeeper_lantern_visual").toString())
    );

    public static final RegistrySupplier<EntityType<FlameSeedVisualEntity>> FLAME_SEED_VISUAL = ENTITIES.register(
            "flame_seed_visual",
            () -> EntityType.Builder.<FlameSeedVisualEntity>create(FlameSeedVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "flame_seed_visual").toString())
    );

    public static final RegistrySupplier<EntityType<WhisperwindSlashVisualEntity>> WHISPERWIND_SLASH_VISUAL = ENTITIES.register(
            "whisperwind_slash_visual",
            () -> EntityType.Builder.<WhisperwindSlashVisualEntity>create(WhisperwindSlashVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "whisperwind_slash_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DreadwhisperVisualEntity>> DREADWHISPER_VISUAL = ENTITIES.register(
            "dreadwhisper_visual",
            () -> EntityType.Builder.<DreadwhisperVisualEntity>create(DreadwhisperVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(Identifier.of(SimplySwords.MOD_ID, "dreadwhisper_visual").toString())
    );

    public static final RegistrySupplier<EntityType<RunicSlashProjectileEntity>> RUNIC_SLASH_PROJECTILE = ENTITIES.register(
            "runic_slash_projectile",
            () -> EntityType.Builder.<RunicSlashProjectileEntity>create(RunicSlashProjectileEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.25f, 1.25f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "runic_slash_projectile").toString())
    );

    public static final RegistrySupplier<EntityType<TwistedBladeCrescendoVisualEntity>> TWISTED_BLADE_CRESCENDO_VISUAL = ENTITIES.register(
            "twisted_blade_crescendo_visual",
            () -> EntityType.Builder.<TwistedBladeCrescendoVisualEntity>create(TwistedBladeCrescendoVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "twisted_blade_crescendo_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DeathKnellVisualEntity>> DEATH_KNELL_VISUAL = ENTITIES.register(
            "death_knell_visual",
            () -> EntityType.Builder.<DeathKnellVisualEntity>create(DeathKnellVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "death_knell_visual").toString())
    );

    public static final RegistrySupplier<EntityType<EmberlashSmoulderVisualEntity>> EMBERLASH_SMOULDER_VISUAL = ENTITIES.register(
            "emberlash_smoulder_visual",
            () -> EntityType.Builder.<EmberlashSmoulderVisualEntity>create(EmberlashSmoulderVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "emberlash_smoulder_visual").toString())
    );

    public static final RegistrySupplier<EntityType<ImplicitStatusVisualEntity>> IMPLICIT_STATUS_VISUAL = ENTITIES.register(
            "implicit_status_visual",
            () -> EntityType.Builder.<ImplicitStatusVisualEntity>create(ImplicitStatusVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "implicit_status_visual").toString())
    );

    public static final RegistrySupplier<EntityType<ShadowstingAfterimageVisualEntity>> SHADOWSTING_AFTERIMAGE_VISUAL = ENTITIES.register(
            "shadowsting_afterimage_visual",
            () -> EntityType.Builder.<ShadowstingAfterimageVisualEntity>create(ShadowstingAfterimageVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.6f, 1.8f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "shadowsting_afterimage_visual").toString())
    );

    public static final RegistrySupplier<EntityType<StarsEdgeConstellationVisualEntity>> STARS_EDGE_CONSTELLATION_VISUAL = ENTITIES.register(
            "stars_edge_constellation_visual",
            () -> EntityType.Builder.<StarsEdgeConstellationVisualEntity>create(StarsEdgeConstellationVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "stars_edge_constellation_visual").toString())
    );

    public static final RegistrySupplier<EntityType<IcewhisperCometVisualEntity>> ICEWHISPER_COMET_VISUAL = ENTITIES.register(
            "icewhisper_comet_visual",
            () -> EntityType.Builder.<IcewhisperCometVisualEntity>create(IcewhisperCometVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.55f, 0.55f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "icewhisper_comet_visual").toString())
    );

    public static final RegistrySupplier<EntityType<MagispearFirmamentVisualEntity>> MAGISPEAR_FIRMAMENT_VISUAL = ENTITIES.register(
            "magispear_firmament_visual",
            () -> EntityType.Builder.<MagispearFirmamentVisualEntity>create(MagispearFirmamentVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(10.0F, 12.0F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "magispear_firmament_visual").toString())
    );

    public static final RegistrySupplier<EntityType<MagispearFallingSpearVisualEntity>> MAGISPEAR_FALLING_SPEAR_VISUAL = ENTITIES.register(
            "magispear_falling_spear_visual",
            () -> EntityType.Builder.<MagispearFallingSpearVisualEntity>create(MagispearFallingSpearVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.75F, 2.5F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "magispear_falling_spear_visual").toString())
    );

    public static final RegistrySupplier<EntityType<BrimstoneClaymoreVisualEntity>> BRIMSTONE_CLAYMORE_VISUAL = ENTITIES.register(
            "brimstone_claymore_visual",
            () -> EntityType.Builder.<BrimstoneClaymoreVisualEntity>create(BrimstoneClaymoreVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.75f, 0.75f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "brimstone_claymore_visual").toString())
    );

    public static final RegistrySupplier<EntityType<StormscaleRodVisualEntity>> STORMSCALE_ROD_VISUAL = ENTITIES.register(
            "stormscale_rod_visual",
            () -> EntityType.Builder.<StormscaleRodVisualEntity>create(StormscaleRodVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.75F, 2.5F)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "stormscale_rod_visual").toString())
    );

    public static final RegistrySupplier<EntityType<IonboundStormscaleVisualEntity>> IONBOUND_STORMSCALE_VISUAL = ENTITIES.register(
            "ionbound_stormscale_visual",
            () -> EntityType.Builder.<IonboundStormscaleVisualEntity>create(IonboundStormscaleVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "ionbound_stormscale_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DevourerMassVisualEntity>> DEVOURER_MASS_VISUAL = ENTITIES.register(
            "devourer_mass_visual",
            () -> EntityType.Builder.<DevourerMassVisualEntity>create(DevourerMassVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "devourer_mass_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DevourerTendrilVisualEntity>> DEVOURER_TENDRIL_VISUAL = ENTITIES.register(
            "devourer_tendril_visual",
            () -> EntityType.Builder.<DevourerTendrilVisualEntity>create(DevourerTendrilVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "devourer_tendril_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DevourerReprisalVisualEntity>> DEVOURER_REPRISAL_VISUAL = ENTITIES.register(
            "devourer_reprisal_visual",
            () -> EntityType.Builder.<DevourerReprisalVisualEntity>create(DevourerReprisalVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "devourer_reprisal_visual").toString())
    );

    public static final RegistrySupplier<EntityType<WraithmawCutlassEntity>> WRAITHMAW_CUTLASS = ENTITIES.register(
            "wraithmaw_cutlass",
            () -> EntityType.Builder.<WraithmawCutlassEntity>create(WraithmawCutlassEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.55F, 0.55F)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "wraithmaw_cutlass").toString())
    );

    public static final RegistrySupplier<EntityType<GloampiercerSpearEntity>> GLOAMPIERCER_SPEAR = ENTITIES.register(
            "gloampiercer_spear",
            () -> EntityType.Builder.<GloampiercerSpearEntity>create(GloampiercerSpearEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.45F, 0.45F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "gloampiercer_spear").toString())
    );

    public static final RegistrySupplier<EntityType<GloampiercerCloneVisualEntity>> GLOAMPIERCER_CLONE_VISUAL = ENTITIES.register(
            "gloampiercer_clone_visual",
            () -> EntityType.Builder.<GloampiercerCloneVisualEntity>create(GloampiercerCloneVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.6F, 1.8F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(new Identifier(SimplySwords.MOD_ID, "gloampiercer_clone_visual").toString())
    );

    public static final RegistrySupplier<EntityType<SoulstalkerStrideEntity>> SOULSTALKER_STRIDE = ENTITIES.register(
            "soulstalker_stride",
            () -> EntityType.Builder.<SoulstalkerStrideEntity>create(SoulstalkerStrideEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.1F, 3.9F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(new Identifier(SimplySwords.MOD_ID, "soulstalker_stride").toString())
    );

    public static final RegistrySupplier<EntityType<SoulstalkerCleaveEntity>> SOULSTALKER_CLEAVE = ENTITIES.register(
            "soulstalker_cleave",
            () -> EntityType.Builder.<SoulstalkerCleaveEntity>create(SoulstalkerCleaveEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.2F, 0.2F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(new Identifier(SimplySwords.MOD_ID, "soulstalker_cleave").toString())
    );

    public static final RegistrySupplier<EntityType<SoulstalkerTentacleVisualEntity>> SOULSTALKER_TENTACLE_VISUAL = ENTITIES.register(
            "soulstalker_tentacle_visual",
            () -> EntityType.Builder.<SoulstalkerTentacleVisualEntity>create(SoulstalkerTentacleVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.2F, 0.2F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(new Identifier(SimplySwords.MOD_ID, "soulstalker_tentacle_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DawnquiverBowVisualEntity>> DAWNQUIVER_BOW_VISUAL = ENTITIES.register(
            "dawnquiver_bow_visual",
            () -> EntityType.Builder.<DawnquiverBowVisualEntity>create(DawnquiverBowVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.2F, 0.2F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(Identifier.of(SimplySwords.MOD_ID, "dawnquiver_bow_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DawnquiverArrowEntity>> DAWNQUIVER_ARROW = ENTITIES.register(
            "dawnquiver_arrow",
            () -> EntityType.Builder.<DawnquiverArrowEntity>create(DawnquiverArrowEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.4F, 0.4F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(Identifier.of(SimplySwords.MOD_ID, "dawnquiver_arrow").toString())
    );

    public static final RegistrySupplier<EntityType<DawnquiverImpactVisualEntity>> DAWNQUIVER_IMPACT_VISUAL = ENTITIES.register(
            "dawnquiver_impact_visual",
            () -> EntityType.Builder.<DawnquiverImpactVisualEntity>create(DawnquiverImpactVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.2F, 0.2F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(Identifier.of(SimplySwords.MOD_ID, "dawnquiver_impact_visual").toString())
    );

    public static final RegistrySupplier<EntityType<RiftmaneChargerEntity>> RIFTMANE_CHARGER = ENTITIES.register(
            "riftmane_charger",
            () -> EntityType.Builder.<RiftmaneChargerEntity>create(RiftmaneChargerEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.3964844F, 1.6F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(Identifier.of(SimplySwords.MOD_ID, "riftmane_charger").toString())
    );

    public static final RegistrySupplier<EntityType<RiftmaneRiftVisualEntity>> RIFTMANE_RIFT_VISUAL = ENTITIES.register(
            "riftmane_rift_visual",
            () -> EntityType.Builder.<RiftmaneRiftVisualEntity>create(RiftmaneRiftVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.2F, 0.2F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build(Identifier.of(SimplySwords.MOD_ID, "riftmane_rift_visual").toString())
    );

    public static final RegistrySupplier<EntityType<ChainLightningVisualEntity>> CHAIN_LIGHTNING_VISUAL = ENTITIES.register(
            "chain_lightning_visual",
            () -> EntityType.Builder.<ChainLightningVisualEntity>create(ChainLightningVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35f, 0.35f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "chain_lightning_visual").toString())
    );

    public static final RegistrySupplier<EntityType<LightningPhenomenonVisualEntity>> LIGHTNING_PHENOMENON_VISUAL = ENTITIES.register(
            "lightning_phenomenon_visual",
            () -> EntityType.Builder.<LightningPhenomenonVisualEntity>create(LightningPhenomenonVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "lightning_phenomenon_visual").toString())
    );

    public static final RegistrySupplier<EntityType<AtmosphericVisualEntity>> ATMOSPHERIC_VISUAL = ENTITIES.register(
            "atmospheric_visual",
            () -> EntityType.Builder.<AtmosphericVisualEntity>create(AtmosphericVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(160)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "atmospheric_visual").toString())
    );

    public static final RegistrySupplier<EntityType<FurnaceChainVisualEntity>> FURNACE_CHAIN_VISUAL = ENTITIES.register(
            "furnace_chain_visual",
            () -> EntityType.Builder.<FurnaceChainVisualEntity>create(FurnaceChainVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "furnace_chain_visual").toString())
    );

    public static final RegistrySupplier<EntityType<BrambleRootVisualEntity>> BRAMBLE_ROOT_VISUAL = ENTITIES.register(
            "bramble_root_visual",
            () -> EntityType.Builder.<BrambleRootVisualEntity>create(BrambleRootVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "bramble_root_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DragonWingBuffetVisualEntity>> DRAGON_WING_BUFFET_VISUAL = ENTITIES.register(
            "dragon_wing_buffet_visual",
            () -> EntityType.Builder.<DragonWingBuffetVisualEntity>create(DragonWingBuffetVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(4.0f, 4.0f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "dragon_wing_buffet_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DragonMawHeadVisualEntity>> DRAGON_MAW_HEAD_VISUAL = ENTITIES.register(
            "dragon_maw_head_visual",
            () -> EntityType.Builder.<DragonMawHeadVisualEntity>create(DragonMawHeadVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(3.5f, 3.5f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "dragon_maw_head_visual").toString())
    );

    public static final RegistrySupplier<EntityType<MagibladeWardenHeadVisualEntity>> MAGIBLADE_WARDEN_HEAD_VISUAL = ENTITIES.register(
            "magiblade_warden_head_visual",
            () -> EntityType.Builder.<MagibladeWardenHeadVisualEntity>create(MagibladeWardenHeadVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.5F, 1.5F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "magiblade_warden_head_visual").toString())
    );

    public static final RegistrySupplier<EntityType<DragonMawBreathCloudEntity>> DRAGON_MAW_BREATH_CLOUD = ENTITIES.register(
            "dragon_maw_breath_cloud",
            () -> EntityType.Builder.<DragonMawBreathCloudEntity>create(DragonMawBreathCloudEntity::new, SpawnGroup.MISC)
                    .setDimensions(4.5f, 1.25f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "dragon_maw_breath_cloud").toString())
    );

    public static final RegistrySupplier<EntityType<VerdantTrailVisualEntity>> VERDANT_TRAIL_VISUAL = ENTITIES.register(
            "verdant_trail_visual",
            () -> EntityType.Builder.<VerdantTrailVisualEntity>create(VerdantTrailVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.45f, 0.7f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "verdant_trail_visual").toString())
    );

    public static final RegistrySupplier<EntityType<FaultlineSpikeVisualEntity>> FAULTLINE_SPIKE_VISUAL = ENTITIES.register(
            "faultline_spike_visual",
            () -> EntityType.Builder.<FaultlineSpikeVisualEntity>create(FaultlineSpikeVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.45f, 1.8f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "faultline_spike_visual").toString())
    );

    public static final RegistrySupplier<EntityType<LivyatanWaveVisualEntity>> LIVYATAN_WAVE_VISUAL = ENTITIES.register(
            "livyatan_wave_visual",
            () -> EntityType.Builder.<LivyatanWaveVisualEntity>create(LivyatanWaveVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.0f, 1.0f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "livyatan_wave_visual").toString())
    );

    public static final RegistrySupplier<EntityType<BloodwakeBladeVisualEntity>> BLOODWAKE_BLADE_VISUAL = ENTITIES.register(
            "bloodwake_blade_visual",
            () -> EntityType.Builder.<BloodwakeBladeVisualEntity>create(BloodwakeBladeVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.75F, 2.5F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "bloodwake_blade_visual").toString())
    );

    public static final RegistrySupplier<EntityType<BloodPlagueSpreadVisualEntity>> BLOOD_PLAGUE_SPREAD_VISUAL = ENTITIES.register(
            "blood_plague_spread_visual",
            () -> EntityType.Builder.<BloodPlagueSpreadVisualEntity>create(BloodPlagueSpreadVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "blood_plague_spread_visual").toString())
    );

    public static final RegistrySupplier<EntityType<BloodStainVisualEntity>> BLOOD_STAIN_VISUAL = ENTITIES.register(
            "blood_stain_visual",
            () -> EntityType.Builder.<BloodStainVisualEntity>create(BloodStainVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(128)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "blood_stain_visual").toString())
    );

    public static final RegistrySupplier<EntityType<MoltenRuptureVisualEntity>> MOLTEN_RUPTURE_VISUAL = ENTITIES.register(
            "molten_rupture_visual",
            () -> EntityType.Builder.<MoltenRuptureVisualEntity>create(MoltenRuptureVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.75f, 1.25f)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "molten_rupture_visual").toString())
    );

    public static final RegistrySupplier<EntityType<CaelestisBreachVisualEntity>> CAELESTIS_BREACH_VISUAL = ENTITIES.register(
            "caelestis_breach_visual",
            () -> EntityType.Builder.<CaelestisBreachVisualEntity>create(CaelestisBreachVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(31.0F, 4.0F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "caelestis_breach_visual").toString())
    );

    public static final RegistrySupplier<EntityType<SoulPyreVisualEntity>> SOUL_PYRE_VISUAL = ENTITIES.register(
            "soul_pyre_visual",
            () -> EntityType.Builder.<SoulPyreVisualEntity>create(SoulPyreVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(27.0F, 4.0F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "soul_pyre_visual").toString())
    );

    public static final RegistrySupplier<EntityType<SoulPyreWispEntity>> SOUL_PYRE_WISP = ENTITIES.register(
            "soul_pyre_wisp",
            () -> EntityType.Builder.<SoulPyreWispEntity>create(SoulPyreWispEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.35F, 0.35F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "soul_pyre_wisp").toString())
    );

    public static final RegistrySupplier<EntityType<CaelestisRiftlingEntity>> CAELESTIS_RIFTLING = ENTITIES.register(
            "caelestis_riftling",
            () -> EntityType.Builder.<CaelestisRiftlingEntity>create(CaelestisRiftlingEntity::new, SpawnGroup.MONSTER)
                    .setDimensions(0.9F, 0.6F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(2)
                    .build(new Identifier(SimplySwords.MOD_ID, "caelestis_riftling").toString())
    );

    public static final RegistrySupplier<EntityType<CaelestisHollowEntity>> CAELESTIS_HOLLOW = ENTITIES.register(
            "caelestis_hollow",
            () -> EntityType.Builder.<CaelestisHollowEntity>create(CaelestisHollowEntity::new, SpawnGroup.MONSTER)
                    .setDimensions(0.6F, 1.95F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(2)
                    .build(new Identifier(SimplySwords.MOD_ID, "caelestis_hollow").toString())
    );

    public static final RegistrySupplier<EntityType<CaelestisDreadglareEntity>> CAELESTIS_DREADGLARE = ENTITIES.register(
            "caelestis_dreadglare",
            () -> EntityType.Builder.<CaelestisDreadglareEntity>create(CaelestisDreadglareEntity::new, SpawnGroup.MONSTER)
                    .setDimensions(0.75F, 0.75F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "caelestis_dreadglare").toString())
    );

    public static final RegistrySupplier<EntityType<CaelestisTentacleEntity>> CAELESTIS_TENTACLE = ENTITIES.register(
            "caelestis_tentacle",
            () -> EntityType.Builder.<CaelestisTentacleEntity>create(CaelestisTentacleEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.6F, 4.1F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "caelestis_tentacle").toString())
    );

    public static final RegistrySupplier<EntityType<WaxweaverWaxVisualEntity>> WAXWEAVER_WAX_VISUAL = ENTITIES.register(
            "waxweaver_wax_visual",
            () -> EntityType.Builder.<WaxweaverWaxVisualEntity>create(WaxweaverWaxVisualEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .maxTrackingRange(96)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "waxweaver_wax_visual").toString())
    );

    public static final RegistrySupplier<EntityType<SimplySwordsCreeperHeadEntity>> CREEPER_HEAD = ENTITIES.register(
            "creeper_head",
            () -> EntityType.Builder.<SimplySwordsCreeperHeadEntity>create(SimplySwordsCreeperHeadEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(new Identifier(SimplySwords.MOD_ID, "creeper_head").toString())
    );

}
