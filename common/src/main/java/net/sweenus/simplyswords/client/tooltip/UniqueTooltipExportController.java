package net.sweenus.simplyswords.client.tooltip;

import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.AwakeningFormRegistry;
import net.sweenus.simplyswords.item.component.AwakeningRouteComponent;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplytooltips.api.TooltipExportApi;
import net.sweenus.simplytooltips.api.TooltipExportEntry;
import net.sweenus.simplytooltips.api.TooltipExportListener;
import net.sweenus.simplytooltips.api.TooltipExportOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/** Builds the canonical documentation stacks and sends them to Simply Tooltips' batch exporter. */
public final class UniqueTooltipExportController {
    private static final int MAX_AWAKENING = 8;

    private static final List<ExportSpec> EXPORTS = List.of(
            spec("arcanethyst", ItemsRegistry.ARCANETHYST::get),
            spec("bramblethorn", ItemsRegistry.BRAMBLETHORN::get),
            spec("brimstone_claymore", ItemsRegistry.BRIMSTONE_CLAYMORE::get),
            spec("bloodwake", ItemsRegistry.BLOODWAKE::get),
            spec("caelestis", ItemsRegistry.CAELESTIS::get),
            spec("chompolotl", ItemsRegistry.CHOMPOLOTL::get),
            spec("decaying_relic", ItemsRegistry.DECAYING_RELIC::get, 0),
            spec("dormant_relic", ItemsRegistry.DORMANT_RELIC::get, 0),
            spec("emberblade", ItemsRegistry.EMBERBLADE::get),
            spec("emberlash", ItemsRegistry.EMBERLASH::get),
            spec("enigma", ItemsRegistry.ENIGMA::get),
            spec("flamewind", ItemsRegistry.FLAMEWIND::get),
            spec("frostfall", ItemsRegistry.FROSTFALL::get),
            spec("harbinger", ItemsRegistry.HARBINGER::get, AwakeningFormRegistry.HARBINGER_ROUTE),
            spec("hearthflame", ItemsRegistry.HEARTHFLAME::get),
            spec("hiveheart", ItemsRegistry.HIVEHEART::get),
            spec("icewhisper", ItemsRegistry.ICEWHISPER::get),
            spec("lichblade", ItemsRegistry.AWAKENED_LICHBLADE::get),
            spec("livyatan", ItemsRegistry.LIVYATAN::get),
            spec("magiblade", ItemsRegistry.MAGIBLADE::get),
            spec("magiscythe", ItemsRegistry.MAGISCYTHE::get),
            spec("magispear", ItemsRegistry.MAGISPEAR::get),
            spec("mjolnir", ItemsRegistry.MJOLNIR::get),
            spec("molten_edge", ItemsRegistry.MOLTEN_EDGE::get),
            spec("ribboncleaver", ItemsRegistry.RIBBONCLEAVER::get),
            spec("riftmane", ItemsRegistry.RIFTMANE::get),
            spec("righteous_relic", ItemsRegistry.RIGHTEOUS_RELIC::get, 4, AwakeningFormRegistry.SUN_ROUTE),
            spec("shadowsting", ItemsRegistry.SHADOWSTING::get),
            spec("soulkeeper", ItemsRegistry.SOULKEEPER::get),
            spec("soulpyre", ItemsRegistry.SOULPYRE::get),
            spec("soulrender", ItemsRegistry.SOULRENDER::get),
            spec("soulstalker", ItemsRegistry.SOULSTALKER::get, AwakeningFormRegistry.SOULSTALKER_ROUTE),
            spec("soulstealer", ItemsRegistry.SOULSTEALER::get),
            spec("stars_edge", ItemsRegistry.STARS_EDGE::get),
            spec("stormbringer", ItemsRegistry.STORMBRINGER::get),
            spec("stormscale", ItemsRegistry.STORMSCALE::get),
            spec("ionbound_stormscale", ItemsRegistry.IONBOUND_STORMSCALE::get,
                    AwakeningFormRegistry.IONBOUND_ROUTE),
            spec("storms_edge", ItemsRegistry.STORMS_EDGE::get),
            spec("sunfire", ItemsRegistry.SUNFIRE::get, AwakeningFormRegistry.SUN_ROUTE),
            spec("tainted_relic", ItemsRegistry.TAINTED_RELIC::get, 4, AwakeningFormRegistry.HARBINGER_ROUTE),
            spec("tempest", ItemsRegistry.TEMPEST::get),
            spec("thunderbrand", ItemsRegistry.THUNDERBRAND::get),
            spec("toxic_longsword", ItemsRegistry.TOXIC_LONGSWORD::get),
            spec("twisted_blade", ItemsRegistry.TWISTED_BLADE::get),
            spec("watcher_claymore", ItemsRegistry.WATCHER_CLAYMORE::get),
            spec("the_devourer", ItemsRegistry.THE_DEVOURER::get, AwakeningFormRegistry.DEVOURER_ROUTE),
            spec("watching_warglaive", ItemsRegistry.WATCHING_WARGLAIVE::get),
            spec("waxweaver", ItemsRegistry.WAXWEAVER::get),
            spec("whisperwind", ItemsRegistry.WHISPERWIND::get),
            spec("wickpiercer", ItemsRegistry.WICKPIERCER::get),
            spec("gloampiercer", ItemsRegistry.GLOAMPIERCER::get, AwakeningFormRegistry.GLOAMPIERCER_ROUTE),
            spec("wraithfang", ItemsRegistry.WRAITHFANG::get),
            spec("wraithmaw", ItemsRegistry.WRAITHMAW::get, AwakeningFormRegistry.WRAITHMAW_ROUTE)
    );

    public static int start() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return 0;
        }
        if (!"en_us".equals(client.getLanguageManager().getLanguage())) {
            client.player.sendMessage(Text.literal("Tooltip export requires the English (US) language."), false);
            return 0;
        }
        if (TooltipExportApi.isRunning()) {
            client.player.sendMessage(Text.literal("A tooltip export is already running."), false);
            return 0;
        }

        Path outputDirectory = resolveOutputDirectory(client);
        List<TooltipExportEntry> entries = EXPORTS.stream()
                .map(UniqueTooltipExportController::createEntry)
                .toList();
        boolean started = TooltipExportApi.startBatch(
                entries,
                outputDirectory,
                TooltipExportOptions.documentationDefaults(),
                new ExportMessages()
        );
        return started ? 1 : 0;
    }

    public static int cancel() {
        if (!TooltipExportApi.isRunning()) {
            sendMessage("No tooltip export is running.", false);
            return 0;
        }
        TooltipExportApi.cancel();
        return 1;
    }

    private static TooltipExportEntry createEntry(ExportSpec spec) {
        ItemStack stack = new ItemStack(spec.item().get());
        if (spec.awakeningRoute() != null) {
            stack.set(ComponentTypeRegistry.AWAKENING_ROUTE.get(),
                    new AwakeningRouteComponent(spec.awakeningRoute()));
        }
        AwakeningApi.ensureInitialized(stack);
        AwakeningApi.setLevel(stack, spec.awakeningLevel());
        stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.createEmpty(true, true));
        return new TooltipExportEntry(spec.outputName(), stack);
    }

    private static Path resolveOutputDirectory(MinecraftClient client) {
        String configuredRoot = System.getProperty("simplyswords.docsRoot");
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot).toAbsolutePath().normalize()
                    .resolve(".assets/simplyswords/unique-tooltips");
        }

        Path runDirectory = client.runDirectory.toPath().toAbsolutePath().normalize();
        if (Platform.isDevelopmentEnvironment()) {
            Path projectRoot = runDirectory.getParent() == null ? null : runDirectory.getParent().getParent();
            if (projectRoot != null) {
                Path docs = projectRoot.resolve("docs");
                if (Files.isDirectory(docs)) {
                    return docs.resolve(".assets/simplyswords/unique-tooltips");
                }
            }
        }
        return runDirectory.resolve("screenshots/simplytooltips/simplyswords/unique-tooltips");
    }

    private static ExportSpec spec(String outputName, Supplier<? extends Item> item) {
        return spec(outputName, item, MAX_AWAKENING, null);
    }

    private static ExportSpec spec(String outputName, Supplier<? extends Item> item, int awakeningLevel) {
        return spec(outputName, item, awakeningLevel, null);
    }

    private static ExportSpec spec(String outputName, Supplier<? extends Item> item, Identifier awakeningRoute) {
        return spec(outputName, item, MAX_AWAKENING, awakeningRoute);
    }

    private static ExportSpec spec(String outputName, Supplier<? extends Item> item, int awakeningLevel,
                                   Identifier awakeningRoute) {
        return new ExportSpec(outputName, item, awakeningLevel, awakeningRoute);
    }

    private static void sendMessage(String message, boolean actionBar) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), actionBar);
        }
    }

    private record ExportSpec(String outputName, Supplier<? extends Item> item, int awakeningLevel,
                              Identifier awakeningRoute) {
    }

    private static final class ExportMessages implements TooltipExportListener {
        @Override
        public void onStarted(int total, Path outputDirectory) {
            sendMessage("Exporting " + total + " unique weapon tooltips...", false);
        }

        @Override
        public void onProgress(int completed, int total, String outputName) {
            sendMessage("Tooltip export: " + completed + "/" + total + " (" + outputName + ")", true);
        }

        @Override
        public void onItemFailed(String outputName, Throwable error) {
            sendMessage("Failed to export " + outputName + ": " + error.getMessage(), false);
        }

        @Override
        public void onCompleted(int succeeded, int failed, Path outputDirectory) {
            sendMessage("Tooltip export complete: " + succeeded + " succeeded, " + failed
                    + " failed. Saved to " + outputDirectory, false);
        }

        @Override
        public void onCancelled(int completed, int total, Path outputDirectory) {
            sendMessage("Tooltip export cancelled after " + completed + "/" + total + " items.", false);
        }
    }

    private UniqueTooltipExportController() {
    }
}
