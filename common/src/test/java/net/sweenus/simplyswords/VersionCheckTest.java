package net.sweenus.simplyswords;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionCheckTest {

    @Test
    void rejectsOlderIronsSpellbooksVersions() {
        assertFalse(VersionHelper.meetsMinimum("1.20.1-3.3.0", "1.20.1-3.4.0"));
        assertFalse(VersionHelper.meetsMinimum("1.20.1-3.3.9.9", "1.20.1-3.4.0"));
    }

    @Test
    void acceptsExactAndNewerIronsSpellbooksVersions() {
        assertTrue(VersionHelper.meetsMinimum("1.20.1-3.4.0", "1.20.1-3.4.0"));
        assertTrue(VersionHelper.meetsMinimum("1.20.1-3.4.0.9", "1.20.1-3.4.0"));
        assertTrue(VersionHelper.meetsMinimum("1.20.1-3.16.2", "1.20.1-3.4.0"));
    }

    @Test
    void comparesCurrentPlatformQualifiedVersionsNumerically() {
        assertTrue(VersionHelper.meetsMinimum("0.12.0+1.20.1", "0.10.0+1.20.1"));
        assertFalse(VersionHelper.meetsMinimum("0.9.0+1.20.1", "0.10.0+1.20.1"));
    }

    @Test
    void rejectsMissingAndMalformedVersions() {
        assertFalse(VersionHelper.meetsMinimum(null, "1.20.1-3.16.2"));
        assertFalse(VersionHelper.meetsMinimum("1.20.1-3.16.2", null));
        assertFalse(VersionHelper.meetsMinimum("", "1.20.1-3.16.2"));
        assertFalse(VersionHelper.meetsMinimum("not-a-version", "1.20.1-3.16.2"));
    }
}
