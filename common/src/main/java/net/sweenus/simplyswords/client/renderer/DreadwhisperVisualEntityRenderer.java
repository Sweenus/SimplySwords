package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IonDeferredRenderController;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.entity.DreadwhisperVisualEntity;
import org.joml.Matrix4f;

public final class DreadwhisperVisualEntityRenderer extends EntityRenderer<DreadwhisperVisualEntity> {
    private static final Identifier WHITE = new Identifier("minecraft", "textures/misc/white.png");
    private static final Identifier SMOKE = new Identifier("minecraft", "textures/particle/big_smoke_6.png");
    private static final int VOID_BLACK = 0x09030F;
    private static final int DEEP_PURPLE = 0x210836;
    private static final int MID_PURPLE = 0x46116B;
    private static final int REND_PURPLE = 0x9B3CDD;
    private static final int SICKLY_GREEN = 0x78FF72;
    private static final int PALE_GREEN = 0xC0FF9C;

    public DreadwhisperVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(DreadwhisperVisualEntity entity) {
        return WHITE;
    }

    @Override
    public boolean shouldRender(DreadwhisperVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getVisibilityBoundingBox());
    }

    @Override
    public void render(DreadwhisperVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }
        matrices.push();
        if (entity.getKind() == DreadwhisperVisualEntity.REAVE_FRONT) {
            renderFront(entity, tickDelta, matrices, consumers);
        } else if (entity.getKind() == DreadwhisperVisualEntity.LEECH_HIT) {
            renderLeechHit(entity, tickDelta, matrices);
        } else if (entity.getKind() == DreadwhisperVisualEntity.WOUND_BURST) {
            renderWoundBurst(entity, tickDelta, matrices);
        }
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private void renderFront(DreadwhisperVisualEntity entity, float tickDelta, MatrixStack matrices,
                             VertexConsumerProvider consumers) {
        LivingEntity owner = entity.getOwner();
        if (entity.getCollapseAge() < 0 && owner != null) {
            Vec3d correction = owner.getLerpedPos(tickDelta).subtract(entity.getLerpedPos(tickDelta));
            matrices.translate(correction.x, correction.y, correction.z);
        }

        float age = entity.age + tickDelta;
        float appear = smooth(MathHelper.clamp(age / 2.2F, 0.0F, 1.0F));
        float fade = 1.0F;
        if (entity.getCollapseAge() >= 0) {
            fade = 1.0F - smooth(MathHelper.clamp(
                    (age - entity.getCollapseAge()) / Math.max(1.0F, entity.getLifetime() - entity.getCollapseAge()),
                    0.0F, 1.0F));
        }
        float opacity = appear * fade;
        if (opacity <= 0.01F) {
            return;
        }

        Vec3d direction = entity.getDirection();
        Vec3d side = new Vec3d(-direction.z, 0.0, direction.x);
        float width = entity.getVisualWidth() * (0.84F + appear * 0.16F);
        float height = entity.getVisualHeight();
        float firstPersonOpacity = 1.0F;
        if (isFirstPersonOwner(owner)) {
            matrices.translate(direction.x * 0.72, -0.52, direction.z * 0.72);
            firstPersonOpacity = 0.56F;
        }
        renderSmoke(entity, age, opacity, firstPersonOpacity, width, height, direction, side, matrices, consumers);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer field = IonDeferredRenderController.getFieldBuffer();

        for (int i = 0; i < 15; i++) {
            float sidePosition = (i & 1) == 0
                    ? MathHelper.lerp(noise(entity.getSeed() + 31, i), 0.18F, 0.54F) * width
                    : MathHelper.lerp(noise(entity.getSeed() + 31, i), -0.54F, -0.18F) * width;
            float edge = Math.abs(sidePosition) / Math.max(0.1F, width * 0.5F);
            float baseY = 0.18F + noise(entity.getSeed() + 59, i) * height * 0.52F;
            float radius = (0.46F + noise(entity.getSeed() + 83, i) * 0.48F)
                    * (0.92F + edge * 0.24F);
            float pulse = 0.92F + MathHelper.sin(age * 0.19F + i * 1.67F) * 0.08F;
            Vec3d center = side.multiply(sidePosition)
                    .add(direction.multiply((noise(entity.getSeed() + 107, i) - 0.5F) * 0.72F))
                    .add(0.0, baseY, 0.0);
            int color = i % 3 == 0 ? VOID_BLACK : i % 3 == 1 ? DEEP_PURPLE : MID_PURPLE;
            int alpha = alpha((i % 3 == 0 ? 170.0F : 132.0F) * opacity
                    * (edge < 0.26F ? firstPersonOpacity : 1.0F));
            cloudLobe(field, matrix, center, side, direction, radius * pulse,
                    radius * (0.72F + noise(entity.getSeed() + 137, i) * 0.55F), color, alpha,
                    noise(entity.getSeed() + 163, i) * MathHelper.TAU);
        }

        for (int i = 0; i < 12; i++) {
            float lateral = MathHelper.lerp(noise(entity.getSeed() + 211, i), -0.49F, 0.49F) * width;
            float y = 0.3F + noise(entity.getSeed() + 251, i) * height * 0.66F;
            float trailLength = (2.4F + noise(entity.getSeed() + 277, i) * 2.4F) * appear;
            float wobble = MathHelper.sin(age * 0.22F + i * 1.37F) * 0.16F;
            Vec3d start = side.multiply(lateral).add(direction.multiply(-0.1F)).add(0.0, y, 0.0);
            Vec3d middle = side.multiply(lateral + wobble)
                    .add(direction.multiply(-trailLength * 0.48F))
                    .add(0.0, y + (noise(entity.getSeed() + 313, i) - 0.5F) * 0.52F, 0.0);
            Vec3d end = side.multiply(lateral + wobble * 0.4F)
                    .add(direction.multiply(-trailLength))
                    .add(0.0, y + (noise(entity.getSeed() + 337, i) - 0.5F) * 0.72F, 0.0);
            float thickness = 0.13F + noise(entity.getSeed() + 359, i) * 0.14F;
            ribbon(field, matrix, start, middle, side, thickness, DEEP_PURPLE, alpha(150.0F * opacity));
            ribbon(field, matrix, middle, end, side, thickness * 0.58F, VOID_BLACK, alpha(122.0F * opacity));
        }

        VertexConsumer energy = IonDeferredRenderController.getLightningBuffer();
        for (int i = 0; i < 12; i++) {
            float lateral = MathHelper.lerp(noise(entity.getSeed() + 211, i), -0.49F, 0.49F) * width;
            float y = 0.3F + noise(entity.getSeed() + 251, i) * height * 0.66F;
            float trailLength = (2.4F + noise(entity.getSeed() + 277, i) * 2.4F) * appear;
            float wobble = MathHelper.sin(age * 0.22F + i * 1.37F) * 0.16F;
            Vec3d start = side.multiply(lateral).add(direction.multiply(-0.09F)).add(0.0, y, 0.0);
            Vec3d middle = side.multiply(lateral + wobble)
                    .add(direction.multiply(-trailLength * 0.48F))
                    .add(0.0, y + (noise(entity.getSeed() + 313, i) - 0.5F) * 0.52F, 0.0);
            Vec3d end = side.multiply(lateral + wobble * 0.4F)
                    .add(direction.multiply(-trailLength))
                    .add(0.0, y + (noise(entity.getSeed() + 337, i) - 0.5F) * 0.72F, 0.0);
            int color = i % 4 == 0 ? SICKLY_GREEN : REND_PURPLE;
            ribbon(energy, matrix, start, middle, side, 0.026F, color, alpha(220.0F * opacity));
            ribbon(energy, matrix, middle, end, side, 0.014F, color, alpha(176.0F * opacity));
        }

        float[] nodes = {-0.31F, 0.0F, 0.31F};
        for (int i = 0; i < nodes.length; i++) {
            float bob = MathHelper.sin(age * 0.34F + i * 2.1F) * 0.08F;
            Vec3d center = side.multiply(nodes[i] * width)
                    .add(direction.multiply(0.18F))
                    .add(0.0, height * (i == 1 ? 0.53F : 0.46F) + bob, 0.0);
            float size = (i == 1 ? 0.28F : 0.23F) * (0.9F + MathHelper.sin(age * 0.45F + i) * 0.1F);
            diamond(energy, matrix, center, side, size * 2.1F, size * 1.45F,
                    MID_PURPLE, alpha(150.0F * opacity));
            diamond(energy, matrix, center.add(direction.multiply(0.012F)), side, size * 1.25F, size,
                    SICKLY_GREEN, alpha(235.0F * opacity));
            diamond(energy, matrix, center.add(direction.multiply(0.018F)), side, size * 0.46F, size * 0.42F,
                    PALE_GREEN, alpha(245.0F * opacity));
        }

        for (int i = 0; i < 24; i++) {
            float phase = fract(noise(entity.getSeed() + 401, i) + age * (0.035F + noise(entity.getSeed() + 431, i) * 0.025F));
            float lateral = MathHelper.lerp(noise(entity.getSeed() + 457, i), -0.56F, 0.56F) * width;
            float y = 0.18F + noise(entity.getSeed() + 479, i) * height;
            float rear = phase * (3.2F + noise(entity.getSeed() + 503, i) * 1.7F);
            Vec3d head = side.multiply(lateral).add(direction.multiply(-rear)).add(0.0, y, 0.0);
            Vec3d tail = head.add(direction.multiply(-0.22F - noise(entity.getSeed() + 541, i) * 0.38F))
                    .add(0.0, (noise(entity.getSeed() + 563, i) - 0.5F) * 0.25F, 0.0);
            ribbon(energy, matrix, head, tail, side, 0.018F + noise(entity.getSeed() + 587, i) * 0.016F,
                    i % 5 == 0 ? SICKLY_GREEN : REND_PURPLE,
                    alpha((170.0F + (1.0F - phase) * 65.0F) * opacity));
        }
    }

    private void renderSmoke(DreadwhisperVisualEntity entity, float age, float opacity,
                             float firstPersonOpacity, float width, float height,
                             Vec3d direction, Vec3d side, MatrixStack matrices,
                             VertexConsumerProvider consumers) {
        VertexConsumer smoke = consumers.getBuffer(RenderLayer.getEntityTranslucent(SMOKE));
        for (int i = 0; i < 12; i++) {
            float sign = (i & 1) == 0 ? 1.0F : -1.0F;
            float lateral = sign * MathHelper.lerp(noise(entity.getSeed() + 619, i), 0.2F, 0.52F) * width;
            float vertical = 0.24F + noise(entity.getSeed() + 647, i) * height * 0.54F;
            float depth = (noise(entity.getSeed() + 673, i) - 0.5F) * 0.8F;
            float pulse = 0.91F + MathHelper.sin(age * 0.14F + i * 1.91F) * 0.09F;
            float size = (0.62F + noise(entity.getSeed() + 701, i) * 0.52F) * pulse;
            int smokeAlpha = alpha((68.0F + noise(entity.getSeed() + 727, i) * 42.0F)
                    * opacity * (Math.abs(lateral) < width * 0.24F ? firstPersonOpacity : 1.0F));
            matrices.push();
            Vec3d center = side.multiply(lateral).add(direction.multiply(depth)).add(0.0, vertical, 0.0);
            matrices.translate(center.x, center.y, center.z);
            matrices.multiply(dispatcher.getRotation());
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    noise(entity.getSeed() + 751, i) * 360.0F + age * ((i & 1) == 0 ? 1.1F : -0.9F)));
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            smokeVertex(smoke, matrix, -size, -size * 0.78F, 0.0F, 1.0F, 72, 22, 105, smokeAlpha);
            smokeVertex(smoke, matrix, size, -size * 0.78F, 1.0F, 1.0F, 72, 22, 105, smokeAlpha);
            smokeVertex(smoke, matrix, size, size * 0.78F, 1.0F, 0.0F, 72, 22, 105, smokeAlpha);
            smokeVertex(smoke, matrix, -size, size * 0.78F, 0.0F, 0.0F, 72, 22, 105, smokeAlpha);
            matrices.pop();
        }
    }

    private static void renderLeechHit(DreadwhisperVisualEntity entity, float tickDelta, MatrixStack matrices) {
        float age = entity.age + tickDelta;
        float progress = MathHelper.clamp(age / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float fade = 1.0F - smooth(progress);
        float burst = smooth(MathHelper.clamp(age / 2.0F, 0.0F, 1.0F));
        LivingEntity owner = entity.getOwner();
        Vec3d ownerPoint = owner == null ? new Vec3d(0.0, 0.7, 0.0)
                : owner.getLerpedPos(tickDelta).add(0.0, owner.getHeight() * 0.55, 0.0)
                .subtract(entity.getLerpedPos(tickDelta));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer field = IonDeferredRenderController.getFieldBuffer();

        for (int i = 0; i < 10; i++) {
            float angle = MathHelper.TAU * i / 10.0F + noise(entity.getSeed(), i) * 0.5F;
            Vec3d direction = new Vec3d(MathHelper.sin(angle),
                    (noise(entity.getSeed() + 29, i) - 0.28F) * 0.85F,
                    MathHelper.cos(angle)).normalize();
            Vec3d side = perpendicular(direction);
            Vec3d start = direction.multiply(entity.getVisualWidth() * 0.12F);
            Vec3d end = direction.multiply(entity.getVisualWidth() * (0.5F + noise(entity.getSeed() + 53, i) * 0.8F) * burst);
            ribbon(field, matrix, start, end, side, 0.055F, DEEP_PURPLE, alpha(172.0F * fade));
        }

        VertexConsumer energy = IonDeferredRenderController.getLightningBuffer();
        for (int i = 0; i < 10; i++) {
            float angle = MathHelper.TAU * i / 10.0F + noise(entity.getSeed(), i) * 0.5F;
            Vec3d direction = new Vec3d(MathHelper.sin(angle),
                    (noise(entity.getSeed() + 29, i) - 0.28F) * 0.85F,
                    MathHelper.cos(angle)).normalize();
            Vec3d side = perpendicular(direction);
            Vec3d start = direction.multiply(entity.getVisualWidth() * 0.12F);
            Vec3d end = direction.multiply(entity.getVisualWidth() * (0.5F + noise(entity.getSeed() + 53, i) * 0.8F) * burst);
            ribbon(energy, matrix, start, end, side, 0.018F,
                    i % 3 == 0 ? SICKLY_GREEN : REND_PURPLE, alpha(238.0F * fade));
        }

        if (owner != null && ownerPoint.lengthSquared() > 0.25) {
            for (int stream = 0; stream < 3; stream++) {
                Vec3d offset = perpendicular(ownerPoint).multiply((stream - 1) * 0.13F);
                Vec3d start = offset;
                Vec3d end = ownerPoint.add(offset.multiply(-0.35));
                Vec3d midpoint = start.add(end).multiply(0.5)
                        .add(0.0, 0.45F + stream * 0.11F, 0.0)
                        .add(perpendicular(end.subtract(start)).multiply((stream - 1) * 0.18F));
                curvedRibbon(energy, matrix, start, midpoint, end, 7, 0.026F,
                        stream == 1 ? PALE_GREEN : SICKLY_GREEN, alpha(218.0F * fade));
                float pulse = fract(progress * 2.25F - stream * 0.18F);
                Vec3d pulsePoint = quadratic(start, midpoint, end, pulse);
                diamond(energy, matrix, pulsePoint, perpendicular(ownerPoint), 0.13F, 0.13F,
                        PALE_GREEN, alpha(245.0F * fade));
            }
        }
    }

    private static void renderWoundBurst(DreadwhisperVisualEntity entity, float tickDelta, MatrixStack matrices) {
        float age = entity.age + tickDelta;
        float progress = MathHelper.clamp(age / Math.max(1.0F, entity.getLifetime()), 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        float fade = 1.0F - smooth(progress);
        float flash = 1.0F - smooth(MathHelper.clamp(age / 3.2F, 0.0F, 1.0F));
        float span = Math.max(entity.getVisualWidth(), entity.getVisualHeight() * 0.46F);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer field = IonDeferredRenderController.getFieldBuffer();
        diamond(field, matrix, Vec3d.ZERO, new Vec3d(1.0, 0.0, 0.0),
                entity.getVisualWidth() * (0.34F + flash * 0.16F),
                entity.getVisualHeight() * (0.2F + flash * 0.08F),
                VOID_BLACK, alpha((205.0F + flash * 35.0F) * fade));
        for (int i = 0; i < 10; i++) {
            Vec3d direction = burstDirection(entity.getSeed(), i, 10);
            Vec3d side = perpendicular(direction);
            Vec3d up = side.crossProduct(direction).normalize();
            float distance = eased * span * (0.28F + noise(entity.getSeed() + 149, i) * 1.05F);
            Vec3d center = direction.multiply(distance);
            float width = span * (0.065F + noise(entity.getSeed() + 211, i) * 0.075F);
            float height = span * (0.09F + noise(entity.getSeed() + 239, i) * 0.11F);
            Vec3d trailStart = center.subtract(direction.multiply(span * (0.16F + noise(entity.getSeed() + 263, i) * 0.2F)));
            ribbon(field, matrix, trailStart, center, side, width * 0.28F,
                    VOID_BLACK, alpha(118.0F * fade));
            tornFragment(field, matrix, center, side, up, width, height,
                    i % 3 == 0 ? MID_PURPLE : DEEP_PURPLE, alpha(205.0F * fade));
        }
        VertexConsumer energy = IonDeferredRenderController.getLightningBuffer();
        diamond(energy, matrix, Vec3d.ZERO, new Vec3d(1.0, 0.0, 0.0),
                entity.getVisualWidth() * (0.2F + flash * 0.16F),
                entity.getVisualHeight() * (0.12F + flash * 0.1F),
                SICKLY_GREEN, alpha(248.0F * flash));
        diamond(energy, matrix, Vec3d.ZERO, new Vec3d(0.0, 0.0, 1.0),
                entity.getVisualWidth() * (0.16F + flash * 0.12F),
                entity.getVisualHeight() * (0.1F + flash * 0.08F),
                PALE_GREEN, alpha(235.0F * flash));
        for (int i = 0; i < 10; i++) {
            Vec3d direction = burstDirection(entity.getSeed(), i, 10);
            Vec3d side = perpendicular(direction);
            Vec3d up = side.crossProduct(direction).normalize();
            float distance = eased * span * (0.28F + noise(entity.getSeed() + 149, i) * 1.05F);
            Vec3d center = direction.multiply(distance).add(direction.multiply(0.003F));
            float width = span * (0.03F + noise(entity.getSeed() + 211, i) * 0.035F);
            float height = span * (0.045F + noise(entity.getSeed() + 239, i) * 0.05F);
            tornFragment(energy, matrix, center, side, up, width, height,
                    i % 3 == 0 ? SICKLY_GREEN : REND_PURPLE, alpha(238.0F * fade));
        }
    }

    private static Vec3d burstDirection(long seed, int index, int count) {
        float angle = MathHelper.TAU * index / count + noise(seed, index) * 0.52F;
        return new Vec3d(MathHelper.sin(angle),
                0.08F + noise(seed + 83, index) * 0.68F,
                MathHelper.cos(angle)).normalize();
    }

    private static void tornFragment(VertexConsumer vertices, Matrix4f matrix, Vec3d center,
                                     Vec3d side, Vec3d up, float width, float height,
                                     int color, int alpha) {
        Vec3d a = center.add(up.multiply(height)).add(side.multiply(width * 0.18F));
        Vec3d b = center.add(side.multiply(width)).subtract(up.multiply(height * 0.16F));
        Vec3d c = center.subtract(up.multiply(height)).subtract(side.multiply(width * 0.3F));
        Vec3d d = center.subtract(side.multiply(width)).add(up.multiply(height * 0.12F));
        quad(vertices, matrix, a, b, c, d, color, alpha);
        quad(vertices, matrix, d, c, b, a, color, alpha);
    }

    private static void cloudLobe(VertexConsumer vertices, Matrix4f matrix, Vec3d center,
                                  Vec3d side, Vec3d depth, float radius, float height,
                                  int color, int alpha, float rotation) {
        if (alpha <= 0) {
            return;
        }
        Vec3d horizontal = side.multiply(MathHelper.cos(rotation)).add(depth.multiply(MathHelper.sin(rotation))).normalize();
        Vec3d vertical = new Vec3d(0.0, 1.0, 0.0);
        Vec3d a = center.add(horizontal.multiply(-radius * 0.68F));
        Vec3d b = center.add(horizontal.multiply(-radius)).add(vertical.multiply(height * 0.18F));
        Vec3d c = center.add(horizontal.multiply(-radius * 0.52F)).add(vertical.multiply(height * 0.72F));
        Vec3d d = center.add(horizontal.multiply(-radius * 0.12F)).add(vertical.multiply(height));
        Vec3d e = center.add(horizontal.multiply(radius * 0.55F)).add(vertical.multiply(height * 0.78F));
        Vec3d f = center.add(horizontal.multiply(radius)).add(vertical.multiply(height * 0.3F));
        Vec3d g = center.add(horizontal.multiply(radius * 0.66F)).add(vertical.multiply(-height * 0.16F));
        Vec3d h = center.add(horizontal.multiply(-radius * 0.18F)).add(vertical.multiply(-height * 0.28F));
        quad(vertices, matrix, a, b, c, center, color, alpha);
        quad(vertices, matrix, center, c, d, e, color, alpha);
        quad(vertices, matrix, center, e, f, g, color, alpha);
        quad(vertices, matrix, center, g, h, a, color, alpha);
        Vec3d cross = depth.multiply(MathHelper.cos(rotation)).subtract(side.multiply(MathHelper.sin(rotation))).normalize();
        diamond(vertices, matrix, center.add(0.0, height * 0.28F, 0.0), cross,
                radius * 0.82F, height * 0.7F, color, Math.max(1, alpha * 3 / 4));
    }

    private static void curvedRibbon(VertexConsumer vertices, Matrix4f matrix,
                                     Vec3d start, Vec3d control, Vec3d end, int segments,
                                     float width, int color, int alpha) {
        Vec3d previous = start;
        for (int i = 1; i <= segments; i++) {
            float progress = i / (float) segments;
            Vec3d current = quadratic(start, control, end, progress);
            ribbon(vertices, matrix, previous, current, perpendicular(current.subtract(previous)),
                    width * (1.0F - progress * 0.45F), color, alpha);
            previous = current;
        }
    }

    private static Vec3d quadratic(Vec3d start, Vec3d control, Vec3d end, float progress) {
        double inverse = 1.0 - progress;
        return start.multiply(inverse * inverse)
                .add(control.multiply(2.0 * inverse * progress))
                .add(end.multiply(progress * progress));
    }

    private static void diamond(VertexConsumer vertices, Matrix4f matrix, Vec3d center,
                                Vec3d horizontal, float halfWidth, float halfHeight,
                                int color, int alpha) {
        Vec3d axis = horizontal.lengthSquared() < 1.0E-6 ? new Vec3d(1.0, 0.0, 0.0) : horizontal.normalize();
        Vec3d top = center.add(0.0, halfHeight, 0.0);
        Vec3d right = center.add(axis.multiply(halfWidth));
        Vec3d bottom = center.add(0.0, -halfHeight, 0.0);
        Vec3d left = center.subtract(axis.multiply(halfWidth));
        quad(vertices, matrix, top, right, bottom, left, color, alpha);
        quad(vertices, matrix, left, bottom, right, top, color, alpha);
    }

    private static void ribbon(VertexConsumer vertices, Matrix4f matrix, Vec3d start, Vec3d end,
                               Vec3d side, float halfWidth, int color, int alpha) {
        if (alpha <= 0 || side.lengthSquared() < 1.0E-8) {
            return;
        }
        Vec3d offset = side.normalize().multiply(halfWidth);
        quad(vertices, matrix, start.add(offset), start.subtract(offset), end.subtract(offset), end.add(offset), color, alpha);
        quad(vertices, matrix, end.add(offset), end.subtract(offset), start.subtract(offset), start.add(offset), color, alpha);
    }

    private static Vec3d perpendicular(Vec3d direction) {
        Vec3d horizontal = new Vec3d(-direction.z, 0.0, direction.x);
        if (horizontal.lengthSquared() < 1.0E-6) {
            return new Vec3d(1.0, 0.0, 0.0);
        }
        return horizontal.normalize();
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix,
                             Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color, int alpha) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        vertices.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(red, green, blue, alpha).next();
        vertices.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(red, green, blue, alpha).next();
        vertices.vertex(matrix, (float) c.x, (float) c.y, (float) c.z).color(red, green, blue, alpha).next();
        vertices.vertex(matrix, (float) d.x, (float) d.y, (float) d.z).color(red, green, blue, alpha).next();
    }

    private static void smokeVertex(VertexConsumer vertices, Matrix4f matrix,
                                    float x, float y, float u, float v,
                                    int red, int green, int blue, int alpha) {
        vertices.vertex(matrix, x, y, 0.0F)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(0.0F, 1.0F, 0.0F).next();
    }

    private static boolean isFirstPersonOwner(LivingEntity owner) {
        MinecraftClient client = MinecraftClient.getInstance();
        return owner != null && owner == client.player && client.options.getPerspective().isFirstPerson();
    }

    private static int alpha(float value) {
        return MathHelper.clamp(Math.round(value), 0, 255);
    }

    private static float noise(long seed, int index) {
        long value = seed + index * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value & 0xFFFFFFL) / (float) 0x1000000;
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float fract(float value) {
        return value - MathHelper.floor(value);
    }
}
