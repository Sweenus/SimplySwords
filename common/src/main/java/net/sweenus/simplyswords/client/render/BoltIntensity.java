package net.sweenus.simplyswords.client.render;

import net.minecraft.util.math.MathHelper;

public final class BoltIntensity {

    private BoltIntensity() {
    }

    public static float of(long seed, float age, float strikePeriod) {
        return shimmer(seed, age) + restrike(seed, age, strikePeriod);
    }

    public static float shimmer(long seed, float age) {
        return 0.72F + 0.28F * valueNoise(seed, age * 0.55F);
    }

    public static float restrike(long seed, float age, float strikePeriod) {
        float period = Math.max(2.0F, strikePeriod);
        long window = MathHelper.floor(age / period);
        float offset = BoltPath.unit(seed, window, 977L) * period;
        float since = age - (window * period + offset);
        if (since < 0.0F || since > 2.0F) {
            return 0.0F;
        }
        float decay = 1.0F - since * 0.5F;
        return 0.8F * decay * decay;
    }

    public static float strikeEnvelope(float age, float lifetime) {
        if (age < 0.0F) {
            return 0.0F;
        }
        float span = Math.max(1.0F, lifetime);
        float attack = MathHelper.clamp(age / 1.5F, 0.0F, 1.0F);
        float remaining = MathHelper.clamp(1.0F - age / span, 0.0F, 1.0F);
        return attack * remaining * remaining;
    }

    public static float sustainEnvelope(float age, float lifetime, float fadeTicks) {
        if (age < 0.0F) {
            return 0.0F;
        }
        float span = Math.max(1.0F, lifetime);
        float fade = Math.max(1.0F, fadeTicks);
        float attack = MathHelper.clamp(age / 2.0F, 0.0F, 1.0F);
        float remaining = MathHelper.clamp((span - age) / fade, 0.0F, 1.0F);
        return attack * remaining;
    }

    private static float valueNoise(long seed, float x) {
        long cell = MathHelper.floor(x);
        float t = x - cell;
        float smooth = t * t * (3.0F - 2.0F * t);
        float a = BoltPath.unit(seed, cell, 613L);
        float b = BoltPath.unit(seed, cell + 1L, 613L);
        return MathHelper.lerp(smooth, a, b);
    }
}
