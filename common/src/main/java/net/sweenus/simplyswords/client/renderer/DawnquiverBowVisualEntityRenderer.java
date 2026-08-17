package net.sweenus.simplyswords.client.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.render.IrisCompat;
import net.sweenus.simplyswords.client.render.LightningRenderLayers;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DawnquiverBowVisualEntity;
import org.joml.Matrix4f;

public final class DawnquiverBowVisualEntityRenderer extends EntityRenderer<DawnquiverBowVisualEntity> {

    private static final Identifier WHITE_TEXTURE = Identifier.ofVanilla("textures/misc/white.png");
    private static final int LIMB_SPANS = 18;
    private static final int FEATHER_SPANS = 10;
    private static final float FORMATION_TICKS = 7.0F;
    private static final float RELEASE_TICKS = 6.0F;
    private static final float FADE_OUT_TICKS = 5.0F;

    public DawnquiverBowVisualEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public Identifier getTexture(DawnquiverBowVisualEntity entity) {
        return WHITE_TEXTURE;
    }

    @Override
    public boolean shouldRender(DawnquiverBowVisualEntity entity, Frustum frustum,
                                double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z)
                || frustum.isVisible(entity.getVisibilityBoundingBox());
    }

    @Override
    public void render(DawnquiverBowVisualEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (IrisCompat.isRenderingShadowPass()) {
            return;
        }

        float age = entity.age + tickDelta;
        float remaining = entity.getLifetime() - age;
        float opacity = MathHelper.clamp(age / 2.0F, 0.0F, 1.0F)
                * MathHelper.clamp(remaining / FADE_OUT_TICKS, 0.0F, 1.0F);
        if (opacity <= 0.01F) {
            return;
        }

        boolean passive = entity.getMode() == DawnquiverBowVisualEntity.MODE_PASSIVE;
        float formTicks = passive ? 2.4F : FORMATION_TICKS;
        float formation = DawnquiverRenderGeometry.easeOutBack(
                MathHelper.clamp(age / formTicks, 0.0F, 1.0F));
        boolean released = entity.getPhase() == DawnquiverBowVisualEntity.PHASE_RELEASED;
        float releaseDelay = passive ? 2.0F : 0.0F;
        float sinceRelease = released
                ? Math.max(0.0F, age - entity.getReleaseAge() - releaseDelay)
                : 0.0F;
        float snap = released ? DawnquiverRenderGeometry.easeOutCubic(
                MathHelper.clamp(sinceRelease / RELEASE_TICKS, 0.0F, 1.0F)) : 0.0F;
        float draw = passive ? 1.0F : entity.getDrawProgress();

        LivingEntity owner = entity.getOwner();
        boolean firstPerson = isFirstPersonOwner(owner);
        Vec3d entityOrigin = entity.getLerpedPos(tickDelta);
        Vec3d aimDirection = renderDirection(entity, owner, tickDelta);
        Vec3d displayDirection = firstPerson && owner != null
                ? normalizedOrFallback(owner.getRotationVec(tickDelta), aimDirection)
                : aimDirection;
        Vec3d renderOrigin = renderOrigin(entity, owner, tickDelta, firstPerson, displayDirection);
        float renderYaw = directionYaw(aimDirection);
        float renderPitch = directionPitch(aimDirection);

        float configuredScale = (float) Math.max(0.1, Config.uniqueEffects.dawnquiver.bowScale);
        float baseScale = entity.getScale() * configuredScale;
        float firstPersonScale = firstPerson ? 0.58F : 1.0F;
        float formedScale = MathHelper.lerp(MathHelper.clamp(formation, 0.0F, 1.0F), 0.28F, 1.0F);
        float chargeScale = MathHelper.lerp(draw, 0.92F, 1.12F);
        float releaseScale = MathHelper.lerp(snap, 1.0F, 0.78F);
        float scale = baseScale * firstPersonScale * formedScale * chargeScale * releaseScale;
        float roll = firstPerson ? 0.0F
                : (entity.getHand() == Hand.OFF_HAND ? -1.0F : 1.0F) * 7.0F;

        matrices.push();
        matrices.translate(renderOrigin.x - entityOrigin.x,
                renderOrigin.y - entityOrigin.y,
                renderOrigin.z - entityOrigin.z);
        if (firstPerson) {
            matrices.multiply(this.dispatcher.getRotation());
        } else {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-renderYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(renderPitch));
        }
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.scale(scale, scale, scale);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        VertexConsumer body = consumers.getBuffer(LightningRenderLayers.BLOCKY_LIGHTNING);
        VertexConsumer glow = consumers.getBuffer(LightningRenderLayers.LIGHTNING);

        drawBow(body, glow, matrix, age, draw, formation, snap, opacity);
        if (Config.general.enableModernFieldEffects) {
            drawFeatherBloom(body, glow, matrix, age, formation, snap, opacity, entity.getSeed());
            drawChargeHalo(glow, matrix, age, draw, snap, opacity);
        }
        drawString(body, glow, matrix, draw, snap, opacity);
        drawEnergyArrow(body, glow, matrix, age, draw, snap, opacity);
        if (released) {
            drawReleaseFlare(glow, matrix, age, snap, opacity);
        }

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static boolean isFirstPersonOwner(LivingEntity owner) {
        MinecraftClient client = MinecraftClient.getInstance();
        return owner != null && owner == client.player && client.options.getPerspective().isFirstPerson();
    }

    private static Vec3d renderDirection(DawnquiverBowVisualEntity entity, LivingEntity owner,
                                         float tickDelta) {
        LivingEntity target = entity.getTarget();
        if (target != null) {
            Vec3d source = entity.getLerpedPos(tickDelta);
            Vec3d targetPoint = target.getLerpedPos(tickDelta).add(0.0, target.getHeight() * 0.55, 0.0);
            Vec3d direction = targetPoint.subtract(source);
            if (direction.lengthSquared() >= 1.0E-6) {
                return direction.normalize();
            }
        }
        if (owner != null && entity.getPhase() == DawnquiverBowVisualEntity.PHASE_DRAWING) {
            Vec3d direction = owner.getRotationVec(tickDelta);
            if (direction.lengthSquared() >= 1.0E-6) {
                return direction.normalize();
            }
        }
        Vec3d direction = entity.getRotationVec(tickDelta);
        return direction.lengthSquared() < 1.0E-6 ? new Vec3d(0.0, 0.0, 1.0) : direction.normalize();
    }

    private static Vec3d normalizedOrFallback(Vec3d direction, Vec3d fallback) {
        return direction.lengthSquared() < 1.0E-6 ? fallback : direction.normalize();
    }

    private static Vec3d renderOrigin(DawnquiverBowVisualEntity entity, LivingEntity owner,
                                      float tickDelta, boolean firstPerson, Vec3d forward) {
        if (owner == null) {
            return entity.getLerpedPos(tickDelta);
        }
        Vec3d right = new Vec3d(0.0, 1.0, 0.0).crossProduct(forward);
        if (right.lengthSquared() < 1.0E-6) {
            float yaw = directionYaw(forward) * MathHelper.RADIANS_PER_DEGREE;
            right = new Vec3d(MathHelper.cos(yaw), 0.0, MathHelper.sin(yaw));
        } else {
            right = right.normalize();
        }
        Vec3d up = forward.crossProduct(right).normalize();
        double handSide = entity.getHand() == Hand.OFF_HAND ? 1.0 : -1.0;

        if (entity.getMode() == DawnquiverBowVisualEntity.MODE_PASSIVE) {
            Vec3d base = owner.getCameraPosVec(tickDelta);
            if (firstPerson) {
                return base.add(forward.multiply(0.9))
                        .add(right.multiply(handSide * 0.38))
                        .add(up.multiply(0.08));
            }
            return base.add(right.multiply(handSide * 0.62)).add(up.multiply(0.16));
        }

        double distance = Math.max(0.35, Config.uniqueEffects.dawnquiver.bowDistance);
        if (firstPerson) {
            return owner.getCameraPosVec(tickDelta)
                    .add(forward.multiply(Math.max(0.9, distance)))
                    .add(right.multiply(handSide * 0.13))
                    .subtract(up.multiply(0.18));
        }
        return owner.getLerpedPos(tickDelta)
                .add(0.0, owner.getHeight() * 0.62, 0.0)
                .add(forward.multiply(distance));
    }

    private static float directionYaw(Vec3d direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    private static float directionPitch(Vec3d direction) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
    }

    private static void drawBow(VertexConsumer body, VertexConsumer glow, Matrix4f matrix,
                                float age, float draw, float formation, float snap, float opacity) {
        for (float sign : new float[]{-1.0F, 1.0F}) {
            Vec3d[] path = limbPath(sign, draw, formation, snap);
            double[] widths = limbWidths(draw, snap);
            double[] aura = scaledWidths(widths, 2.25, 0.018);
            double[] core = scaledWidths(widths, 0.34, 0.0);
            double[] verticalAura = scaledWidths(widths, 0.82, 0.012);
            double[] verticalBody = scaledWidths(widths, 0.42, 0.004);
            double[] verticalCore = scaledWidths(widths, 0.14, 0.002);

            DawnquiverRenderGeometry.profiledRibbonXZ(glow, matrix, path, aura,
                    104, 235, 255, 196, 255, 250,
                    Math.round(72 * opacity), 0);
            DawnquiverRenderGeometry.profiledRibbonXZ(body, matrix, path, widths,
                    210, 255, 255, 255, 250, 210,
                    Math.round(225 * opacity), Math.round(150 * opacity));
            DawnquiverRenderGeometry.profiledRibbonXZ(glow, matrix, path, core,
                    255, 255, 248, 255, 232, 142,
                    Math.round(245 * opacity), Math.round(205 * opacity));
            DawnquiverRenderGeometry.profiledRibbonWithSide(glow, matrix, path, verticalAura,
                    new Vec3d(0.0, 1.0, 0.0),
                    104, 235, 255, 196, 255, 250,
                    Math.round(62 * opacity), 0);
            DawnquiverRenderGeometry.profiledRibbonWithSide(body, matrix, path, verticalBody,
                    new Vec3d(0.0, 1.0, 0.0),
                    210, 255, 255, 255, 250, 210,
                    Math.round(220 * opacity), Math.round(145 * opacity));
            DawnquiverRenderGeometry.profiledRibbonWithSide(glow, matrix, path, verticalCore,
                    new Vec3d(0.0, 1.0, 0.0),
                    255, 255, 248, 255, 232, 142,
                    Math.round(240 * opacity), Math.round(195 * opacity));

            drawLimbProngs(body, glow, matrix, path, sign, opacity, snap);
        }

        float pulse = 0.94F + MathHelper.sin(age * 0.45F) * 0.06F;
        DawnquiverRenderGeometry.billboardLens(glow, matrix, new Vec3d(0.0, 0.0, 0.09),
                0.21F * pulse, 0.21F * pulse, 255, 244, 192, Math.round(185 * opacity));
        DawnquiverRenderGeometry.billboardLens(body, matrix, new Vec3d(0.0, 0.0, 0.092),
                0.09F, 0.15F, 239, 255, 255, Math.round(235 * opacity));
    }

    private static Vec3d[] limbPath(float sign, float draw, float formation, float snap) {
        float reach = MathHelper.lerp(snap, 1.0F, 0.54F);
        float belly = 0.39F + draw * 0.15F;
        float form = MathHelper.clamp(formation, 0.0F, 1.0F);
        Vec3d[] path = new Vec3d[LIMB_SPANS + 1];
        for (int index = 0; index <= LIMB_SPANS; index++) {
            float t = index / (float) LIMB_SPANS;
            double span = sign * 1.22 * reach * t;
            double bend = 0.09 + Math.sin(t * Math.PI * 0.86) * belly;
            double tipCurl = Math.pow(Math.max(0.0F, t - 0.76F) / 0.24F, 2.0);
            bend -= tipCurl * 0.34 * reach;

            double swirl = (1.0 - form) * sign * (0.82 + t * 0.45);
            double cos = Math.cos(swirl);
            double sin = Math.sin(swirl);
            double formedSpan = (span * cos - bend * sin) * form;
            double formedBend = (span * sin + bend * cos) * form;
            path[index] = new Vec3d(formedSpan, 0.0,
                    formedBend + snap * (0.14 + 0.28 * t));
        }
        return path;
    }

    private static double[] limbWidths(float draw, float snap) {
        double peak = MathHelper.lerp(snap, 0.145F, 0.095F) * (0.9F + draw * 0.18F);
        double[] widths = new double[LIMB_SPANS + 1];
        for (int index = 0; index <= LIMB_SPANS; index++) {
            float t = index / (float) LIMB_SPANS;
            double root = MathHelper.clamp(t / 0.19F, 0.0F, 1.0F);
            double taper = Math.pow(1.0 - t, 1.25) * 0.94 + 0.06;
            widths[index] = peak * (0.28 + root * 0.72) * taper;
        }
        return widths;
    }

    private static double[] scaledWidths(double[] widths, double scale, double extra) {
        double[] result = new double[widths.length];
        for (int index = 0; index < widths.length; index++) {
            result[index] = widths[index] * scale + extra;
        }
        return result;
    }

    private static void drawLimbProngs(VertexConsumer body, VertexConsumer glow, Matrix4f matrix,
                                       Vec3d[] path, float sign, float opacity, float snap) {
        for (int branch = 0; branch < 2; branch++) {
            int index = branch == 0 ? 8 : 12;
            Vec3d root = path[index];
            Vec3d tangent = path[index + 1].subtract(path[index - 1]).normalize();
            Vec3d outward = new Vec3d(-tangent.z, 0.0, tangent.x).multiply(sign).normalize();
            double length = (branch == 0 ? 0.31 : 0.23) * (1.0 - snap * 0.35);
            Vec3d tip = root.add(outward.multiply(length)).add(sign * 0.05, 0.015, 0.0);
            DawnquiverRenderGeometry.chevronXZ(glow, matrix, tip, outward, length, 0.13,
                    116, 244, 255, Math.round(160 * opacity));
            DawnquiverRenderGeometry.chevronXZ(body, matrix, tip, outward, length * 0.78, 0.07,
                    240, 255, 252, Math.round(205 * opacity));
            DawnquiverRenderGeometry.chevronWithSide(glow, matrix, tip, outward,
                    new Vec3d(0.0, 1.0, 0.0), length * 0.82, 0.055,
                    116, 244, 255, Math.round(135 * opacity));
            DawnquiverRenderGeometry.chevronWithSide(body, matrix, tip, outward,
                    new Vec3d(0.0, 1.0, 0.0), length * 0.68, 0.028,
                    240, 255, 252, Math.round(190 * opacity));
        }
    }

    private static void drawFeatherBloom(VertexConsumer body, VertexConsumer glow, Matrix4f matrix,
                                         float age, float formation, float snap, float opacity, int seed) {
        float form = MathHelper.clamp(formation, 0.0F, 1.0F);
        float bloom = MathHelper.clamp(age / 4.0F, 0.0F, 1.0F) * (1.0F - snap);
        for (float sign : new float[]{-1.0F, 1.0F}) {
            for (int feather = 0; feather < 3; feather++) {
                Vec3d[] path = featherPath(sign, feather, form, seed);
                double startWidth = 0.085 - feather * 0.012;
                int alpha = Math.round((115 - feather * 18) * bloom * opacity);
                DawnquiverRenderGeometry.planarRibbonXZ(glow, matrix, path,
                        startWidth * 2.0, 0.005,
                        86, 224, 255, 225, 255, 250, alpha, 0);
                DawnquiverRenderGeometry.planarRibbonXZ(body, matrix, path,
                        startWidth, 0.002,
                        201, 255, 255, 255, 250, 214, Math.round(alpha * 0.82F), 0);
                DawnquiverRenderGeometry.taperedRibbon(glow, matrix, path,
                        new Vec3d(0.0, 1.0, 0.0), startWidth * 0.78, 0.003,
                        116, 240, 255, Math.round(alpha * 0.78F), 0);
                DawnquiverRenderGeometry.taperedRibbon(body, matrix, path,
                        new Vec3d(0.0, 1.0, 0.0), startWidth * 0.34, 0.001,
                        238, 255, 250, Math.round(alpha * 0.72F), 0);
            }
        }
    }

    private static Vec3d[] featherPath(float sign, int feather, float formation, int seed) {
        Vec3d[] path = new Vec3d[FEATHER_SPANS + 1];
        double phase = ((seed >> (feather * 2)) & 3) * 0.025;
        for (int index = 0; index <= FEATHER_SPANS; index++) {
            float t = index / (float) FEATHER_SPANS;
            double length = 0.62 + feather * 0.24;
            double span = sign * (0.16 + t * length);
            double bend = 0.10 + Math.sin(t * Math.PI) * (0.28 + feather * 0.10)
                    + t * feather * 0.055;
            double opening = (1.0 - formation) * sign * (feather - 1) * 0.34;
            double cos = Math.cos(opening);
            double sin = Math.sin(opening);
            double formedSpan = span * cos - bend * sin;
            double formedBend = span * sin + bend * cos;
            path[index] = new Vec3d(formedSpan,
                    (feather - 1) * 0.018 + phase * 0.2,
                    formedBend - 0.025 - feather * 0.018);
        }
        return path;
    }

    private static void drawString(VertexConsumer body, VertexConsumer glow, Matrix4f matrix,
                                   float draw, float snap, float opacity) {
        if (snap >= 0.98F) {
            return;
        }
        Vec3d[] upper = limbPath(1.0F, draw, 1.0F, snap);
        Vec3d[] lower = limbPath(-1.0F, draw, 1.0F, snap);
        Vec3d nock = nockPoint(draw, snap);
        int bodyAlpha = Math.round(185 * opacity * (1.0F - snap));
        int glowAlpha = Math.round(100 * opacity * (1.0F - snap));
        stringSegment(glow, matrix, upper[upper.length - 1], nock, 0.018, 115, 238, 255, glowAlpha);
        stringSegment(glow, matrix, lower[lower.length - 1], nock, 0.018, 115, 238, 255, glowAlpha);
        stringSegment(body, matrix, upper[upper.length - 1], nock, 0.007, 235, 255, 255, bodyAlpha);
        stringSegment(body, matrix, lower[lower.length - 1], nock, 0.007, 235, 255, 255, bodyAlpha);
    }

    private static void stringSegment(VertexConsumer vertices, Matrix4f matrix,
                                      Vec3d from, Vec3d to, double width,
                                      int red, int green, int blue, int alpha) {
        Vec3d direction = to.subtract(from);
        if (direction.lengthSquared() < 1.0E-8) {
            return;
        }
        DawnquiverRenderGeometry.line(vertices, matrix, from, to,
                new Vec3d(0.0, 1.0, 0.0), width, red, green, blue, alpha);
        Vec3d planarSide = new Vec3d(-direction.z, 0.0, direction.x);
        if (planarSide.lengthSquared() >= 1.0E-8) {
            DawnquiverRenderGeometry.line(vertices, matrix, from, to,
                    planarSide.normalize(), width * 0.55, red, green, blue, alpha);
        }
    }

    private static void drawEnergyArrow(VertexConsumer body, VertexConsumer glow, Matrix4f matrix,
                                        float age, float draw, float snap, float opacity) {
        float charge = MathHelper.clamp((draw - 0.08F) / 0.92F, 0.0F, 1.0F);
        float visible = Math.max(0.18F, charge) * (1.0F - snap);
        Vec3d nock = nockPoint(draw, snap);
        Vec3d tip = new Vec3d(0.0, 0.0, 0.68 + charge * 0.66);
        float pulse = 0.93F + MathHelper.sin(age * 0.65F) * 0.07F;
        double outer = (0.055 + charge * 0.085) * pulse;

        DawnquiverRenderGeometry.line(glow, matrix, nock, tip,
                new Vec3d(1.0, 0.0, 0.0), outer * 2.2,
                255, 185, 54, Math.round(145 * visible * opacity));
        DawnquiverRenderGeometry.line(glow, matrix, nock, tip,
                new Vec3d(0.0, 1.0, 0.0), outer * 2.2,
                255, 185, 54, Math.round(145 * visible * opacity));
        DawnquiverRenderGeometry.line(body, matrix, nock, tip,
                new Vec3d(1.0, 0.0, 0.0), outer * 0.62,
                255, 251, 224, Math.round(245 * visible * opacity));
        DawnquiverRenderGeometry.line(body, matrix, nock, tip,
                new Vec3d(0.0, 1.0, 0.0), outer * 0.62,
                255, 251, 224, Math.round(245 * visible * opacity));

        float head = (0.13F + charge * 0.20F) * pulse;
        DawnquiverRenderGeometry.billboardLens(glow, matrix, tip, head, head,
                255, 192, 62, Math.round(190 * visible * opacity));
        DawnquiverRenderGeometry.billboardLens(body, matrix, tip.add(0.0, 0.0, 0.003),
                head * 0.38F, head * 0.38F,
                255, 255, 239, Math.round(250 * visible * opacity));
    }

    private static Vec3d nockPoint(float draw, float snap) {
        double pull = -0.08 - draw * 0.78 + snap * 0.88;
        return new Vec3d(0.0, 0.0, pull);
    }

    private static void drawChargeHalo(VertexConsumer glow, Matrix4f matrix,
                                       float age, float draw, float snap, float opacity) {
        float strength = MathHelper.clamp((draw - 0.28F) / 0.72F, 0.0F, 1.0F) * (1.0F - snap);
        if (strength <= 0.01F) {
            return;
        }
        float pulse = 0.95F + MathHelper.sin(age * 0.32F) * 0.05F;
        DawnquiverRenderGeometry.flatRing(glow, matrix, 40, new Vec3d(0.0, 0.0, 0.05),
                (0.50 + strength * 0.46) * pulse, 0.055 + strength * 0.035,
                255, 226, 126, Math.round(58 * strength * opacity));
        DawnquiverRenderGeometry.rays(glow, matrix, nockPoint(draw, snap), 8,
                0.11, 0.28 + strength * 0.34, 0.035,
                age * 0.045F, 255, 219, 104,
                Math.round(105 * strength * opacity), 0);
    }

    private static void drawReleaseFlare(VertexConsumer glow, Matrix4f matrix,
                                         float age, float snap, float opacity) {
        if (snap <= 0.0F || snap >= 1.0F) {
            return;
        }
        float fade = (1.0F - snap) * opacity;
        float radius = 0.25F + snap * 1.85F;
        DawnquiverRenderGeometry.billboardLens(glow, matrix, new Vec3d(0.0, 0.0, 0.28),
                radius, radius * 0.58F,
                255, 221, 120, Math.round(205 * fade));
        DawnquiverRenderGeometry.flatRing(glow, matrix, 40, new Vec3d(0.0, 0.0, 0.30),
                0.28 + snap * 1.18, 0.13 * (1.0F - snap * 0.45F),
                255, 239, 175, Math.round(175 * fade));
        DawnquiverRenderGeometry.rays(glow, matrix, new Vec3d(0.0, 0.0, -0.06), 10,
                0.16, 0.7 + snap * 1.15, 0.075,
                age * 0.04F, 91, 232, 255, Math.round(185 * fade), 0);
    }
}
