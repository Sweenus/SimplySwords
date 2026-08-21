package net.sweenus.simplyswords;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.AxolotlEntityRenderer;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.GoatEntityRenderer;
import net.minecraft.client.render.entity.SkeletonEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.AwakeningFormRegistry;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.render.ObserverStatusVisualShape;
import net.sweenus.simplyswords.api.render.ObserverStatusVisualStyle;
import net.sweenus.simplyswords.client.api.ObserverStatusEffectClientApi;
import net.sweenus.simplyswords.client.renderer.*;
import net.sweenus.simplyswords.client.renderer.model.BattleStandardDarkModel;
import net.sweenus.simplyswords.client.renderer.model.BattleStandardModel;
import net.sweenus.simplyswords.client.renderer.model.CaelestisDreadglareModel;
import net.sweenus.simplyswords.client.renderer.model.CaelestisRiftlingModel;
import net.sweenus.simplyswords.client.renderer.model.CaelestisTentacleModel;
import net.sweenus.simplyswords.client.AbilityKeybindHandler;
import net.sweenus.simplyswords.client.CaelestisBreachAmbience;
import net.sweenus.simplyswords.client.LocalStormVisualManager;
import net.sweenus.simplyswords.client.hud.WeaponHudRenderer;
import net.sweenus.simplyswords.client.IonboundBeamClientState;
import net.sweenus.simplyswords.command.SimplySwordsCommands;
import net.sweenus.simplyswords.compat.MythicMetalsCompat;
import net.sweenus.simplyswords.compat.bettercombat.BetterCombatCompat;
import net.sweenus.simplyswords.compat.eldritch_end.EldritchEndCompatRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.entity.CaelestisDreadglareEntity;
import net.sweenus.simplyswords.entity.SoulstalkerStrideEntity;
import net.sweenus.simplyswords.entity.CaelestisHollowEntity;
import net.sweenus.simplyswords.entity.CaelestisRiftlingEntity;
import net.sweenus.simplyswords.entity.FallingSnifferEntity;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.entity.SimplySwordsSkeletonMinionEntity;
import net.sweenus.simplyswords.entity.SimplySwordsWolfMinionEntity;
import net.sweenus.simplyswords.entity.SimplySwordsGoatStampedeEntity;
import net.sweenus.simplyswords.entity.SimplySwordsCreeperHeadEntity;
import net.sweenus.simplyswords.entity.RiftmaneChargerEntity;
import net.sweenus.simplyswords.entity.WatcherBatEntity;
import net.sweenus.simplyswords.registry.*;
import net.sweenus.simplyswords.network.SimplySwordsNetwork;
import net.sweenus.simplyswords.util.FileCopier;
import net.sweenus.simplyswords.util.ModLootTableModifiers;
import net.sweenus.simplyswords.world.ObserverStatusEffectSyncManager;
import net.sweenus.simplyswords.world.GloamMechanicsManager;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SimplySwords {
    public static final String MOD_ID = "simplyswords";

    public static final DeferredRegister<ItemGroup> TABS =
            DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.ITEM_GROUP);

    public static final RegistrySupplier<ItemGroup> SIMPLYSWORDS = TABS.register(
            "simplyswords", // Tab ID
            () -> CreativeTabRegistry.create(
                    Text.translatable("itemGroup.simplyswords.simplyswords"), // Tab Name
                    () -> new ItemStack(ItemsRegistry.RUNIC_TABLET.get()) // Icon
            )
    );

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static String minimumEldritchEndVersion = "0.2.40";
    public static String minimumSpellPowerVersion = "0.10.0+1.20.1";
    public static String minimumSpellbookVersion = "1.20.1-3.16.2";
    public static String minimumMythicMetalsVersion = "0.24.0+1.21";

    public static void init() {

        //CONFIG

        Config.init();

        SimplySwords.TABS.register();
        BlocksRegistry.BLOCKS.register();
        ItemsRegistry.ITEM.register();
        SoundRegistry.SOUND.register();
        EffectRegistry.EFFECT.register();
        RecipeTypeRegistry.RECIPES.register();
        EntityRegistry.ENTITIES.register();
        ComponentTypeRegistry.COMPONENT_TYPES.register();
        ScreenHandlerRegistry.SCREEN_HANDLERS.register();
        GemPowerRegistry.register();
        WeaponImplicitRegistry.registerBuiltins();
        ParticlesRegistry.PARTICLES.register();
        TransformationRegistry.register();
        LifecycleEvent.SETUP.register(AwakeningFormRegistry::registerBuiltins);
        // At SETUP rather than here, so Better Combat's classes aren't force-loaded during
        // mod construction and Platform can actually answer isModLoaded.
        LifecycleEvent.SETUP.register(() -> {
            if (Platform.isModLoaded("bettercombat")) {
                BetterCombatCompat.verifyAttackHookTarget();
            }
        });
        SimplySwordsNetwork.init();
        SimplySwordsAPI.registerObserverSyncedStatusEffect(EffectRegistry.SHADOW_DANCE_ID);
        SimplySwordsAPI.registerObserverStatusVisual(EffectRegistry.BLOOD_PLAGUE_ID,
                new ObserverStatusVisualStyle(ObserverStatusVisualShape.GROUND_RING,
                        0x39030B, 0xF02B3D, 1, 1.0F));
        SimplySwordsAPI.registerIncapacitatingStatusEffect(EffectRegistry.ION_PARALYSIS_ID);
        SimplySwordsAPI.registerObserverStatusVisual(EffectRegistry.ION_PARALYSIS_ID,
                new ObserverStatusVisualStyle(ObserverStatusVisualShape.STATIC_STREAKS,
                        0x2F8CFF, 0xF7FDFF, 5, 1.0F));
        SimplySwordsAPI.registerObserverStatusVisual(EffectRegistry.CORRUPTED_WOUND_ID,
                new ObserverStatusVisualStyle(ObserverStatusVisualShape.CORRUPTED_WOUNDS,
                        0x4B0B71, 0x8DFF73, 5, 1.0F));
        SimplySwordsAPI.registerObserverStatusVisual(EffectRegistry.GLOAM_EXPOSURE_ID,
                new ObserverStatusVisualStyle(ObserverStatusVisualShape.GLOAM_EXPOSURE,
                        0x250A35, 0xD49A2C, 3, 1.0F));
        SimplySwordsAPI.registerObserverStatusVisual(EffectRegistry.GLOAM_GRASP_ID,
                new ObserverStatusVisualStyle(ObserverStatusVisualShape.GLOAM_GRASP,
                        0x250A35, 0x250A35, 4, 1.0F));
        GloamMechanicsManager.init();
        ObserverStatusEffectSyncManager.init();
        ShadowstingShadowDanceManager.init();
        SimplySwordsCommands.register();
        EntityAttributeRegistry.register(EntityRegistry.BATTLESTANDARD, BattleStandardEntity::createBattleStandardAttributes);
        EntityAttributeRegistry.register(EntityRegistry.BATTLESTANDARDDARK, BattleStandardDarkEntity::createBattleStandardDarkAttributes);
        EntityAttributeRegistry.register(EntityRegistry.SIMPLYBEEENTITY, SimplySwordsBeeEntity::createSimplyBeeAttributes);
        EntityAttributeRegistry.register(EntityRegistry.WATCHER_BAT, WatcherBatEntity::createWatcherBatAttributes);
        EntityAttributeRegistry.register(EntityRegistry.SKELETON_MINION, SimplySwordsSkeletonMinionEntity::createMinionAttributes);
        EntityAttributeRegistry.register(EntityRegistry.WOLF_MINION, SimplySwordsWolfMinionEntity::createMinionAttributes);
        EntityAttributeRegistry.register(EntityRegistry.GOAT_STAMPEDE, SimplySwordsGoatStampedeEntity::createStampedeAttributes);
        EntityAttributeRegistry.register(EntityRegistry.FALLING_SNIFFER, FallingSnifferEntity::createFallingSnifferAttributes);
        EntityAttributeRegistry.register(EntityRegistry.CREEPER_HEAD, SimplySwordsCreeperHeadEntity::createHeadAttributes);
        EntityAttributeRegistry.register(EntityRegistry.SIMPLYAXOLOTLENTITY, SimplySwordsAxolotlEntity::createSimplyAxolotlAttributes);
        EntityAttributeRegistry.register(EntityRegistry.CAELESTIS_RIFTLING, CaelestisRiftlingEntity::createBreachAttributes);
        EntityAttributeRegistry.register(EntityRegistry.CAELESTIS_HOLLOW, CaelestisHollowEntity::createBreachAttributes);
        EntityAttributeRegistry.register(EntityRegistry.CAELESTIS_DREADGLARE, CaelestisDreadglareEntity::createBreachAttributes);
        EntityAttributeRegistry.register(EntityRegistry.SOULSTALKER_STRIDE, SoulstalkerStrideEntity::createStrideAttributes);
        EntityAttributeRegistry.register(EntityRegistry.RIFTMANE_CHARGER, RiftmaneChargerEntity::createChargerAttributes);

        ModLootTableModifiers.init();
        if (passVersionCheck("eldritch_end", minimumEldritchEndVersion)) {
            //EldritchEndCompat.registerModItems(); 1.21
            EldritchEndCompatRegistry.EFFECT.register();
        }
        if (passVersionCheck("mythicmetals", minimumMythicMetalsVersion)) {
            LOGGER.info("Registering Mythic Metals items");
            MythicMetalsCompat.init();
        }
        try {
            FileCopier.copyFileToConfigDirectory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(SimplySwordsExpectPlatform.getConfigDirectory().toAbsolutePath().normalize().toString());
        EnvExecutor.runInEnv(Env.CLIENT, () -> SimplySwords.Client::initializeClient);

    }

    public static boolean passVersionCheck(String modId, String requiredVersion) {
        if (Platform.isModLoaded(modId)) {
            if (Platform.getMod(modId).getVersion().compareTo(requiredVersion) >= 0) {
                return true;
            }
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    public static class Client {
        public static final EntityModelLayer BATTLESTANDARD_MODEL =
                new EntityModelLayer(new Identifier("battlestandard", "cube"), "main");
        public static final EntityModelLayer BATTLESTANDARD_DARK_MODEL =
                new EntityModelLayer(new Identifier("battlestandarddark", "cube"), "main");
        public static final EntityModelLayer CAELESTIS_RIFTLING_MODEL =
                new EntityModelLayer(new Identifier(MOD_ID, "caelestis_riftling"), "main");
        public static final EntityModelLayer CAELESTIS_DREADGLARE_MODEL =
                new EntityModelLayer(new Identifier(MOD_ID, "caelestis_dreadglare"), "main");
        public static final EntityModelLayer CAELESTIS_TENTACLE_MODEL =
                new EntityModelLayer(new Identifier(MOD_ID, "caelestis_tentacle"), "main");

        @Environment(EnvType.CLIENT)
        public static void initializeClient() {
            AbilityKeybindHandler.init();
            CaelestisBreachAmbience.init();
            ObserverStatusEffectClientApi.init();
            LocalStormVisualManager.init();
            // Not a mixin on InGameHud#render: Forge 1.20.1 substitutes ForgeGui, which overrides
            // render without calling super, so such a mixin never runs on Forge.
            ClientGuiEvent.RENDER_HUD.register(WeaponHudRenderer::render);
            IonboundBeamClientState.init();
            // Entity
            EntityRendererRegistry.register(EntityRegistry.BATTLESTANDARD, BattleStandardRenderer::new);
            EntityModelLayerRegistry.register(BATTLESTANDARD_MODEL, BattleStandardModel::getTexturedModelData);
            EntityRendererRegistry.register(EntityRegistry.BATTLESTANDARDDARK, BattleStandardDarkRenderer::new);
            EntityModelLayerRegistry.register(BATTLESTANDARD_DARK_MODEL, BattleStandardDarkModel::getTexturedModelData);
            EntityModelLayerRegistry.register(CAELESTIS_RIFTLING_MODEL, CaelestisRiftlingModel::getTexturedModelData);
            EntityModelLayerRegistry.register(CAELESTIS_DREADGLARE_MODEL, CaelestisDreadglareModel::getTexturedModelData);
            EntityModelLayerRegistry.register(CAELESTIS_TENTACLE_MODEL, CaelestisTentacleModel::getTexturedModelData);
            EntityRendererRegistry.register(EntityRegistry.SIMPLYBEEENTITY, HivemindBeeEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.WATCHER_BAT, WatcherBatEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SKELETON_MINION, SkeletonEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.WOLF_MINION, SimplySwordsWolfMinionRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SIMPLYAXOLOTLENTITY, AxolotlEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.GOAT_STAMPEDE, GoatEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.FALLING_SNIFFER, FallingSnifferEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.CREEPER_HEAD, CreeperHeadVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.THROWNSWORDENTITY, ThrownSwordEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.FROSTFALLENTITY, ThrownSwordEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.LIVYATANENTITY, ThrownSwordEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SPEAR, ThrownSpearEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.THROWNRUNICENTITY, ThrownSwordEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DANCING_BLADE_VISUAL, DancingBladeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.FROSTFALL_ICE_SPIKE_VISUAL, FrostfallIceSpikeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SOULRENDER_MARK_VISUAL, SoulrenderMarkVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.REVIVAL_CANDLE_VISUAL, RevivalCandleVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SOULKEEPER_LANTERN_VISUAL, SoulkeeperLanternVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.FLAME_SEED_VISUAL, FlameSeedVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.WHISPERWIND_SLASH_VISUAL, WhisperwindSlashVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DREADWHISPER_VISUAL, DreadwhisperVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.RUNIC_SLASH_PROJECTILE, RunicSlashProjectileEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.TWISTED_BLADE_CRESCENDO_VISUAL, TwistedBladeCrescendoVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DEATH_KNELL_VISUAL, DeathKnellVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.EMBERLASH_SMOULDER_VISUAL, EmberlashSmoulderVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.IMPLICIT_STATUS_VISUAL, ImplicitStatusVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SHADOWSTING_AFTERIMAGE_VISUAL, ShadowstingAfterimageVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.STARS_EDGE_CONSTELLATION_VISUAL, StarsEdgeConstellationVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ICEWHISPER_COMET_VISUAL, IcewhisperCometVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.MAGISPEAR_FIRMAMENT_VISUAL, MagispearFirmamentVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.MAGISPEAR_FALLING_SPEAR_VISUAL, MagispearFallingSpearVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BRIMSTONE_CLAYMORE_VISUAL, BrimstoneClaymoreVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.STORMSCALE_ROD_VISUAL, StormscaleRodVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.IONBOUND_STORMSCALE_VISUAL, IonboundStormscaleVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DEVOURER_MASS_VISUAL, DevourerMassVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DEVOURER_TENDRIL_VISUAL, DevourerTendrilVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DEVOURER_REPRISAL_VISUAL, DevourerReprisalVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.WRAITHMAW_CUTLASS, WraithmawCutlassEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.GLOAMPIERCER_SPEAR, GloampiercerSpearEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.GLOAMPIERCER_CLONE_VISUAL, GloampiercerCloneVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SOULSTALKER_STRIDE, SoulstalkerStrideEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SOULSTALKER_CLEAVE, SoulstalkerCleaveEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SOULSTALKER_TENTACLE_VISUAL, SoulstalkerTentacleVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.RIFTMANE_CHARGER, RiftmaneChargerEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.RIFTMANE_RIFT_VISUAL, RiftmaneRiftVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DAWNQUIVER_BOW_VISUAL, DawnquiverBowVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DAWNQUIVER_ARROW, DawnquiverArrowEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DAWNQUIVER_IMPACT_VISUAL, DawnquiverImpactVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.CHAIN_LIGHTNING_VISUAL, ChainLightningVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.LIGHTNING_PHENOMENON_VISUAL, LightningPhenomenonVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ATMOSPHERIC_VISUAL, AtmosphericVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.FURNACE_CHAIN_VISUAL, FurnaceChainVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BRAMBLE_ROOT_VISUAL, BrambleRootVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DRAGON_WING_BUFFET_VISUAL, DragonWingBuffetVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DRAGON_MAW_HEAD_VISUAL, DragonMawHeadVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.MAGIBLADE_WARDEN_HEAD_VISUAL, MagibladeWardenHeadVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.DRAGON_MAW_BREATH_CLOUD, EmptyEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.VERDANT_TRAIL_VISUAL, VerdantTrailVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.FAULTLINE_SPIKE_VISUAL, FaultlineSpikeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.LIVYATAN_WAVE_VISUAL, LivyatanWaveVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BLOODWAKE_BLADE_VISUAL, BloodwakeBladeVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BLOOD_PLAGUE_SPREAD_VISUAL, BloodPlagueSpreadVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BLOOD_STAIN_VISUAL, BloodStainVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.MOLTEN_RUPTURE_VISUAL, MoltenRuptureVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.CAELESTIS_BREACH_VISUAL, CaelestisBreachVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SOUL_PYRE_VISUAL, SoulPyreVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SOUL_PYRE_WISP, SoulPyreWispEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.CAELESTIS_RIFTLING, CaelestisRiftlingRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.CAELESTIS_HOLLOW, CaelestisHollowRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.CAELESTIS_DREADGLARE, CaelestisDreadglareRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.CAELESTIS_TENTACLE, CaelestisTentacleRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.WAXWEAVER_WAX_VISUAL, WaxweaverWaxVisualEntityRenderer::new);

            // Simply Tooltips bridge — renders all simplyswords sword items with the ST engine
            net.sweenus.simplytooltips.api.TooltipProviderRegistry.register(
                    new net.sweenus.simplyswords.client.tooltip.SimplySwordsTooltipProvider(), 100);
        }

        //
        // Registers client features that dereference deferred registry entries. Loader entrypoints
        // must call this only after their item registries have been populated.
        //
        @Environment(EnvType.CLIENT)
        public static void initializeRegistryDependentClient() {
            ItemPropertiesRegistry.register(ItemsRegistry.SLUMBERING_LICHBLADE.get(),
                    new Identifier(MOD_ID, "awakening"),
                    (stack, world, entity, seed) -> AwakeningApi.getFormModelValue(stack));
            ItemPropertiesRegistry.register(ItemsRegistry.DORMANT_RELIC.get(),
                    new Identifier(MOD_ID, "relic_form"),
                    (stack, world, entity, seed) -> AwakeningApi.getFormModelValue(stack));
        }
    }

}
