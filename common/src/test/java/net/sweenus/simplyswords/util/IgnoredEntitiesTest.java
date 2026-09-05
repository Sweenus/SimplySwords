package net.sweenus.simplyswords.util;

import net.minecraft.entity.passive.BatEntity;
import net.sweenus.simplyswords.entity.WatcherBatEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IgnoredEntitiesTest {

    @Test
    void watcherBatsAreAlwaysIgnoredWithoutExcludingVanillaBats() {
        assertTrue(IgnoredEntities.isAlwaysIgnoredEntityClass(WatcherBatEntity.class));
        assertFalse(IgnoredEntities.isAlwaysIgnoredEntityClass(BatEntity.class));
        assertFalse(IgnoredEntities.isAlwaysIgnoredEntityClass(null));
    }
}
