package net.sweenus.simplyswords.client.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class BoltRibbon {

    private static final float TAIL_FADE = 0.12F;
    private static final int LANES = 4;
    private static final float[] LANE_ALPHA = {0.0F, 1.0F, 1.0F, 0.0F};

    private BoltRibbon() {
    }

    public static void draw(VertexConsumer vertices, Matrix4f matrix, Vec3d cameraLocal,
                            float[] path, float startWidth, float endWidth, float glowScale,
                            int coreColor, int glowColor, float intensity, boolean fadeTail) {
        int points = path.length / 3;
        if (points < 2 || intensity <= 0.0F) {
            return;
        }

        float overbright = MathHelper.clamp(intensity - 1.0F, 0.0F, 1.0F);
        int core = whiten(coreColor, overbright);
        int glow = whiten(glowColor, overbright * 0.5F);
        float baseAlpha = MathHelper.clamp(intensity, 0.0F, 1.0F);

        float[] laneScale = {-glowScale, -1.0F, 1.0F, glowScale};
        int[] laneColor = {glow, core, core, glow};

        float[] progress = new float[points];
        float total = 0.0F;
        for (int i = 1; i < points; i++) {
            float dx = path[i * 3] - path[(i - 1) * 3];
            float dy = path[i * 3 + 1] - path[(i - 1) * 3 + 1];
            float dz = path[i * 3 + 2] - path[(i - 1) * 3 + 2];
            total += MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            progress[i] = total;
        }
        if (total < 1.0E-5F) {
            return;
        }
        for (int i = 1; i < points; i++) {
            progress[i] /= total;
        }

        float[] sides = new float[points * 3];
        computeSides(path, points, cameraLocal, sides);

        for (int i = 0; i < points - 1; i++) {
            float widthA = MathHelper.lerp(progress[i], startWidth, endWidth);
            float widthB = MathHelper.lerp(progress[i + 1], startWidth, endWidth);
            float alphaA = baseAlpha * tail(progress[i], fadeTail);
            float alphaB = baseAlpha * tail(progress[i + 1], fadeTail);
            if (alphaA <= 0.0F && alphaB <= 0.0F) {
                continue;
            }

            for (int lane = 0; lane < LANES - 1; lane++) {
                emitQuad(vertices, matrix, path, sides, i,
                        laneScale[lane] * widthA, laneScale[lane] * widthB,
                        laneColor[lane], LANE_ALPHA[lane] * alphaA, LANE_ALPHA[lane] * alphaB,
                        laneScale[lane + 1] * widthA, laneScale[lane + 1] * widthB,
                        laneColor[lane + 1], LANE_ALPHA[lane + 1] * alphaA,
                        LANE_ALPHA[lane + 1] * alphaB);
            }
        }
    }

    private static void emitQuad(VertexConsumer vertices, Matrix4f matrix, float[] path, float[] sides,
                                 int segment,
                                 float lowOffsetA, float lowOffsetB, int lowColor,
                                 float lowAlphaA, float lowAlphaB,
                                 float highOffsetA, float highOffsetB, int highColor,
                                 float highAlphaA, float highAlphaB) {
        int a = segment * 3;
        int b = (segment + 1) * 3;
        vertex(vertices, matrix, path, sides, a, lowOffsetA, lowColor, lowAlphaA);
        vertex(vertices, matrix, path, sides, a, highOffsetA, highColor, highAlphaA);
        vertex(vertices, matrix, path, sides, b, highOffsetB, highColor, highAlphaB);
        vertex(vertices, matrix, path, sides, b, lowOffsetB, lowColor, lowAlphaB);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float[] path, float[] sides,
                               int offset, float lane, int color, float alpha) {
        vertices.vertex(matrix,
                        path[offset] + sides[offset] * lane,
                        path[offset + 1] + sides[offset + 1] * lane,
                        path[offset + 2] + sides[offset + 2] * lane)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255,
                        MathHelper.clamp(Math.round(alpha * 255.0F), 0, 255));
    }

    private static void computeSides(float[] path, int points, Vec3d cameraLocal, float[] sides) {
        float cameraX = (float) cameraLocal.x;
        float cameraY = (float) cameraLocal.y;
        float cameraZ = (float) cameraLocal.z;
        float lastX = 0.0F;
        float lastY = 1.0F;
        float lastZ = 0.0F;
        boolean hasLast = false;

        for (int i = 0; i < points; i++) {
            int o = i * 3;
            float tangentX = 0.0F;
            float tangentY = 0.0F;
            float tangentZ = 0.0F;

            if (i > 0) {
                int p = (i - 1) * 3;
                float dx = path[o] - path[p];
                float dy = path[o + 1] - path[p + 1];
                float dz = path[o + 2] - path[p + 2];
                float length = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
                if (length > 1.0E-5F) {
                    tangentX += dx / length;
                    tangentY += dy / length;
                    tangentZ += dz / length;
                }
            }
            if (i < points - 1) {
                int n = (i + 1) * 3;
                float dx = path[n] - path[o];
                float dy = path[n + 1] - path[o + 1];
                float dz = path[n + 2] - path[o + 2];
                float length = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
                if (length > 1.0E-5F) {
                    tangentX += dx / length;
                    tangentY += dy / length;
                    tangentZ += dz / length;
                }
            }

            float viewX = path[o] - cameraX;
            float viewY = path[o + 1] - cameraY;
            float viewZ = path[o + 2] - cameraZ;

            float sideX = tangentY * viewZ - tangentZ * viewY;
            float sideY = tangentZ * viewX - tangentX * viewZ;
            float sideZ = tangentX * viewY - tangentY * viewX;
            float length = MathHelper.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);

            if (length > 1.0E-4F) {
                lastX = sideX / length;
                lastY = sideY / length;
                lastZ = sideZ / length;
                hasLast = true;
            } else if (!hasLast) {
                lastX = 0.0F;
                lastY = 1.0F;
                lastZ = 0.0F;
            }

            sides[o] = lastX;
            sides[o + 1] = lastY;
            sides[o + 2] = lastZ;
        }
    }

    private static float tail(float progress, boolean fadeTail) {
        if (!fadeTail || progress < 1.0F - TAIL_FADE) {
            return 1.0F;
        }
        return MathHelper.clamp((1.0F - progress) / TAIL_FADE, 0.0F, 1.0F);
    }

    private static int whiten(int color, float amount) {
        if (amount <= 0.0F) {
            return color;
        }
        int red = Math.round(MathHelper.lerp(amount, color >> 16 & 255, 255.0F));
        int green = Math.round(MathHelper.lerp(amount, color >> 8 & 255, 255.0F));
        int blue = Math.round(MathHelper.lerp(amount, color & 255, 255.0F));
        return red << 16 | green << 8 | blue;
    }

    public static void drawBranches(VertexConsumer vertices, Matrix4f matrix, Vec3d cameraLocal,
                                    float[] trunk, long seed, float age, int count, float length,
                                    float width, float glowScale, int coreColor, int glowColor,
                                    float intensity, int depth, float epochTicks, int generations) {
        int points = trunk.length / 3;
        if (points < 4 || count <= 0 || generations <= 0 || intensity <= 0.0F || depth < 3) {
            return;
        }

        for (int branch = 0; branch < count; branch++) {
            long key = seed * 131L + branch * 7919L;
            int index = MathHelper.clamp(
                    1 + (int) (BoltPath.unit(key, 11L, 0L) * (points - 2)), 1, points - 2);
            int origin = index * 3;

            int previous = (index - 1) * 3;
            int next = (index + 1) * 3;
            double tangentX = trunk[next] - trunk[previous];
            double tangentY = trunk[next + 1] - trunk[previous + 1];
            double tangentZ = trunk[next + 2] - trunk[previous + 2];
            double tangentLength = Math.sqrt(tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ);
            if (tangentLength < 1.0E-5) {
                continue;
            }
            tangentX /= tangentLength;
            tangentY /= tangentLength;
            tangentZ /= tangentLength;

            boolean useY = Math.abs(tangentY) < 0.9;
            double refX = useY ? 0.0 : 1.0;
            double refY = useY ? 1.0 : 0.0;
            double sideX = -tangentZ * refY;
            double sideY = tangentZ * refX;
            double sideZ = tangentX * refY - tangentY * refX;
            double sideLength = Math.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);
            if (sideLength < 1.0E-5) {
                continue;
            }
            sideX /= sideLength;
            sideY /= sideLength;
            sideZ /= sideLength;
            double upX = tangentY * sideZ - tangentZ * sideY;
            double upY = tangentZ * sideX - tangentX * sideZ;
            double upZ = tangentX * sideY - tangentY * sideX;

            double angle = BoltPath.unit(key, 23L, 0L) * MathHelper.TAU;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double forward = 0.18 + BoltPath.unit(key, 29L, 0L) * 0.24;
            double lateral = 0.55 + BoltPath.unit(key, 31L, 0L) * 0.45;
            double dirX = tangentX * forward + (sideX * cos + upX * sin) * lateral;
            double dirY = tangentY * forward + (sideY * cos + upY * sin) * lateral;
            double dirZ = tangentZ * forward + (sideZ * cos + upZ * sin) * lateral;
            double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (dirLength < 1.0E-5) {
                continue;
            }

            double reach = length * (0.65 + BoltPath.unit(key, 37L, 0L) * 0.9) / dirLength;
            Vec3d start = new Vec3d(trunk[origin], trunk[origin + 1], trunk[origin + 2]);
            Vec3d end = start.add(dirX * reach, dirY * reach, dirZ * reach);

            float[] path = BoltPath.generate(start, end, key, age, length * 0.3F, depth - 1, epochTicks);
            draw(vertices, matrix, cameraLocal, path, width, width * 0.35F, glowScale,
                    coreColor, glowColor, intensity, true);

            if (generations > 1) {
                drawBranches(vertices, matrix, cameraLocal, path, key, age,
                        Math.max(1, count / 2), length * 0.5F, width * 0.55F, glowScale,
                        coreColor, glowColor, intensity * 0.7F, depth - 1, epochTicks,
                        generations - 1);
            }
        }
    }
}
