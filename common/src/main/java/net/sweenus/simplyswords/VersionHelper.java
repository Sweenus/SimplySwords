package net.sweenus.simplyswords;

import java.lang.module.ModuleDescriptor.Version;

final class VersionHelper {

    private VersionHelper() {
    }

    static boolean meetsMinimum(String installedVersion, String requiredVersion) {
        if (installedVersion == null || installedVersion.isBlank()
                || requiredVersion == null || requiredVersion.isBlank()) {
            return false;
        }
        try {
            return Version.parse(installedVersion).compareTo(Version.parse(requiredVersion)) >= 0;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
