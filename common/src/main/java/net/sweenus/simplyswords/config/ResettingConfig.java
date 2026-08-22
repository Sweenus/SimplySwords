package net.sweenus.simplyswords.config;

import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Config base used when a release intentionally invalidates every older config value.
 */
public abstract class ResettingConfig extends me.fzzyhmstrs.fzzy_config.config.Config {

    public static final int CURRENT_SCHEMA_VERSION = 3;

    protected ResettingConfig(Identifier identifier) {
        super(identifier);
    }

    @Override
    public final void update(int deserializedVersion) {
        if (deserializedVersion >= CURRENT_SCHEMA_VERSION) {
            return;
        }

        restoreCurrentDefaults();
    }

    private void restoreCurrentDefaults() {
        try {
            ResettingConfig defaults = getClass().getDeclaredConstructor().newInstance();
            Class<?> configClass = getClass();
            while (configClass != ResettingConfig.class) {
                for (Field field : configClass.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || field.isSynthetic()) {
                        continue;
                    }
                    if (Modifier.isFinal(modifiers)) {
                        throw new IllegalStateException(
                                "Config field cannot be reset because it is final: " + field);
                    }
                    field.setAccessible(true);
                    field.set(this, field.get(defaults));
                }
                configClass = configClass.getSuperclass();
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to reset outdated config " + getId() + " to current defaults",
                    exception);
        }
    }
}
