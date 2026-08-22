package net.sweenus.simplyswords.config;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigResetMigrationTest {

    private static final List<String> CONFIG_CLASSES = List.of(
            "GeneralConfig",
            "GuiConfig",
            "GemPowersConfig",
            "StatusEffectsConfig",
            "WeaponAttributesConfig",
            "UniqueEffectsConfig",
            "CompatibilityConfig",
            "LootConfig"
    );

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void olderSchemasDiscardRawAndNestedCustomValues(int schema) {
        TestConfig config = new TestConfig();
        TestSection customizedSection = config.section;
        config.rawValue = 99;
        config.section.value = 88;

        config.update(schema);

        assertEquals(5, config.rawValue);
        assertEquals(7, config.section.value);
        assertNotSame(customizedSection, config.section);
    }

    @Test
    void currentSchemaPreservesCustomValues() {
        TestConfig config = new TestConfig();
        config.rawValue = 99;
        config.section.value = 88;

        config.update(ResettingConfig.CURRENT_SCHEMA_VERSION);

        assertEquals(99, config.rawValue);
        assertEquals(88, config.section.value);
    }

    @Test
    void everySimplySwordsConfigUsesTheCurrentResetSchema() throws IOException {
        assertEquals(8, CONFIG_CLASSES.size());
        Path configRoot = Path.of("src/main/java/net/sweenus/simplyswords/config");
        for (String configClass : CONFIG_CLASSES) {
            String source = Files.readString(configRoot.resolve(configClass + ".java"));
            assertTrue(source.contains("@Version(version = ResettingConfig.CURRENT_SCHEMA_VERSION)"), configClass);
            assertTrue(source.contains("class " + configClass + " extends ResettingConfig"), configClass);
        }
    }

    public static final class TestConfig extends ResettingConfig {
        public int rawValue = 5;
        public TestSection section = new TestSection();

        public TestConfig() {
            super(Identifier.of("simplyswords", "reset_test"));
        }
    }

    public static final class TestSection {
        public int value = 7;
    }
}
