package net.sweenus.simplyswords;

import dev.architectury.platform.Platform;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.AxolotlEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.client.renderer.*;
import net.sweenus.simplyswords.client.renderer.model.BattleStandardDarkModel;
import net.sweenus.simplyswords.client.renderer.model.BattleStandardModel;
import net.sweenus.simplyswords.command.SimplySwordsCommands;
import net.sweenus.simplyswords.compat.MythicMetalsCompat;
import net.sweenus.simplyswords.compat.eldritch_end.EldritchEndCompatRegistry;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.registry.*;
import net.sweenus.simplyswords.util.FileCopier;
import net.sweenus.simplyswords.util.ModLootTableModifiers;
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
    public static String minimumSpellbookVersion = "1.21-3.8.0";
    public static String minimumMythicMetalsVersion = "0.24.0+1.21";

    public static void init() {

        //CONFIG

        Config.init();

        SimplySwords.TABS.register();
        ItemsRegistry.ITEM.register();
        SoundRegistry.SOUND.register();
        EffectRegistry.EFFECT.register();
        RecipeTypeRegistry.RECIPES.register();
        EntityRegistry.ENTITIES.register();
        ComponentTypeRegistry.COMPONENT_TYPES.register();
        GemPowerRegistry.register();
        WeaponImplicitRegistry.registerBuiltins();
        ParticlesRegistry.PARTICLES.register();
        TransformationRegistry.register();
        SimplySwordsCommands.register();
        EntityAttributeRegistry.register(EntityRegistry.BATTLESTANDARD, BattleStandardEntity::createBattleStandardAttributes);
        EntityAttributeRegistry.register(EntityRegistry.BATTLESTANDARDDARK, BattleStandardDarkEntity::createBattleStandardDarkAttributes);
        EntityAttributeRegistry.register(EntityRegistry.SIMPLYBEEENTITY, SimplySwordsBeeEntity::createSimplyBeeAttributes);
        EntityAttributeRegistry.register(EntityRegistry.SIMPLYAXOLOTLENTITY, SimplySwordsAxolotlEntity::createSimplyAxolotlAttributes);

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
        public static final EntityModelLayer BATTLESTANDARD_MODEL = new EntityModelLayer(Identifier.of("battlestandard", "cube"), "main");
        public static final EntityModelLayer BATTLESTANDARD_DARK_MODEL = new EntityModelLayer(Identifier.of("battlestandarddark", "cube"), "main");

        @Environment(EnvType.CLIENT)
        public static void initializeClient() {
            // Entity
            EntityRendererRegistry.register(EntityRegistry.BATTLESTANDARD, BattleStandardRenderer::new);
            EntityModelLayerRegistry.register(BATTLESTANDARD_MODEL, BattleStandardModel::getTexturedModelData);
            EntityRendererRegistry.register(EntityRegistry.BATTLESTANDARDDARK, BattleStandardDarkRenderer::new);
            EntityModelLayerRegistry.register(BATTLESTANDARD_DARK_MODEL, BattleStandardDarkModel::getTexturedModelData);
            EntityRendererRegistry.register(EntityRegistry.SIMPLYBEEENTITY, HivemindBeeEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SIMPLYAXOLOTLENTITY, AxolotlEntityRenderer::new);
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
            EntityRendererRegistry.register(EntityRegistry.EMBERLASH_SMOULDER_VISUAL, EmberlashSmoulderVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.IMPLICIT_STATUS_VISUAL, ImplicitStatusVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.SHADOWSTING_AFTERIMAGE_VISUAL, ShadowstingAfterimageVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.ICEWHISPER_COMET_VISUAL, IcewhisperCometVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.BRIMSTONE_CLAYMORE_VISUAL, BrimstoneClaymoreVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.CHAIN_LIGHTNING_VISUAL, ChainLightningVisualEntityRenderer::new);
            EntityRendererRegistry.register(EntityRegistry.VERDANT_TRAIL_VISUAL, VerdantTrailVisualEntityRenderer::new);

            // Simply Tooltips bridge — renders all simplyswords sword items with the ST engine
            net.sweenus.simplytooltips.api.TooltipProviderRegistry.register(
                    new net.sweenus.simplyswords.client.tooltip.SimplySwordsTooltipProvider(), 100);
            }
    }

}
