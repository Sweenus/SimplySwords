package net.sweenus.simplyswords.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.reflect.Method;

@Environment(EnvType.CLIENT)
public final class IrisCompat {
    private static boolean initialized;
    private static Object irisApi;
    private static Method shadowPassMethod;

    private IrisCompat() {
    }

    public static boolean isRenderingShadowPass() {
        if (!initialized) {
            initialize();
        }
        if (irisApi == null || shadowPassMethod == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(shadowPassMethod.invoke(irisApi));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            irisApi = null;
            shadowPassMethod = null;
            return false;
        }
    }

    private static void initialize() {
        initialized = true;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstance = apiClass.getMethod("getInstance");
            irisApi = getInstance.invoke(null);
            shadowPassMethod = apiClass.getMethod("isRenderingShadowPass");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            irisApi = null;
            shadowPassMethod = null;
        }
    }
}
