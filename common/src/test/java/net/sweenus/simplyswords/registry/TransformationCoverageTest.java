package net.sweenus.simplyswords.registry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationCoverageTest {

    private static final Path REGISTRY_SOURCE =
            Path.of("src/main/java/net/sweenus/simplyswords/registry/TransformationRegistry.java");
    private static final Path LOOTABLE_UNIQUES =
            Path.of("src/main/resources/data/simplyswords/tags/items/lootable_uniques.json");

    private static final Pattern TRANSFORMATION =
            Pattern.compile("registerTransformation\\(Blocks\\.([A-Z0-9_]+),[^\"]*\"simplyswords\",\\s*\"([a-z0-9_]+)\"");
    private static final Pattern TAG_ENTRY = Pattern.compile("\"simplyswords:([a-z0-9_]+)\"");

    @Test
    void everyLootableUniqueHasAContainedRemnantTransformation() throws IOException {
        Set<String> outputs = new LinkedHashSet<>();
        Matcher matcher = TRANSFORMATION.matcher(Files.readString(REGISTRY_SOURCE));
        while (matcher.find()) {
            outputs.add(matcher.group(2));
        }

        List<String> missing = new ArrayList<>();
        Matcher entries = TAG_ENTRY.matcher(Files.readString(LOOTABLE_UNIQUES));
        while (entries.find()) {
            String unique = entries.group(1);
            if (!outputs.contains(unique)) {
                missing.add(unique);
            }
        }

        assertTrue(missing.isEmpty(), "Lootable uniques without a Contained Remnant transformation: " + missing);
    }

    @Test
    void noBlockIsMappedToTwoTransformations() throws IOException {
        Set<String> blocks = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        Matcher matcher = TRANSFORMATION.matcher(Files.readString(REGISTRY_SOURCE));
        while (matcher.find()) {
            if (!blocks.add(matcher.group(1))) {
                duplicates.add(matcher.group(1));
            }
        }

        assertTrue(duplicates.isEmpty(), "Blocks registered more than once, silently overwriting an outcome: " + duplicates);
    }
}
