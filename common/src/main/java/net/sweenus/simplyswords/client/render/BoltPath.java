package net.sweenus.simplyswords.client.render;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;

public final class BoltPath {

    public static final float STRIKE_EPOCH = 3.0F;
    public static final float DRIFT_EPOCH = 6.0F;

    private static final float ROUGHNESS = 0.55F;
    private static final float REFERENCE_AMPLITUDE = 0.30F;
    private static final float REFERENCE_SPREAD = 0.045F;
    private static final float MIN_AMPLITUDE_SCALE = 0.45F;
    private static final float MAX_AMPLITUDE_SCALE = 1.9F;
    private static final float MIN_AMPLITUDE = 0.10F;
    private static final float MAX_AMPLITUDE_RATIO = 0.16F;

    private static final int MIN_DEPTH = 2;
    private static final int MAX_DEPTH = 6;

    private BoltPath() {
    }

    public static float[] generate(Vec3d start, Vec3d end, long seed, float age,
                                   float amplitude, int depth, float epochTicks) {
        depth = MathHelper.clamp(depth, MIN_DEPTH, MAX_DEPTH);
        int points = (1 << depth) + 1;
        float[] path = new float[points * 3];

        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4) {
            for (int i = 0; i < points; i++) {
                path[i * 3] = (float) start.x;
                path[i * 3 + 1] = (float) start.y;
                path[i * 3 + 2] = (float) start.z;
            }
            return path;
        }

        double invLength = 1.0 / length;
        double dirX = dx * invLength;
        double dirY = dy * invLength;
        double dirZ = dz * invLength;

        double absX = Math.abs(dirX);
        double absY = Math.abs(dirY);
        double absZ = Math.abs(dirZ);
        double refX = 0.0;
        double refY = 0.0;
        double refZ = 0.0;
        if (absX <= absY && absX <= absZ) {
            refX = 1.0;
        } else if (absY <= absZ) {
            refY = 1.0;
        } else {
            refZ = 1.0;
        }

        double sideX = dirY * refZ - dirZ * refY;
        double sideY = dirZ * refX - dirX * refZ;
        double sideZ = dirX * refY - dirY * refX;
        double sideLength = Math.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);
        sideX /= sideLength;
        sideY /= sideLength;
        sideZ /= sideLength;

        double upX = dirY * sideZ - dirZ * sideY;
        double upY = dirZ * sideX - dirX * sideZ;
        double upZ = dirX * sideY - dirY * sideX;

        float scale = MathHelper.clamp(amplitude / REFERENCE_AMPLITUDE,
                MIN_AMPLITUDE_SCALE, MAX_AMPLITUDE_SCALE);
        float clamped = (float) MathHelper.clamp(length * REFERENCE_SPREAD * scale,
                MIN_AMPLITUDE, length * MAX_AMPLITUDE_RATIO);

        long epoch = MathHelper.floor(age / epochTicks);
        float blend = smoothstep(age / epochTicks - epoch);

        float[] current = new float[points * 2];
        float[] next = new float[points * 2];
        displace(current, points, depth, seed, epoch, clamped);
        displace(next, points, depth, seed, epoch + 1L, clamped);

        int last = points - 1;
        for (int i = 0; i < points; i++) {
            double t = i / (double) last;
            float lateral = MathHelper.lerp(blend, current[i * 2], next[i * 2]);
            float vertical = MathHelper.lerp(blend, current[i * 2 + 1], next[i * 2 + 1]);
            path[i * 3] = (float) (start.x + dx * t + sideX * lateral + upX * vertical);
            path[i * 3 + 1] = (float) (start.y + dy * t + sideY * lateral + upY * vertical);
            path[i * 3 + 2] = (float) (start.z + dz * t + sideZ * lateral + upZ * vertical);
        }
        return path;
    }

    private static void displace(float[] offsets, int points, int depth,
                                 long seed, long epoch, float amplitude) {
        int last = points - 1;
        offsets[0] = 0.0F;
        offsets[1] = 0.0F;
        offsets[last * 2] = 0.0F;
        offsets[last * 2 + 1] = 0.0F;

        int stride = last;
        float scale = amplitude;
        for (int level = 0; level < depth; level++) {
            int half = stride >> 1;
            for (int mid = half; mid < last; mid += stride) {
                int low = mid - half;
                int high = mid + half;
                float baseLateral = 0.5F * (offsets[low * 2] + offsets[high * 2]);
                float baseVertical = 0.5F * (offsets[low * 2 + 1] + offsets[high * 2 + 1]);
                float envelope = envelope(mid / (float) last);
                offsets[mid * 2] = baseLateral + scale * envelope * signedUnit(seed, epoch, mid, 0);
                offsets[mid * 2 + 1] = baseVertical + scale * envelope * signedUnit(seed, epoch, mid, 1);
            }
            stride = half;
            scale *= ROUGHNESS;
        }
    }

    private static float envelope(float t) {
        float sine = MathHelper.sin(t * MathHelper.PI);
        return 0.6F * (float) Math.sqrt(sine) + 0.4F * sine;
    }

    private static float smoothstep(float t) {
        float clamped = MathHelper.clamp(t, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    public static float unit(long seed, long a, long b) {
        long h = seed * 0x9E3779B97F4A7C15L
                + a * 0xBF58476D1CE4E5B9L
                + b * 0x94D049BB133111EBL;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 40) / (float) (1 << 24);
    }

    private static float signedUnit(long seed, long epoch, int index, int axis) {
        return unit(seed, epoch * 31L + axis, index) * 2.0F - 1.0F;
    }

    public static int depthForSegments(int segments) {
        int depth = Math.round((float) (Math.log(Math.max(2, segments)) / Math.log(2.0)));
        return MathHelper.clamp(depth, 3, 5);
    }

    public static int depthFor(double distanceSq, int baseDepth) {
        int depth = baseDepth + detailOffset();
        if (distanceSq > 64.0 * 64.0) {
            depth -= 3;
        } else if (distanceSq > 32.0 * 32.0) {
            depth -= 2;
        } else if (distanceSq > 16.0 * 16.0) {
            depth -= 1;
        }
        return MathHelper.clamp(depth, MIN_DEPTH, MAX_DEPTH);
    }

    private static int detailOffset() {
        return MathHelper.clamp(Config.general.stormEffectDetail, 0, 2) - 2;
    }
}
